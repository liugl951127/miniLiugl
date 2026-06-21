package com.minimax.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.function.executor.ToolExecutor;
import com.minimax.function.mapper.FunctionToolMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * V2.1: Agent 自主任务服务 (ReAct 模式)
 *
 * 流程:
 *   Thought:  LLM 思考下一步
 *   Action:   调工具 (Function Call)
 *   Observation: 工具结果回传
 *   循环直到 LLM 给出 Final Answer 或超 maxRounds
 *
 * 核心区别于 FunctionCallingService:
 *   - Agent 是"目标驱动" (给一个 goal, 自己拆解步骤)
 *   - FunctionCalling 是"轮次驱动" (一轮调 1-N 个工具)
 *   - Agent 输出可序列化的 plan + steps, 用于前端可视化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    @Value("${minimax.agent.max-rounds:8}")
    private int defaultMaxRounds;

    @Value("${minimax.agent.base-url:http://localhost:8083}")
    private String baseUrl;

    @Value("${minimax.agent.model:MiniMax-Text-01}")
    private String model;

    @Value("${minimax.agent.token:}")
    private String token;

    private final FunctionToolMapper toolMapper;
    private final ToolExecutor toolExecutor;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    // V5.16: SSE 异步执行 (避免阻塞 Tomcat 线程)
    private final java.util.concurrent.ExecutorService executor =
        java.util.concurrent.Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "agent-sse");
            t.setDaemon(true);
            return t;
        });

    /**
     * 执行 Agent 任务。
     * @param goal 目标 (e.g. "查北京明天天气并发邮件给我")
     * @param tools 允许使用的工具名列表 (空 = 全部启用)
     */
    public AgentResult run(Long userId, String goal, List<String> tools) {
        long t0 = System.currentTimeMillis();
        int maxRounds = defaultMaxRounds;
        List<Step> steps = new ArrayList<>();
        Set<String> toolsUsed = new LinkedHashSet<>();

        // 1) 准备 tools schema
        var enabledTools = pickTools(tools);
        if (enabledTools.isEmpty()) {
            return AgentResult.fail("没有可用工具, 无法执行", steps, 0, toolsUsed);
        }
        var toolSchemas = toOpenAiTools(enabledTools);

        // 2) 初始化 messages
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "你是一个 Agent。给定一个目标, 用 Thought/Action/Observation 循环自主完成。\n" +
                "可用工具: " + enabledTools.stream().map(t -> t.getName()).toList() + "\n" +
                "规则:\n" +
                "1. 每轮先输出 Thought (你的思考, 简短)\n" +
                "2. 然后决定 Action: 调工具 (返回 tool_calls) 或 Final Answer (用 <final>你的答案</final> 包裹)\n" +
                "3. 最多 " + maxRounds + " 轮\n" +
                "4. 没工具可用就直接给 Final Answer"));

        messages.add(Map.of("role", "user", "content", "目标: " + goal));

        // 3) 循环
        for (int round = 1; round <= maxRounds; round++) {
            LlmResp resp;
            try {
                resp = callLlm(messages, toolSchemas);
            } catch (Exception e) {
                log.warn("Agent LLM 失败 round={}: {}", round, e.getMessage());
                return AgentResult.fail("LLM 失败: " + e.getMessage(), steps, round, toolsUsed);
            }

            Step step = new Step();
            step.round = round;
            step.thought = resp.content;
            step.thinking = extractThinking(resp.content);

            // 检测 Final Answer
            String finalAns = extractFinalAnswer(resp.content);
            if (finalAns != null) {
                step.action = "FinalAnswer";
                step.observation = finalAns;
                steps.add(step);
                return AgentResult.ok(finalAns, steps, round, toolsUsed,
                        System.currentTimeMillis() - t0);
            }

            // 检测 tool_calls
            if (resp.toolCalls == null || resp.toolCalls.isEmpty()) {
                // 既无 final 又无 tool → 模型没按规则, 给最后一次机会
                step.action = "stalled";
                step.observation = "模型未产生 tool_call 或 final_answer";
                steps.add(step);
                if (round == maxRounds) {
                    return AgentResult.ok(resp.content == null ? "" : resp.content,
                            steps, round, toolsUsed, System.currentTimeMillis() - t0);
                }
                continue;
            }

            // 加 assistant message
            Map<String, Object> asst = new HashMap<>();
            asst.put("role", "assistant");
            asst.put("content", resp.content == null ? "" : resp.content);
            asst.put("tool_calls", resp.toolCalls);
            messages.add(asst);

            for (var tc : resp.toolCalls) {
                toolsUsed.add(tc.function.name);
                step.action = "call:" + tc.function.name;
                step.arguments = tc.function.arguments;
                long ts = System.currentTimeMillis();
                try {
                    Map<String, Object> argsMap = json.readValue(
                            tc.function.arguments == null ? "{}" : tc.function.arguments, Map.class);
                    String result = toolExecutor.executeForChat(tc.function.name, argsMap);
                    step.observation = truncate(result, 500);
                    step.durationMs = System.currentTimeMillis() - ts;
                } catch (Exception e) {
                    step.observation = "ERROR: " + e.getMessage();
                    step.durationMs = System.currentTimeMillis() - ts;
                }
                // tool message
                Map<String, Object> tm = new HashMap<>();
                tm.put("role", "tool");
                tm.put("tool_call_id", tc.id);
                tm.put("name", tc.function.name);
                tm.put("content", step.observation);
                messages.add(tm);
            }
            steps.add(step);
        }

        return AgentResult.fail("达到 maxRounds=" + maxRounds + " 仍未完成",
                steps, maxRounds, toolsUsed);
    }

    private List<com.minimax.function.entity.FunctionTool> pickTools(List<String> names) {
        if (names == null || names.isEmpty()) {
            return toolMapper.selectEnabled();
        }
        return names.stream()
                .map(toolMapper::selectByName)
                .filter(Objects::nonNull)
                .filter(t -> t.getEnabled() != null && t.getEnabled() == 1)
                .toList();
    }

    private List<Map<String, Object>> toOpenAiTools(List<com.minimax.function.entity.FunctionTool> tools) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (var t : tools) {
            Map<String, Object> fn = new HashMap<>();
            fn.put("name", t.getName());
            fn.put("description", t.getDescription());
            try {
                fn.put("parameters", json.readValue(t.getParameters(), Map.class));
            } catch (Exception e) {
                fn.put("parameters", Map.of("type", "object", "properties", Map.of()));
            }
            out.add(Map.of("type", "function", "function", fn));
        }
        return out;
    }

    private LlmResp callLlm(List<Map<String, Object>> messages,
                              List<Map<String, Object>> tools) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.4);
        if (tools != null && !tools.isEmpty()) body.put("tools", tools);

        HttpRequest.Builder hb = HttpRequest.newBuilder()
                .uri(URI.create(stripSlash(baseUrl) + "/api/v1/models/chat"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        if (token != null && !token.isBlank()) hb.header("Authorization", "Bearer " + token);
        HttpResponse<String> resp = client.send(hb.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + resp.statusCode() + " " + truncate(resp.body(), 200));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> root = json.readValue(resp.body(), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) root.get("data");
        if (data == null) return new LlmResp("", null);

        String content = (String) data.get("content");
        @SuppressWarnings("unchecked")
        Map<String, Object> msg = (Map<String, Object>) data.get("message");
        List<LlmToolCall> toolCalls = null;
        if (msg != null && msg.get("tool_calls") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tcs = (List<Map<String, Object>>) msg.get("tool_calls");
            toolCalls = new ArrayList<>();
            for (Map<String, Object> tc : tcs) {
                LlmToolCall c = new LlmToolCall();
                c.id = (String) tc.get("id");
                @SuppressWarnings("unchecked")
                Map<String, Object> fn = (Map<String, Object>) tc.get("function");
                if (fn != null) {
                    c.function = new LlmToolCall.Fn();
                    c.function.name = (String) fn.get("name");
                    c.function.arguments = (String) fn.get("arguments");
                }
                toolCalls.add(c);
            }
        }
        return new LlmResp(content == null ? "" : content, toolCalls);
    }

    /**
     * V5.16: Agent 流式执行 (SSE).
     * 实时推送每个步骤: step-start / thought / tool-call / observation / final / done
     * 用 SseEmitter 替代单次返回, 让用户看到 agent 思考过程
     *
     * @return SseEmitter, 客户端通过 EventSource 接收
     */
    public SseEmitter runStream(Long userId, String goal, List<String> tools) {
        SseEmitter emitter = new SseEmitter(120_000L);  // 2 分钟超时
        executor.execute(() -> {
            try {
                runStreamInternal(userId, goal, tools, emitter);
            } catch (Exception e) {
                sendEvent(emitter, "error", Map.of("message", e.getMessage()));
                emitter.completeWithError(e);
            } finally {
                try { emitter.complete(); } catch (Exception ignore) {}
            }
        });
        return emitter;
    }

    private void runStreamInternal(Long userId, String goal, List<String> tools, SseEmitter emitter) throws Exception {
        long t0 = System.currentTimeMillis();
        int maxRounds = defaultMaxRounds;
        List<Step> steps = new ArrayList<>();
        Set<String> toolsUsed = new LinkedHashSet<>();

        sendEvent(emitter, "start", Map.of(
            "goal", goal,
            "maxRounds", maxRounds,
            "ts", t0
        ));

        // 1) 工具
        var enabledTools = pickTools(tools);
        if (enabledTools.isEmpty()) {
            sendEvent(emitter, "error", Map.of("message", "没有可用工具, 无法执行"));
            return;
        }
        var toolSchemas = toOpenAiTools(enabledTools);
        sendEvent(emitter, "tools", Map.of("tools", enabledTools.stream().map(t -> Map.of(
            "name", t.getName(), "description", t.getDescription()
        )).toList()));

        // 2) messages
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "你是一个 Agent。给定一个目标, 用 Thought/Action/Observation 循环自主完成。\n" +
                "可用工具: " + enabledTools.stream().map(t -> t.getName()).toList() + "\n" +
                "规则:\n" +
                "1. 每轮先输出 Thought (你的思考, 简短)\n" +
                "2. 然后决定 Action: 调工具 (返回 tool_calls) 或 Final Answer (用 <final>你的答案</final> 包裹)\n" +
                "3. 最多 " + maxRounds + " 轮\n" +
                "4. 没工具可用就直接给 Final Answer"));
        messages.add(Map.of("role", "user", "content", "目标: " + goal));

        // 3) ReAct 循环
        for (int round = 1; round <= maxRounds; round++) {
            Step step = new Step();
            step.round = round;
            long r0 = System.currentTimeMillis();

            sendEvent(emitter, "step-start", Map.of("round", round));
            LlmResp resp;
            try {
                resp = callLlm(messages, toolSchemas);
            } catch (Exception e) {
                sendEvent(emitter, "step-error", Map.of("round", round, "message", e.getMessage()));
                return;
            }

            String content = resp.content == null ? "" : resp.content;
            String thought = extractThinking(content);
            step.thinking = thought;

            // 推送 thought
            sendEvent(emitter, "thought", Map.of(
                "round", round,
                "thought", thought
            ));

            // 检查 final answer
            String finalAns = extractFinalAnswer(content);
            if (finalAns != null) {
                step.observation = "FINAL: " + finalAns;
                step.durationMs = System.currentTimeMillis() - r0;
                steps.add(step);
                sendEvent(emitter, "final", Map.of(
                    "round", round,
                    "answer", finalAns
                ));
                sendEvent(emitter, "done", Map.of(
                    "success", true,
                    "rounds", round,
                    "toolsUsed", new ArrayList<>(toolsUsed),
                    "durationMs", System.currentTimeMillis() - t0
                ));
                return;
            }

            // 调用工具
            if (resp.toolCalls == null || resp.toolCalls.isEmpty()) {
                sendEvent(emitter, "step-error", Map.of("round", round, "message", "模型未返回工具调用, 终止"));
                return;
            }

            for (var tc : resp.toolCalls) {
                String toolName = tc.function.name;
                String args = tc.function.arguments;
                step.action = toolName;
                step.arguments = args;
                toolsUsed.add(toolName);

                sendEvent(emitter, "tool-call", Map.of(
                    "round", round,
                    "tool", toolName,
                    "arguments", args
                ));

                String observation = toolExecutor.invoke(toolName, args);
                step.observation = observation;
                sendEvent(emitter, "observation", Map.of(
                    "round", round,
                    "tool", toolName,
                    "observation", observation,
                    "durationMs", System.currentTimeMillis() - r0
                ));

                // 加入 messages
                messages.add(Map.of("role", "assistant", "content", content,
                    "tool_calls", List.of(Map.of(
                        "id", tc.id != null ? tc.id : "call_" + round,
                        "type", "function",
                        "function", Map.of("name", toolName, "arguments", args)
                    ))));
                messages.add(Map.of("role", "tool", "tool_call_id",
                    tc.id != null ? tc.id : "call_" + round,
                    "content", observation));
            }
            step.durationMs = System.currentTimeMillis() - r0;
            steps.add(step);
        }

        // 超过 maxRounds
        sendEvent(emitter, "done", Map.of(
            "success", false,
            "message", "达到最大轮次 " + maxRounds,
            "rounds", maxRounds,
            "toolsUsed", new ArrayList<>(toolsUsed),
            "durationMs", System.currentTimeMillis() - t0
        ));
    }

    /**
     * V5.16: 集成 RAG 长期记忆 — 执行时先检索相关记忆, 拼入 system prompt.
     * 简化实现: 调 RAG /retrieve 拿相关 chunks, 拼到 system message
     */
    public AgentResult runWithMemory(Long userId, String goal, List<String> tools, Long sessionId) {
        // 1) 调 RAG retrieve 拿相关记忆
        List<String> memories = retrieveMemories(goal, 3);
        // 2) 拼到 goal
        String enrichedGoal = goal;
        if (!memories.isEmpty()) {
            enrichedGoal = "相关记忆:\n" + String.join("\n", memories) + "\n\n目标: " + goal;
        }
        // 3) 调普通 run
        AgentResult base = run(userId, enrichedGoal, tools);
        return new AgentResult(base.success(), base.answer(), base.steps(),
                base.rounds(), base.toolsUsed(), base.durationMs());
    }

    /**
     * V5.16: 调 RAG 服务拿相关记忆 (HTTP /api/v1/rag/retrieve)
     */
    private List<String> retrieveMemories(String query, int topK) {
        try {
            // 默认走 gateway
            String url = "http://localhost:8080/api/v1/rag/retrieve";
            String body = String.format("{\"query\":\"%s\",\"topK\":%d}",
                query.replace("\\", "\\\\").replace("\"", "\\\""), topK);
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return List.of();
            @SuppressWarnings("unchecked")
            Map<String, Object> root = json.readValue(resp.body(), Map.class);
            Object data = root.get("data");
            if (!(data instanceof List)) return List.of();
            List<String> out = new ArrayList<>();
            for (Object o : (List<?>) data) {
                if (o instanceof Map) {
                    Object text = ((Map<?, ?>) o).get("text");
                    if (text != null) out.add(text.toString());
                }
            }
            return out;
        } catch (Exception e) {
            log.debug("RAG retrieve 失败: {}", e.getMessage());
            return List.of();
        }
    }

    private void sendEvent(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event()
                .name(event)
                .data(data, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.warn("SSE 发送失败 event={}: {}", event, e.getMessage());
        }
    }

    // ---- V5.16: Plan 模式 ----

    /**
     * V5.16: Plan 模式 — 先让 LLM 把目标拆成 3-7 个步骤, 用户确认后再执行.
     * 用于复杂任务, 避免 agent 跑偏.
     */
    public List<String> plan(Long userId, String goal) {
        try {
            String sysPrompt = "你是一个任务规划专家。给定一个目标, 拆成 3-7 个有序步骤。\n" +
                "每个步骤要明确: 用什么工具/动作, 预期输出。\n" +
                "返回 JSON 数组: [\"步骤1: ...\", \"步骤2: ...\", ...]\n" +
                "不要解释, 只返回 JSON 数组。";
            List<Map<String, Object>> msgs = List.of(
                Map.of("role", "system", "content", sysPrompt),
                Map.of("role", "user", "content", "目标: " + goal)
            );
            Map<String, Object> body = Map.of(
                "model", model,
                "messages", msgs,
                "temperature", 0.3
            );
            HttpRequest.Builder hb = HttpRequest.newBuilder()
                .uri(URI.create(stripSlash(baseUrl) + "/api/v1/models/chat"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
            if (token != null && !token.isBlank()) {
                hb.header("Authorization", "Bearer " + token);
            }
            HttpRequest req = hb.build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return List.of();
            @SuppressWarnings("unchecked")
            Map<String, Object> root = json.readValue(resp.body(), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) root.get("data");
            if (data == null) return List.of();
            String content = (String) data.get("content");
            // 简单解析 JSON 数组
            if (content == null) return List.of();
            content = content.trim();
            if (content.startsWith("```")) {
                int end = content.indexOf("```", 3);
                if (end > 0) content = content.substring(3, end).trim();
                if (content.startsWith("json")) content = content.substring(4).trim();
            }
            if (content.startsWith("[") && content.endsWith("]")) {
                return json.readValue(content, List.class);
            }
            // fallback: 拆行
            return Arrays.stream(content.split("\n"))
                .map(s -> s.replaceAll("^[\\d\\.\\s\\-\\*]+", "").trim())
                .filter(s -> !s.isEmpty() && s.length() > 3)
                .limit(7)
                .toList();
        } catch (Exception e) {
            log.warn("plan 失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * V5.16: 执行已确认的 Plan — 按步骤依次调用 agent.run(), 每步独立完成
     */
    public AgentResult runPlan(Long userId, String goal, List<String> planSteps) {
        long t0 = System.currentTimeMillis();
        List<Step> allSteps = new ArrayList<>();
        Set<String> toolsUsed = new LinkedHashSet<>();
        StringBuilder finalAns = new StringBuilder();

        for (int i = 0; i < planSteps.size(); i++) {
            String subGoal = planSteps.get(i);
            AgentResult sub = run(userId, subGoal, List.of());
            if (sub.steps() != null) allSteps.addAll(sub.steps());
            if (sub.toolsUsed() != null) toolsUsed.addAll(sub.toolsUsed());
            if (sub.answer() != null) {
                finalAns.append("【").append(i + 1).append("】").append(sub.answer()).append("\n\n");
            }
        }
        return AgentResult.ok(
            finalAns.length() > 0 ? finalAns.toString().trim() : "Plan 执行完成",
            allSteps,
            planSteps.size(),
            toolsUsed,
            System.currentTimeMillis() - t0
        );
    }

    private String extractFinalAnswer(String content) {
        if (content == null) return null;
        int s = content.indexOf("<final>");
        int e = content.indexOf("</final>");
        if (s >= 0 && e > s) {
            return content.substring(s + 7, e).trim();
        }
        return null;
    }

    private String extractThinking(String content) {
        if (content == null) return null;
        int s = content.indexOf("Thought:");
        int e = content.indexOf("Action:", s);
        if (s >= 0 && e > s) return content.substring(s + 8, e).trim();
        return content.length() > 200 ? content.substring(0, 200) : content;
    }

    private String stripSlash(String s) {
        return s == null ? "" : (s.endsWith("/") ? s.substring(0, s.length()-1) : s);
    }

    private String truncate(String s, int n) {
        return s == null ? null : (s.length() > n ? s.substring(0, n) : s);
    }

    // ---- DTOs ----

    public static class LlmResp {
        public String content;
        public List<LlmToolCall> toolCalls;
        public LlmResp(String c, List<LlmToolCall> t) { this.content = c; this.toolCalls = t; }
    }

    public static class LlmToolCall {
        public String id;
        public Fn function;
        public static class Fn { public String name; public String arguments; }
    }

    public static class Step {
        public int round;
        public String thinking;
        public String thought;
        public String action;
        public String arguments;
        public String observation;
        public long durationMs;
    }

    public record AgentResult(boolean success, String answer,
                               List<Step> steps, int rounds,
                               Set<String> toolsUsed, long durationMs) {
        public static AgentResult ok(String a, List<Step> s, int r, Set<String> t, long d) {
            return new AgentResult(true, a, s, r, t, d);
        }
        public static AgentResult fail(String msg, List<Step> s, int r, Set<String> t) {
            return new AgentResult(false, msg, s, r, t, 0);
        }
    }
}
