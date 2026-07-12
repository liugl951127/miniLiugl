package com.minimax.ai.framework.group;

/**
 * 智能体角色 (V3.0.3 自研多智能体群)
 *
 * <p>每个 Agent 在群组中扮演一个角色, 不同角色有不同职责:
 * <ul>
 *   <li><b>MANAGER</b> 协调者: 拆解任务, 分配 Worker, 聚合结果, 终止判定</li>
 *   <li><b>WORKER</b> 执行者: 接收任务, 推理, 输出结果</li>
 *   <li><b>CRITIC</b> 评论家: 评审 Worker 输出, 给出修改建议 (可多次迭代)</li>
 *   <li><b>OBSERVER</b> 观察者: 只读, 记录日志不参与决策</li>
 * </ul>
 *
 * <p>典型组合:
 *   - 1 MANAGER + 2-4 WORKER (基础协作)
 *   - 1 MANAGER + 2 WORKER + 1 CRITIC (带审核)
 *   - 1 MANAGER + N WORKER + M CRITIC + K OBSERVER (复杂)
 */
public enum GroupRole {
    /** 协调者: 拆任务 + 派发 + 聚合 + 终止 */
    MANAGER,
    /** 执行者: 干活 */
    WORKER,
    /** 评论家: 评审 worker 输出 */
    CRITIC,
    /** 观察者: 只记录不参与 */
    OBSERVER
}
