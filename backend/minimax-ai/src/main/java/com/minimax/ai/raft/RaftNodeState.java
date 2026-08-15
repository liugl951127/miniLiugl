package com.minimax.ai.raft;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Raft 节点状态 (V3.5.0)
 *
 * <p>每个节点持有一份, 包含:
 * <ul>
 *   <li>role: 当前角色</li>
 *   <li>currentTerm: 当前任期 (单调增)</li>
 *   <li>votedFor: 本任期投给谁 (null = 未投)</li>
 *   <li>log[]: 日志列表 (按 index 顺序)</li>
 *   <li>commitIndex: 已知提交的索引</li>
 *   <li>lastApplied: 已应用到状态机的最后索引</li>
 *   <li>leaderId: 已知 leader (如存在)</li>
 *   <li>votesReceived: 选举中获得票数 (Candidate 时累计)</li>
 *   <li>electionDeadline: 选举超时截止时间 (ms)</li>
 * </ul>
 *
 * <p>并发: AtomicLong 用于 term, 普通字段 + synchronized 块保护
 */
@Data
public class RaftNodeState {

    /** 本节点 ID */
    private final String nodeId;
    /** 集群节点列表 (用于投票 RPC) */
    private final List<String> peers = new ArrayList<>();

    /** 当前角色 */
    private volatile RaftRole role = RaftRole.FOLLOWER;
    /** 当前任期 */
    private final AtomicLong currentTerm = new AtomicLong(0);
    /** 本任期投给谁 */
    private volatile String votedFor = null;
    /** 日志 (in-memory 副本, 持久化由 mapper 负责) */
    private final List<RaftRpc.LogPayload> log = new ArrayList<>();
    /** 已提交索引 */
    private volatile long commitIndex = 0;
    /** 已应用索引 */
    private volatile long lastApplied = 0;
    /** 已知 leader */
    private volatile String leaderId = null;
    /** 选举获得票数 (Candidate 时) */
    private final AtomicLong votesReceived = new AtomicLong(0);
    /** 选举超时截止时间 (epoch ms) */
    private volatile long electionDeadline = 0L;

    /** 选举超时范围 (ms): 随机 150-300 */
    public static final long ELECTION_TIMEOUT_MIN = 150L;
    public static final long ELECTION_TIMEOUT_MAX = 300L;
    /** 心跳间隔 (ms) */
    public static final long HEARTBEAT_INTERVAL = 50L;

    public RaftNodeState(String nodeId, List<String> peers) {
        this.nodeId = nodeId;
        if (peers != null) this.peers.addAll(peers);
        resetElectionDeadline();
    }

    /**
     * 替换 peers (保留自己)
     */
    public synchronized void setPeers(List<String> newPeers) {
        this.peers.clear();
        if (newPeers != null) {
            newPeers.removeIf(p -> p == null || p.isEmpty());
            this.peers.addAll(newPeers);
        }
    }

    /**
     * 重置选举超时 (随机)
     */
    public void resetElectionDeadline() {
        long range = ELECTION_TIMEOUT_MAX - ELECTION_TIMEOUT_MIN;
        this.electionDeadline = System.currentTimeMillis() + ELECTION_TIMEOUT_MIN + (long) (Math.random() * range);
    }

    /**
     * 集群节点数 (含自己)
     */
    public int clusterSize() {
        return peers == null ? 1 : peers.size();
    }

    /**
     * 多数派大小 (N/2 + 1)
     */
    public int quorum() {
        return clusterSize() / 2 + 1;
    }

    /**
     * 提升任期 (单调增)
     */
    public long incrementTerm() {
        return currentTerm.incrementAndGet();
    }

    /**
     * 设角色 (转换时清状态)
     */
    public void setRole(RaftRole newRole) {
        RaftRole old = this.role;
        this.role = newRole;
        if (newRole == RaftRole.CANDIDATE) {
            votesReceived.set(1); // 投自己 1 票
        } else if (newRole == RaftRole.FOLLOWER) {
            votedFor = null;
        }
    }

    /**
     * 加日志
     */
    public synchronized void appendLog(RaftRpc.LogPayload entry) {
        log.add(entry);
    }

    /**
     * 截断 fromIdx 之后的日志 (用于一致性修复)
     */
    public synchronized void truncateFrom(long fromIdx) {
        log.removeIf(e -> e.getLogIndex() >= fromIdx);
    }

    /**
     * 上一条日志
     */
    public synchronized RaftRpc.LogPayload lastLog() {
        if (log.isEmpty()) return null;
        return log.get(log.size() - 1);
    }

    /**
     * 上一条日志索引
     */
    public long lastLogIndex() {
        RaftRpc.LogPayload last = lastLog();
        return last == null ? 0L : last.getLogIndex();
    }

    /**
     * 上一条日志任期
     */
    public long lastLogTerm() {
        RaftRpc.LogPayload last = lastLog();
        return last == null ? 0L : last.getTerm();
    }
}
