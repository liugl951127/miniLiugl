package com.minimax.ai.raft;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Raft RPC 数据结构 (V3.5.0)
 *
 * <h3>RequestVote RPC</h3>
 * 由 Candidate 发起, 拉票:
 *   - term: Candidate 任期
 *   - candidateId: 候选人 ID
 *   - lastLogIndex/term: 候选人最新日志 (供 Follower 决定是否投票)
 * Follower 响应: granted=true/false, term
 *
 * <h3>AppendEntries RPC</h3>
 * 由 Leader 发起, 心跳 + 日志复制:
 *   - term, leaderId
 *   - prevLogIndex/term: 前一条日志 (用于一致性检查)
 *   - entries: 新日志列表 (空 = 心跳)
 *   - leaderCommit: Leader 已知提交位
 * Follower 响应: success=true/false, term, matchIndex
 */
public class RaftRpc {

    /**
     * RequestVote 请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestVote {
        private long term;
        private String candidateId;
        private long lastLogIndex;
        private long lastLogTerm;
    }

    /**
     * RequestVote 响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoteResponse {
        private long term;
        private boolean voteGranted;
        private String voterId;
    }

    /**
     * AppendEntries 请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppendEntries {
        private long term;
        private String leaderId;
        private long prevLogIndex;
        private long prevLogTerm;
        private List<LogPayload> entries;
        private long leaderCommit;
    }

    /**
     * AppendEntries 响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppendResponse {
        private long term;
        private boolean success;
        private long matchIndex;
        private String followerId;
    }

    /**
     * 日志内容
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogPayload {
        private long term;
        private long logIndex;
        private String command;
    }
}
