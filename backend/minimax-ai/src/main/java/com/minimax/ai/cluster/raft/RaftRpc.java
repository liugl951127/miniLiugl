package com.minimax.ai.cluster.raft;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Raft RPC 消息集合 (V3.5.0)
 *
 * <p>本类定义所有 RPC 消息结构, 用 sealed-style 静态内部类.
 * 实际生产用 Netty/gRPC, 这里为简化用同进程对象传递 (单测可跑).
 */
public final class RaftRpc {
    private RaftRpc() {}

    /**
     * RequestVote RPC (CANDIDATE → 其他节点)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestVote {
        /** 候选人任期 */
        private long term;
        /** 候选人 ID */
        private String candidateId;
        /** 候选人最后日志 index */
        private long lastLogIndex;
        /** 候选人最后日志 term */
        private long lastLogTerm;
    }

    /**
     * RequestVote 响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestVoteResponse {
        /** 当前任期 (供候选人更新) */
        private long term;
        /** true = 投票给该候选人 */
        private boolean voteGranted;
    }

    /**
     * AppendEntries RPC (LEADER → FOLLOWER, 心跳或日志复制)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppendEntries {
        /** Leader 任期 */
        private long term;
        /** Leader ID */
        private String leaderId;
        /** 前一条日志 index (一致性检查) */
        private long prevLogIndex;
        /** 前一条日志 term */
        private long prevLogTerm;
        /** 要追加的日志 (心跳则为空) */
        private List<LogEntry> entries;
        /** Leader 已 commit 的 index */
        private long leaderCommit;
    }

    /**
     * AppendEntries 响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppendEntriesResponse {
        /** 当前任期 */
        private long term;
        /** true = 追加成功 */
        private boolean success;
        /** 失败时的冲突 index (加速回退) */
        private long conflictIndex;
        /** 失败时的冲突 term */
        private long conflictTerm;
    }

    /**
     * InstallSnapshot RPC (LEADER → FOLLOWER, 追上落后节点)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstallSnapshot {
        private long term;
        private String leaderId;
        private long lastIncludedIndex;
        private long lastIncludedTerm;
        private byte[] data;
    }

    /**
     * InstallSnapshot 响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstallSnapshotResponse {
        private long term;
        private boolean success;
    }

    /**
     * 空 entries 列表常量 (心跳用)
     */
    public static final List<LogEntry> EMPTY_ENTRIES = Collections.emptyList();
}
