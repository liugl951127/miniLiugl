package com.minimax.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.function.executor.ToolExecutor;
import com.minimax.function.mapper.FunctionToolMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
