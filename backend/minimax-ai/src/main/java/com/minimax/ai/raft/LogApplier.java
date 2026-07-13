package com.minimax.ai.raft;

/**
 * 日志应用器接口 (V3.5.0)
 *
 * <p>业务注册: 收到已提交日志时做什么
 *   e.g. 更新分布式配置 / 触发数据迁移 / 记录审计
 */
@FunctionalInterface
public interface LogApplier {
    /**
     * @param term  日志任期
     * @param index 日志索引
     * @param cmd   业务命令 (JSON 字符串)
     */
    void apply(long term, long index, String cmd);
}
