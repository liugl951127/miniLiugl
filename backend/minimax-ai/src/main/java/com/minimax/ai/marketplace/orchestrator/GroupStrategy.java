package com.minimax.ai.marketplace.orchestrator;

import com.minimax.ai.marketplace.AgentGroupMember;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * 多智能体协作策略接口 (T1-backend-orchestrator)
 *
 * <p>3 个内建实现:
 * <ul>
 *   <li>{@link PipelineStrategy} - 顺序执行, 上一步输出作下一步输入</li>
 *   <li>{@link ParallelStrategy} - 并行执行, 合并结果</li>
 *   <li>{@link DebateStrategy}   - MANAGER 提问 → N WORKER 回答 → CRITIC 评分选最佳</li>
 * </ul>
 *
 * <p>event 协议 (SSE 推流):
 * <pre>
 *   step-start  {agentCode, role, position}
 *   step-token  {agentCode, content}
 *   step-end    {agentCode, output}
 *   final       {success, finalAnswer, totalSteps, durationMs}
 *   error       {message}
 * </pre>
 *
 * @author MiniMax
 * @since T1
 */
public interface GroupStrategy {

    /** 策略名 (PIPELINE / PARALLEL / DEBATE) */
    String name();

    /**
     * 执行编排
     *
     * @param userId  触发用户
     * @param members 群成员 (已按 position 排序)
     * @param goal    用户目标
     * @param emitter SSE 推流器
     */
    void execute(Long userId, List<AgentGroupMember> members, String goal, SseEmitter emitter) throws IOException;
}
