package com.minimax.ai.marketplace;

import com.minimax.ai.entity.AgentGroup;
import com.minimax.ai.marketplace.orchestrator.DebateStrategy;
import com.minimax.ai.marketplace.orchestrator.GroupStrategy;
import com.minimax.ai.marketplace.orchestrator.ParallelStrategy;
import com.minimax.ai.marketplace.orchestrator.PipelineStrategy;
import com.minimax.ai.marketplace.orchestrator.SseEmitterUtil;
import com.minimax.common.sse.SseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AgentGroupOrchestrator 主入口 (T1-backend-orchestrator)
 *
 * <p>职责:
 * <ol>
 *   <li>加载 group 的全部启用成员 (按 position ASC)</li>
 *   <li>查 AgentGroup 策略</li>
 *   <li>按 strategy 分发到 {@link GroupStrategy} 实现</li>
 *   <li>通过 SseEmitter 推流 (step-start/step-token/step-end/final)</li>
 *   <li>封装异常 (try/catch → emit error → complete)</li>
 * </ol>
 *
 * <p>异步: 业务在线程池跑, 主线程立即返回 emitter, 避免 Servlet 线程被占用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentGroupOrchestrator {

    private final AgentGroupMemberService memberService;
    private final AgentGroupMapper groupMapper;

    private final PipelineStrategy pipeline;
    private final ParallelStrategy parallel;
    private final DebateStrategy   debate;

    private final java.util.concurrent.ExecutorService executor =
            java.util.concurrent.Executors.newFixedThreadPool(8, r -> {
                Thread t = new Thread(r, "group-orch-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            });

    /**
     * 启动流式执行
     *
     * @param userId    触发用户
     * @param groupId   AgentGroup.id (主键, 不是 groupId 业务字段)
     * @param goal      用户目标
     * @param toolCodes 工具代码列表 (预留, 转发给 strategy)
     * @return SseEmitter
     */
    public SseEmitter run(Long userId, Long groupId, String goal, List<String> toolCodes) {
        SseEmitter emitter = new SseEmitter(180_000L); // 3 分钟
        executor.execute(() -> {
            long t0 = System.currentTimeMillis();
            try {
                // 1. 加载 group
                AgentGroup group = groupMapper.selectById(groupId);
                if (group == null) {
                    Map<String, Object> fin = new HashMap<>();
                    fin.put("success", false);
                    fin.put("finalAnswer", "群组不存在: id=" + groupId);
                    fin.put("totalSteps", 0);
                    SseEmitterUtil.send(emitter, "final", fin);
                    SseUtil.sendDone(emitter);
                    emitter.complete();
                    return;
                }
                SseEmitterUtil.send(emitter, "start", Map.of(
                        "groupId",  group.getId(),
                        "groupName", group.getName(),
                        "goal",     goal == null ? "" : goal,
                        "ts",       t0
                ));

                // 2. 加载启用成员
                List<AgentGroupMember> members = memberService.listEnabledByGroupId(groupId);
                if (members.isEmpty()) {
                    Map<String, Object> fin = new HashMap<>();
                    fin.put("success", false);
                    fin.put("finalAnswer", "群组无启用成员");
                    fin.put("totalSteps", 0);
                    SseEmitterUtil.send(emitter, "final", fin);
                    SseUtil.sendDone(emitter);
                    emitter.complete();
                    return;
                }

                // 3. 选 strategy
                String strategyName = group.getStrategy() == null
                        ? PipelineStrategy.NAME
                        : group.getStrategy().toUpperCase();
                GroupStrategy strategy = pickStrategy(strategyName);

                // 4. 推 group-meta 事件 (前端可显示 strategy 名字)
                SseEmitterUtil.send(emitter, "group-meta", Map.of(
                        "strategy",  strategy.name(),
                        "memberCount", members.size()
                ));

                // 5. 执行
                strategy.execute(userId, members, goal, emitter);

                // 6. 完成
                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("durationMs", System.currentTimeMillis() - t0);
                SseEmitterUtil.send(emitter, "summary", summary);
                SseUtil.sendDone(emitter);
                emitter.complete();
            } catch (Exception e) {
                log.error("[orch] group run 异常", e);
                Map<String, Object> fin = new HashMap<>();
                fin.put("success", false);
                fin.put("finalAnswer", "编排异常: " + e.getMessage());
                fin.put("totalSteps", 0);
                try { SseEmitterUtil.send(emitter, "final", fin); } catch (Exception ignore) {}
                try { SseUtil.sendError(emitter, e.getMessage()); } catch (Exception ignore) {}
                try { emitter.completeWithError(e); } catch (Exception ignore) {}
            }
        });
        return emitter;
    }

    /**
     * 用外部 strategy 名执行 (controller 传 strategy 时走此路径, 不读 db 字段)
     */
    public SseEmitter runWithStrategy(Long userId, Long groupId, String goal, List<String> toolCodes, String strategyName) {
        SseEmitter emitter = new SseEmitter(180_000L);
        executor.execute(() -> {
            long t0 = System.currentTimeMillis();
            try {
                AgentGroup group = groupMapper.selectById(groupId);
                if (group == null) {
                    sendFinalError(emitter, "群组不存在: id=" + groupId, 0);
                    return;
                }
                SseEmitterUtil.send(emitter, "start", Map.of(
                        "groupId", group.getId(),
                        "groupName", group.getName(),
                        "goal",    goal == null ? "" : goal,
                        "ts",      t0
                ));
                List<AgentGroupMember> members = memberService.listEnabledByGroupId(groupId);
                if (members.isEmpty()) {
                    sendFinalError(emitter, "群组无启用成员", 0);
                    return;
                }
                String sName = strategyName == null || strategyName.isBlank()
                        ? PipelineStrategy.NAME
                        : strategyName.toUpperCase();
                GroupStrategy strategy = pickStrategy(sName);
                SseEmitterUtil.send(emitter, "group-meta", Map.of(
                        "strategy", strategy.name(),
                        "memberCount", members.size()
                ));
                strategy.execute(userId, members, goal, emitter);
                SseEmitterUtil.send(emitter, "summary", Map.of("durationMs", System.currentTimeMillis() - t0));
                SseUtil.sendDone(emitter);
                emitter.complete();
            } catch (Exception e) {
                log.error("[orch] runWithStrategy 异常", e);
                try {
                    sendFinalError(emitter, "编排异常: " + e.getMessage(), 0);
                    SseUtil.sendError(emitter, e.getMessage());
                    emitter.completeWithError(e);
                } catch (Exception ignore) {}
            }
        });
        return emitter;
    }

    private void sendFinalError(SseEmitter emitter, String msg, int steps) {
        Map<String, Object> fin = new HashMap<>();
        fin.put("success", false);
        fin.put("finalAnswer", msg);
        fin.put("totalSteps", steps);
        SseEmitterUtil.send(emitter, "final", fin);
    }

    private GroupStrategy pickStrategy(String name) {
        return switch (name.toUpperCase()) {
            case "PARALLEL" -> parallel;
            case "DEBATE"   -> debate;
            case "PIPELINE" -> pipeline;
            default -> {
                log.warn("[orch] 未知 strategy={}, 回退到 PIPELINE", name);
                yield pipeline;
            }
        };
    }
}
