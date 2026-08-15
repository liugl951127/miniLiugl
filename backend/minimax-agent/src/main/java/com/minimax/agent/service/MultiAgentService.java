package com.minimax.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * V5.17: Multi-Agent 协作服务.
 *
 * 三个角色协作完成复杂任务:
 *   - Planner  规划师: 把目标拆解成 3-7 步计划
 *   - Executor 执行者: 逐个执行子任务 (复用 AgentService.run)
 *   - Critic   评估者: 评估结果质量, 不通过则反馈给 Planner 调整
 *
 * 流程:
 *   goal → Planner.plan() → steps[]
 *        → Executor 跑每步 → results[]
 *        → Critic.evaluate() → 评估 + 建议
 *            ✓ → final answer
 *            ✗ → Planner 用 critic 反馈重规划 (max 3 rounds)
 *
 * 流式 API (SSE): 实时推送每个角色的决策与执行过程.
 * 事件类型: planner-start / planner-plan / executor-step / executor-result /
 *          critic-eval / critic-retry / final / done / error
 *
 * @since V5.17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiAgentService {

    private final AgentService agentService;

    @Value("${minimax.agent.base-url:http://localhost:8083}")
    private String baseUrl;

    @Value("${minimax.agent.model:MiniMax-Text-01}")
    private String model;

    @Value("${minimax.agent.token:}")
    private String token;

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "multi-agent-sse");
        t.setDaemon(true);
        return t;
    });

    /**
     * 同步执行多智能体任务, 返回完整结果.
     */
    public MultiAgentResult run(Long userId, String goal, List<String> tools) {
        return runWithCritic(userId, goal, tools, 3, null);
    }

    /**
     * 流式执行 (SSE).
     */
    public SseEmitter runStream(Long userId, String goal, List<String> tools) {
        SseEmitter emitter = new SseEmitter(180_000L);  // 3 分钟
        executor.execute(() -> {
            try {
                MultiAgentResult r = runWithCritic(userId, goal, tools, 3, emitter);
                sendEvent(emitter, "done", Map.of(
                    "success", r.success,
                    "answer", r.finalAnswer != null ? r.finalAnswer : "",
                    "rounds", r.rounds,
                    "criticPassed", r.criticPassed,
                    "totalDurationMs", r.totalDurationMs
                ));
            } catch (Exception e) {
                sendEvent(emitter, "error", Map.of("message", e.getMessage()));
            } finally {
                try { emitter.complete(); } catch (Exception ignore) {}
            }
        });
        return emitter;
    }

    /**
     * 核心循环: Planner → Executor → Critic (失败则重规划)
     */
    private MultiAgentResult runWithCritic(Long userId, String goal, List<String> tools,
                                            int maxCriticRounds, SseEmitter emitter) {
        long t0 = System.currentTimeMillis();
        String criticFeedback = null;
        int totalRounds = 0;
        boolean criticPassed = false;
        String finalAnswer = null;
        List<StepRecord> allSteps = new ArrayList<>();
        List<CriticRecord> allCriticEvals = new ArrayList<>();

        if (emitter != null) {
            sendEvent(emitter, "multi-agent-start", Map.of(
                "goal", goal, "maxCriticRounds", maxCriticRounds
            ));
        }

        for (int criticRound = 1; criticRound <= maxCriticRounds; criticRound++) {
            totalRounds = criticRound;

            // 1) Planner: 生成 / 重规划
            if (emitter != null) sendEvent(emitter, "planner-start",
                Map.of("round", criticRound, "feedback", criticFeedback != null ? criticFeedback : ""));
            List<String> planSteps = planSteps(goal, criticFeedback);
            if (emitter != null) sendEvent(emitter, "planner-plan",
                Map.of("round", criticRound, "steps", planSteps));

            if (planSteps.isEmpty()) {
                if (emitter != null) sendEvent(emitter, "error",
                    Map.of("message", "Planner 无法生成计划"));
                return fail("Planner 无法生成计划", t0);
            }

            // 2) Executor: 逐个执行
            StringBuilder results = new StringBuilder();
            for (int i = 0; i < planSteps.size(); i++) {
                String subGoal = planSteps.get(i);
                if (emitter != null) sendEvent(emitter, "executor-step",
                    Map.of("round", criticRound, "step", i + 1, "goal", subGoal));

                AgentService.AgentResult sub = agentService.run(userId, subGoal, tools);
                String obs = sub.answer() != null ? sub.answer() : "(无输出)";
                allSteps.add(new StepRecord(criticRound, i + 1, subGoal, obs, sub.durationMs()));
                results.append("【").append(i + 1).append("】").append(subGoal).append("\n")
                       .append("→ ").append(obs).append("\n\n");

                if (emitter != null) sendEvent(emitter, "executor-result",
                    Map.of("round", criticRound, "step", i + 1,
                           "observation", obs, "durationMs", sub.durationMs()));
            }

            // 3) Critic: 评估
            if (emitter != null) sendEvent(emitter, "critic-eval",
                Map.of("round", criticRound, "plan", planSteps, "results", results.toString()));
            CriticEval eval = evaluate(goal, planSteps, results.toString());
            allCriticEvals.add(new CriticRecord(criticRound, eval.passed, eval.feedback));
            if (emitter != null) sendEvent(emitter, "critic-result",
                Map.of("round", criticRound,
                       "passed", eval.passed,
                       "score", eval.score,
                       "feedback", eval.feedback));

            if (eval.passed) {
                criticPassed = true;
                finalAnswer = eval.improvedAnswer != null ? eval.improvedAnswer : results.toString().trim();
                if (emitter != null) sendEvent(emitter, "final",
                    Map.of("answer", finalAnswer, "rounds", criticRound));
                break;
            } else {
                criticFeedback = eval.feedback;
                if (emitter != null) sendEvent(emitter, "critic-retry",
                    Map.of("round", criticRound, "feedback", criticFeedback));
            }
        }

        if (!criticPassed && finalAnswer == null) {
            // 超过最大 critic 轮次, 仍返回最后结果
            finalAnswer = allSteps.isEmpty() ? "(执行失败)" : "⚠️ Critic 未通过, 取最后一次执行结果";
        }

        return new MultiAgentResult(
            criticPassed,
            finalAnswer,
            totalRounds,
            criticPassed,
            allSteps,
            allCriticEvals,
            System.currentTimeMillis() - t0
        );
    }

    // ---- Planner: 内部直接调 LLM (不走 AgentService.run, 因为要快速) ----

    public List<String> planSteps(String goal, String feedback) {
        try {
            String sysPrompt = "你是一个任务规划专家。给定一个目标, 拆成 3-7 个有序步骤。\n" +
                "每个步骤要明确: 做什么 + 预期输出。\n" +
                "如果有改进建议, 必须采纳并调整计划。\n" +
                "返回 JSON 数组: [\"步骤1\", \"步骤2\", ...]\n" +
                "只返回 JSON, 不要解释。";
            String userMsg = "目标: " + goal
                + (feedback != null ? "\n\n上轮未通过, 改进建议:\n" + feedback : "");

            String content = callLlm(sysPrompt, userMsg, 0.3);
            return parseSteps(content);
        } catch (Exception e) {
            log.warn("plan 失败: {}", e.getMessage());
            return List.of();
        }
    }

    // ---- Critic: 评估执行结果 ----

    public CriticEval evaluate(String goal, List<String> plan, String results) {
        try {
            String sysPrompt = "你是一个结果评估专家。给定用户目标和执行结果, 评估:\n" +
                "1. 是否达成目标 (passed: true/false)\n" +
                "2. 评分 (score: 0-10)\n" +
                "3. 如果不通过, 给出具体改进建议 (feedback)\n" +
                "4. 如果通过, 给出优化后的最终答案 (improvedAnswer)\n" +
                "返回 JSON: {\"passed\":true/false,\"score\":N,\"feedback\":\"...\",\"improvedAnswer\":\"...\"}";

            String userMsg = "目标: " + goal + "\n\n"
                + "计划步骤: " + String.join(" | ", plan) + "\n\n"
                + "执行结果:\n" + results;
            String content = callLlm(sysPrompt, userMsg, 0.2);
            return parseCritic(content);
        } catch (Exception e) {
            log.warn("critic 失败 (默认通过): {}", e.getMessage());
            return new CriticEval(true, 7, "评估异常, 默认通过", results.trim());
        }
    }

    // ---- 通用 LLM 调用 ----

    private String callLlm(String sysPrompt, String userMsg, double temperature) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
            Map.of("role", "system", "content", sysPrompt),
            Map.of("role", "user", "content", userMsg)
        ));
        body.put("temperature", temperature);

        HttpRequest.Builder hb = HttpRequest.newBuilder()
            .uri(URI.create(stripSlash(baseUrl) + "/api/v1/models/chat"))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        if (token != null && !token.isBlank()) {
            hb.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> resp = client.send(hb.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("LLM HTTP " + resp.statusCode() + " " + truncate(resp.body(), 200));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> root = json.readValue(resp.body(), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) root.get("data");
        if (data == null) return "";
        return (String) data.get("content");
    }

    private List<String> parseSteps(String content) {
        if (content == null) return List.of();
        content = content.trim();
        if (content.startsWith("```")) {
            int end = content.indexOf("```", 3);
            if (end > 0) content = content.substring(3, end).trim();
            if (content.startsWith("json")) content = content.substring(4).trim();
        }
        if (content.startsWith("[") && content.endsWith("]")) {
            try {
                @SuppressWarnings("unchecked")
                List<String> list = json.readValue(content, List.class);
                return list != null ? list : List.of();
            } catch (Exception ignore) {}
        }
        return Arrays.stream(content.split("\n"))
            .map(s -> s.replaceAll("^[\\d\\.\\s\\-\\*]+", "").trim())
            .filter(s -> !s.isEmpty() && s.length() > 3)
            .limit(7)
            .toList();
    }

    private CriticEval parseCritic(String content) {
        if (content == null) return new CriticEval(true, 5, "", content);
        content = content.trim();
        if (content.startsWith("```")) {
            int end = content.indexOf("```", 3);
            if (end > 0) content = content.substring(3, end).trim();
            if (content.startsWith("json")) content = content.substring(4).trim();
        }
        try {
            int start = content.indexOf("{");
            int end = content.lastIndexOf("}");
            if (start >= 0 && end > start) {
                String jsonStr = content.substring(start, end + 1);
                @SuppressWarnings("unchecked")
                Map<String, Object> map = json.readValue(jsonStr, Map.class);
                boolean passed = Boolean.TRUE.equals(map.get("passed"));
                int score = map.get("score") instanceof Number ? ((Number) map.get("score")).intValue() : 5;
                String feedback = (String) map.getOrDefault("feedback", "");
                String improved = (String) map.getOrDefault("improvedAnswer", null);
                return new CriticEval(passed, score, feedback, improved);
            }
        } catch (Exception e) {
            log.debug("critic JSON 解析失败: {}", e.getMessage());
        }
        // fallback: 文本里找 true/false
        boolean passed = content.toLowerCase().contains("passed\":true") || content.toLowerCase().contains("通过");
        return new CriticEval(passed, passed ? 7 : 4, content, null);
    }

    // ---- 工具方法 ----

    private void sendEvent(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event()
                .name(event)
                .data(data, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.warn("SSE 发送失败 event={}: {}", event, e.getMessage());
        }
    }

    private String stripSlash(String s) {
        return s == null ? "" : (s.endsWith("/") ? s.substring(0, s.length() - 1) : s);
    }

    private String truncate(String s, int n) {
        return s == null ? null : (s.length() > n ? s.substring(0, n) : s);
    }

    private MultiAgentResult fail(String msg, long t0) {
        return new MultiAgentResult(false, msg, 0, false, List.of(), List.of(),
                System.currentTimeMillis() - t0);
    }

    // ---- DTOs ----

    public record MultiAgentResult(boolean success, String finalAnswer, int rounds,
                                    boolean criticPassed, List<StepRecord> steps,
                                    List<CriticRecord> criticEvals, long totalDurationMs) {}

    public record StepRecord(int criticRound, int stepIndex, String goal, String observation, long durationMs) {}

    public record CriticRecord(int round, boolean passed, String feedback) {}

    public record CriticEval(boolean passed, int score, String feedback, String improvedAnswer) {}
}
