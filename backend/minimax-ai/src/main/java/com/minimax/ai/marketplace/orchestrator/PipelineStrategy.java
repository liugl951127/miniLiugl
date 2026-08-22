package com.minimax.ai.marketplace.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.ai.marketplace.AgentGroupMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PIPELINE 策略 (T1)
 *
 * <p>顺序执行: 上一步的 output 作为下一步的 input (context), 累加为最终结果。
 * 适合"分析 → 写作 → 审核"这种串行工作流。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PipelineStrategy implements GroupStrategy {

    public static final String NAME = "PIPELINE";

    private final AgentInvoker invoker;
    private final ObjectMapper json = new ObjectMapper();

    @Override
    public String name() { return NAME; }

    @Override
    public void execute(Long userId, List<AgentGroupMember> members, String goal, SseEmitter emitter) throws IOException {
        log.info("[pipeline] group run start, members={}", members.size());
        int total = 0;
        String context = "";
        StringBuilder finalBuf = new StringBuilder();

        for (AgentGroupMember m : members) {
            if (m.getEnabled() != null && m.getEnabled() == 0) continue;
            total++;

            // step-start
            SseEmitterUtil.send(emitter, "step-start", Map.of(
                    "agentCode", m.getAgentCode(),
                    "role",      m.getRole(),
                    "position",  m.getPosition()
            ));

            // invoke + streaming tokens
            String out = invoker.invokeStreaming(m, goal, context, token -> {
                Map<String, Object> tk = new HashMap<>();
                tk.put("agentCode", m.getAgentCode());
                tk.put("content",   token);
                SseEmitterUtil.send(emitter, "step-token", tk);
            });

            // step-end
            Map<String, Object> end = new HashMap<>();
            end.put("agentCode", m.getAgentCode());
            end.put("output",    out);
            SseEmitterUtil.send(emitter, "step-end", end);

            context = out;
            if (finalBuf.length() > 0) finalBuf.append("\n\n");
            finalBuf.append("[").append(m.getAgentCode()).append("]\n").append(out);
        }

        Map<String, Object> fin = new HashMap<>();
        fin.put("success",     true);
        fin.put("finalAnswer", finalBuf.toString());
        fin.put("totalSteps",  total);
        SseEmitterUtil.send(emitter, "final", fin);
    }
}
