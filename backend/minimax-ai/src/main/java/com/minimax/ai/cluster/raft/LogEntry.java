package com.minimax.ai.cluster.raft;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Raft 日志条目 (V3.5.0)
 *
 * <p>每条命令 = 1 个 LogEntry, 状态机按 index 顺序应用.
 * Raft 保证: 已被多数派 commit 的 entry, 一定最终会被所有节点应用.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 日志索引 (从 1 开始, 全局单调递增) */
    private long index;
    /** Leader 任期号 (随 Leader 变更递增) */
    private long term;
    /** 命令 (可序列化的对象, 业务自定义) */
    private Object command;
    /** 创建时间戳 (ms) */
    private long timestamp;

    /**
     * 是否早于另一个 entry
     */
    public boolean isBefore(LogEntry other) {
        return this.index < other.index || (this.index == other.index && this.term < other.term);
    }

    /**
     * 是否冲突 (同 index 但 term 不同)
     */
    public boolean conflictsWith(LogEntry other) {
        return this.index == other.index && this.term != other.term;
    }
}
