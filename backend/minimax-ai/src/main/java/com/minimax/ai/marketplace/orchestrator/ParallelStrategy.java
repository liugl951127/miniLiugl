package com.minimax.ai.marketplace.orchestrator;

import com.minimax.ai.marketplace.AgentGroupMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PARALLEL 策略 (T1)
 *
 * <p>所有成员并行执行, 各跑各的, 合并结果。
 * 用 {@link CompletableFuture#allOf} 等待全部完成。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParallelStrategy implements GroupStrategy {

    public static final String NAME = "PARALLEL";

    private final AgentInvoker invoker;
    private final ExecutorService executor = Executors.newFixedThreadPool(8, r -> {
        Thread t = new Thread(r, "group-parallel-" + System.nanoTime());
        t.setDaemon(true);
        return t;
    });

    @Override
    public String name() { return NAME; }

    @Override
    public void execute(Long userId, List<AgentGroupMember> members, String goal, SseEmitter emitter) throws IOException {
        log.info("[parallel] group run start, members={}", members.size());
        AtomicInteger completed = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        List<String> outputs = new ArrayList<>(members.size());

        for (AgentGroupMember m : members) {
            if (m.getEnabled() != null && m.getEnabled() == 0) continue;
            outputs.add(""); // 占位

            // step-start 同步发
            Map<String, Object> start = new HashMap<>();
            start.put("agentCode", m.getAgentCode());
            start.put("role",      m.getRole());
            start.put("position",  m.getPosition());
            SseEmitterUtil.send(emitter, "step-start", start);

            final AgentGroupMember mm = m;
            final int idx = outputs.size() - 1;
            CompletableFuture<Void> f = CompletableFuture.runAsync(() -> {
                String out = invoker.invokeStreaming(mm, goal, "", token -> {
                    Map<String, Object> tk = new HashMap<>();
                    tk.put("agentCode", mm.getAgentCode());
                    tk.put("content",   token);
                    SseEmitterUtil.send(emitter, "step-token", tk);
                });
                outputs.set(idx, out);
                completed.incrementAndGet();

                Map<String, Object> end = new HashMap<>();
                end.put("agentCode", mm.getAgentCode());
                end.put("output",    out);
                SseEmitterUtil.send(emitter, "step-end", end);
            }, executor);
            futures.add(f);
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.warn("[parallel] 部分成员失败: {}", e.getMessage());
        }

        StringBuilder merged = new StringBuilder();
        for (int i = 0; i < members.size(); i++) {
            AgentGroupMember m = members.get(i);
            if (m.getEnabled() != null && m.getEnabled() == 0) continue;
            String out = outputs.get(i);
            if (merged.length() > 0) merged.append("\n\n");
            merged.append("[").append(m.getAgentCode()).append("]\n").append(out);
        }

        Map<String, Object> fin = new HashMap<>();
        fin.put("success",     true);
        fin.put("finalAnswer", merged.toString());
        fin.put("totalSteps",  completed.get());
        SseEmitterUtil.send(emitter, "final", fin);
    }
}
