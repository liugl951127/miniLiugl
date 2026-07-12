package com.minimax.ai.framework.group;

/**
 * Agent 执行器 (V3.0.3)
 *
 * <p>每个 Agent 实现此接口, 群组协调器通过此接口调用
 * <p>无状态, 可重入, 线程安全
 *
 * <h3>实现方式</h3>
 * <ul>
 *   <li>{@link SimpleEchoAgent} - 测试用, 直接 echo 输入 (用于单元测试)</li>
 *   <li>{@link LlmBackedAgent} - 调真实 LLM (V3.1+)</li>
 *   <li>{@link FrameworkAgent} - 复用 minimax-ai 的 Framework Agent</li>
 * </ul>
 */
public interface AgentExecutor {

    /**
     * Agent 名 (群组内唯一)
     */
    String name();

    /**
     * 能力描述
     */
    String capability();

    /**
     * 执行任务
     *
     * @param task       任务 (含 input + context)
     * @param sharedMem  共享内存 (可读其它 Agent 写的内容)
     * @return 输出文本
     */
    String execute(GroupTask.SubTask task, GroupSharedMemory sharedMem);
}
