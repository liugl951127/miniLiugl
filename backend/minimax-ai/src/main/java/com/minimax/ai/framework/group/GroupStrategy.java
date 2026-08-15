package com.minimax.ai.framework.group;

/**
 * 群组协作策略 (V3.0.3 自研)
 *
 * <p>决定多 Agent 之间的协作模式:
 * <ul>
 *   <li><b>PIPELINE</b> 流水线: A → B → C (顺序执行, 上一步输出作为下一步输入)</li>
 *   <li><b>DEBATE</b>   辩论: 多 Agent 并行提议, 互相 review, 收敛达成共识</li>
 *   <li><b>VOTE</b>     投票: 多 Agent 并行独立出答案, 多数决 / 加权决</li>
 *   <li><b>SWARM</b>    群智: 多 Agent 自由贡献, 由 manager 择优</li>
 * </ul>
 *
 * <p>每种策略由 {@link GroupOrchestrator} 解释执行
 */
public enum GroupStrategy {
    /** 流水线 (顺序) */
    PIPELINE,
    /** 辩论 (迭代收敛) */
    DEBATE,
    /** 投票 (并行 + 多数决) */
    VOTE,
    /** 群智 (自由贡献) */
    SWARM
}
