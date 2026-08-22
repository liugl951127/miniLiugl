package com.minimax.ai.marketplace.orchestrator;

import com.minimax.ai.marketplace.AgentGroupMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DEBATE 策略 (T1)
 *
 * <p>流程:
 * <ol>
 *   <li>MANAGER 提问 (复述 goal, 拆分维度)</li>
 *   <li>N 个 WORKER 并行回答</li>
 *   <li>CRITIC 对每个 WORKER 输出打分 (0-10)</li>
 *   <li>选最高分输出作为 final</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DebateStrategy implements GroupStrategy {

    public static final String NAME = "DEBATE";

    private final AgentInvoker invoker;
    private final ExecutorService executor = Executors.newFixedThreadPool(8, r -> {
        Thread t = new Thread(r, "group-debate-" + System.nanoTime());
        t.setDaemon(true);
        return t;
    });

    /** 评分正则: 兼容 "8/10" / "8 分" / "评分: 8" 等 */
    private static final Pattern SCORE_PAT = Pattern.compile(
            "(?:评分|score)[^0-9]{0,8}(\\d{1,2})(?:\\s*/\\s*10|分)?", Pattern.CASE_INSENSITIVE);

    @Override
    public String name() { return NAME; }

    @Override
    public void execute(Long userId, List<AgentGroupMember> members, String goal, SseEmitter emitter) throws IOException {
        log.info("[debate] group run start, members={}", members.size());

        // 1) 分类成员
        AgentGroupMember manager = members.stream()
                .filter(m -> "MANAGER".equalsIgnoreCase(m.getRole()))
                .findFirst().orElse(null);
        List<AgentGroupMember> workers = members.stream()
                .filter(m -> "WORKER".equalsIgnoreCase(m.getRole()))
                .filter(m -> m.getEnabled() == null || m.getEnabled() == 1)
                .toList();
        List<AgentGroupMember> critics = members.stream()
                .filter(m -> "CRITIC".equalsIgnoreCase(m.getRole()))
                .filter(m -> m.getEnabled() == null || m.getEnabled() == 1)
                .toList();

        if (workers.isEmpty()) {
            Map<String, Object> fin = new HashMap<>();
            fin.put("success", false);
            fin.put("finalAnswer", "无 WORKER 成员, DEBATE 无法执行");
            fin.put("totalSteps", 0);
            SseEmitterUtil.send(emitter, "final", fin);
            return;
        }

        int stepIdx = 0;
        final String[] managerQuestionRef = new String[]{goal};

        // 2) MANAGER 提问 (可省略, 用 goal 作 question)
        if (manager != null) {
            AgentGroupMember m = manager;
            SseEmitterUtil.send(emitter, "step-start", Map.of(
                    "agentCode", m.getAgentCode(), "role", m.getRole(), "position", m.getPosition()
            ));
            managerQuestionRef[0] = invoker.invokeStreaming(m, "拆分以下目标为 3 个回答维度:\n" + (goal == null ? "" : goal),
                    "", token -> {
                        Map<String, Object> tk = new HashMap<>();
                        tk.put("agentCode", m.getAgentCode());
                        tk.put("content", token);
                        SseEmitterUtil.send(emitter, "step-token", tk);
                    });
            Map<String, Object> end = new HashMap<>();
            end.put("agentCode", m.getAgentCode());
            end.put("output", managerQuestionRef[0]);
            SseEmitterUtil.send(emitter, "step-end", end);
            stepIdx++;
        }
        final String managerQuestion = managerQuestionRef[0];

        // 3) WORKER 并行回答
        List<CompletableFuture<WorkerOutput>> futures = new ArrayList<>();
        for (AgentGroupMember w : workers) {
            SseEmitterUtil.send(emitter, "step-start", Map.of(
                    "agentCode", w.getAgentCode(), "role", w.getRole(), "position", w.getPosition()
            ));
            final AgentGroupMember ww = w;
            CompletableFuture<WorkerOutput> f = CompletableFuture.supplyAsync(() -> {
                String out = invoker.invokeStreaming(ww, goal, managerQuestion, token -> {
                    Map<String, Object> tk = new HashMap<>();
                    tk.put("agentCode", ww.getAgentCode());
                    tk.put("content", token);
                    SseEmitterUtil.send(emitter, "step-token", tk);
                });
                Map<String, Object> end = new HashMap<>();
                end.put("agentCode", ww.getAgentCode());
                end.put("output", out);
                SseEmitterUtil.send(emitter, "step-end", end);
                return new WorkerOutput(ww, out, 0, "");
            }, executor);
            futures.add(f);
        }

        List<WorkerOutput> workerOutputs = new ArrayList<>();
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            for (CompletableFuture<WorkerOutput> f : futures) {
                workerOutputs.add(f.get());
            }
        } catch (Exception e) {
            log.warn("[debate] worker 收集失败: {}", e.getMessage());
        }
        stepIdx += workerOutputs.size();

        // 4) CRITIC 评分 (取第一个 critic, 没有就 default 评分)
        AgentGroupMember critic = critics.isEmpty() ? null : critics.get(0);
        if (critic != null) {
            for (WorkerOutput wo : workerOutputs) {
                SseEmitterUtil.send(emitter, "step-start", Map.of(
                        "agentCode", critic.getAgentCode(),
                        "role",      "CRITIC",
                        "position",  critic.getPosition()
                ));
                String evalPrompt = "请评估以下回答, 输出格式: 评分: 0-10 分 + 一句改进建议。\n"
                        + "原目标: " + (goal == null ? "" : goal) + "\n"
                        + "回答: " + wo.output;
                String eval = invoker.invokeStreaming(critic, evalPrompt, "", token -> {
                    Map<String, Object> tk = new HashMap<>();
                    tk.put("agentCode", critic.getAgentCode());
                    tk.put("content", token);
                    SseEmitterUtil.send(emitter, "step-token", tk);
                });
                int score = parseScore(eval);
                wo.score = score;
                wo.critique = eval;
                Map<String, Object> end = new HashMap<>();
                end.put("agentCode", critic.getAgentCode());
                end.put("output", eval);
                SseEmitterUtil.send(emitter, "step-end", end);
                stepIdx++;
            }
        } else {
            // 没有 critic: 用确定性 hash 评分, 保证可重入
            for (WorkerOutput wo : workerOutputs) {
                wo.score = (int) (Math.abs((long) wo.output.hashCode() % 11));
                wo.critique = "[auto-score, no critic configured]";
            }
        }

        // 5) 选最高分
        WorkerOutput winner = workerOutputs.stream()
                .max(Comparator.comparingInt(w -> w.score))
                .orElse(workerOutputs.get(0));

        StringBuilder fin = new StringBuilder();
        fin.append("=== DEBATE 决策 ===\n");
        fin.append("总步数: ").append(stepIdx).append("\n");
        fin.append("胜出: ").append(winner.member.getAgentCode()).append(" (score=").append(winner.score).append(")\n\n");
        fin.append("=== 胜出回答 ===\n").append(winner.output).append("\n\n");
        fin.append("=== 其他回答 ===\n");
        for (WorkerOutput wo : workerOutputs) {
            if (wo == winner) continue;
            fin.append("- [").append(wo.member.getAgentCode()).append("] score=").append(wo.score).append("\n");
            fin.append(truncate(wo.output, 200)).append("\n");
        }
        if (!workerOutputs.isEmpty() && workerOutputs.get(0).critique != null
                && !workerOutputs.get(0).critique.isBlank()
                && !workerOutputs.get(0).critique.startsWith("[auto-score")) {
            fin.append("\n=== CRITIC 评语 ===\n");
            for (WorkerOutput wo : workerOutputs) {
                fin.append("- [").append(wo.member.getAgentCode()).append("] ").append(wo.critique).append("\n");
            }
        }

        Map<String, Object> finEvt = new HashMap<>();
        finEvt.put("success",     true);
        finEvt.put("finalAnswer", fin.toString());
        finEvt.put("totalSteps",  stepIdx);
        finEvt.put("winnerCode",  winner.member.getAgentCode());
        finEvt.put("winnerScore", winner.score);
        SseEmitterUtil.send(emitter, "final", finEvt);
    }

    // ---------- helpers ----------

    private static int parseScore(String s) {
        if (s == null) return 0;
        Matcher m = SCORE_PAT.matcher(s);
        if (m.find()) {
            try {
                int v = Integer.parseInt(m.group(1));
                return Math.max(0, Math.min(10, v));
            } catch (NumberFormatException ignore) { }
        }
        return 0;
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }

    /** worker 单次输出 + 评分容器 */
    private static class WorkerOutput {
        final AgentGroupMember member;
        final String output;
        int score;
        String critique;
        WorkerOutput(AgentGroupMember m, String o, int s, String c) {
            this.member = m; this.output = o; this.score = s; this.critique = c;
        }
    }
}
