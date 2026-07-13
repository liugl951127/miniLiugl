package com.minimax.ai.cluster.raft;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Raft 集群 (V3.5.0)
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>管理多个 RaftNode (同进程模拟多节点)</li>
 *   <li>调度选举超时检查 + 心跳</li>
 *   <li>转发 RPC (RequestVote / AppendEntries)</li>
 *   <li>暴露查询接口: 当前 Leader / 集群状态</li>
 * </ul>
 *
 * <h3>线程模型</h3>
 * <ol>
 *   <li>scheduler: 单线程, 每 50ms 检查所有节点的超时 + 心跳</li>
 *   <li>RPC: 同进程, 直接调用目标节点方法 (零开销)</li>
 * </ol>
 */
@Slf4j
public class RaftCluster {

    /** 节点 ID → RaftNode */
    @Getter
    private final Map<String, RaftNode> nodes = new ConcurrentHashMap<>();

    /** Leader ID (仅 1 个) */
    @Getter
    private volatile String leaderId = null;

    /** 选举超时 (ms) */
    private final long electionTimeoutMs;
    /** 心跳间隔 (ms) */
    private final long heartbeatIntervalMs;

    /** 后台调度器 */
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** 全局 applied log (用于观察) */
    @Getter
    private final List<LogEntry> globalApplied = Collections.synchronizedList(new ArrayList<>());

    public RaftCluster(List<String> nodeIds, long electionTimeoutMs, long heartbeatIntervalMs) {
        this.electionTimeoutMs = electionTimeoutMs;
        this.heartbeatIntervalMs = heartbeatIntervalMs;
        for (String id : nodeIds) {
            RaftNode node = new RaftNode(id, nodeIds, electionTimeoutMs, heartbeatIntervalMs);
            nodes.put(id, node);
        }
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "raft-cluster-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 启动集群调度
     */
    public void start() {
        if (!running.compareAndSet(false, true)) return;
        scheduler.scheduleWithFixedDelay(this::tick, 50, 50, TimeUnit.MILLISECONDS);
        log.info("[RaftCluster] 启动, 节点数={}", nodes.size());
    }

    /**
     * 停止
     */
    public void stop() {
        running.set(false);
        scheduler.shutdownNow();
    }

    /**
     * 主循环: 每 50ms 检查
     */
    private void tick() {
        if (!running.get()) return;
        for (RaftNode n : nodes.values()) {
            // 1. 选举超时检查 (仅 FOLLOWER/CANDIDATE)
            if (n.getState() != NodeState.LEADER && n.isElectionTimeout()) {
                triggerElection(n);
            }
            // 2. LEADER 定期发送心跳/日志
            if (n.getState() == NodeState.LEADER) {
                leaderId = n.getNodeId();
                sendHeartbeatOrAppend(n);
            }
        }
    }

    /**
     * 触发选举
     */
    private void triggerElection(RaftNode node) {
        long newTerm = node.becomeCandidate();
        // 给自己投票 (在 becomeCandidate 已计 1)
        // 向所有 peers 发送 RequestVote
        for (String peerId : node.getPeers()) {
            if (peerId.equals(node.getNodeId())) continue;
            RaftNode peer = nodes.get(peerId);
            if (peer == null) continue;
            RaftRpc.RequestVote req = RaftRpc.RequestVote.builder()
                    .term(newTerm)
                    .candidateId(node.getNodeId())
                    .lastLogIndex(node.getLastLogIndex())
                    .lastLogTerm(node.getLastLogTerm())
                    .build();
            try {
                RaftRpc.RequestVoteResponse resp = peer.handleRequestVote(req);
                // 响应处理
                if (resp.getTerm() > node.getCurrentTerm().get()) {
                    node.becomeFollower(resp.getTerm());
                    return;
                }
                if (resp.isVoteGranted()) {
                    node.recordVote();
                    if (node.hasWonElection() && node.getState() == NodeState.CANDIDATE) {
                        node.becomeLeader();
                        leaderId = node.getNodeId();
                    }
                }
            } catch (Exception e) {
                log.warn("[Raft] RequestVote to {} 失败: {}", peerId, e.getMessage());
            }
        }
    }

    /**
     * LEADER 发送心跳或日志
     */
    private void sendHeartbeatOrAppend(RaftNode leader) {
        for (String peerId : leader.getPeers()) {
            if (peerId.equals(leader.getNodeId())) continue;
            RaftNode peer = nodes.get(peerId);
            if (peer == null) continue;
            Long nextIdx = leader.getNextIndex(peerId);
            if (nextIdx == null) nextIdx = 1L;
            // 1. 一致性检查
            long prevIdx = nextIdx - 1;
            long prevTerm = 0;
            if (prevIdx > 0) {
                LogEntry prev = leader.getLogAt(prevIdx);
                if (prev == null) {
                    // 需要快照, 此处简化为跳过
                    continue;
                }
                prevTerm = prev.getTerm();
            }
            // 2. 准备 entries
            List<LogEntry> entries = new ArrayList<>();
            long myLastIdx = leader.getLastLogIndex();
            for (long i = nextIdx; i <= myLastIdx; i++) {
                LogEntry e = leader.getLogAt(i);
                if (e != null) entries.add(e);
            }
            RaftRpc.AppendEntries req = RaftRpc.AppendEntries.builder()
                    .term(leader.getCurrentTerm().get())
                    .leaderId(leader.getNodeId())
                    .prevLogIndex(prevIdx)
                    .prevLogTerm(prevTerm)
                    .entries(entries)
                    .leaderCommit(leader.getCommitIndex().get())
                    .build();
            try {
                RaftRpc.AppendEntriesResponse resp = peer.handleAppendEntries(req);
                if (resp.getTerm() > leader.getCurrentTerm().get()) {
                    leader.becomeFollower(resp.getTerm());
                    leaderId = null;
                    return;
                }
                if (resp.isSuccess()) {
                    if (!entries.isEmpty()) {
                        leader.recordMatchIndex(peerId, myLastIdx);
                    }
                } else {
                    leader.recordMatchFailure(peerId);
                }
            } catch (Exception e) {
                log.warn("[Raft] AppendEntries to {} 失败: {}", peerId, e.getMessage());
            }
        }
        // 3. 检查是否可以推进 commit
        long candidate = leader.findCommitIndex();
        if (candidate > leader.getCommitIndex().get()) {
            LogEntry e = leader.getLogAt(candidate);
            if (e != null && e.getTerm() == leader.getCurrentTerm().get()) {
                // 在 advanceCommit 之前记录当前 lastApplied, 以便补加到 globalApplied
                long prevApplied = leader.getLastApplied().get();
                leader.advanceCommit(candidate);
                // 应用到全局 (模拟状态机): 从 prevApplied+1 到 candidate
                for (long i = prevApplied + 1; i <= candidate; i++) {
                    LogEntry applied = leader.getLogAt(i);
                    if (applied != null && applied.getIndex() > 0) {
                        globalApplied.add(applied);
                    }
                }
            }
        }
    }

    /**
     * 客户端提交命令 (通过 Leader)
     */
    public long submit(Object command) {
        RaftNode leader = findLeader();
        if (leader == null) {
            log.warn("[Raft] 无 Leader, 命令被拒");
            return -1;
        }
        return leader.submitCommand(command);
    }

    /**
     * 找当前 Leader
     */
    public RaftNode findLeader() {
        for (RaftNode n : nodes.values()) {
            if (n.getState() == NodeState.LEADER) return n;
        }
        return null;
    }

    /**
     * 集群状态摘要
     */
    public Map<String, Object> clusterState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("totalNodes", nodes.size());
        state.put("leaderId", leaderId);
        List<Map<String, Object>> nodeStates = new ArrayList<>();
        for (RaftNode n : nodes.values()) {
            Map<String, Object> ns = new LinkedHashMap<>();
            ns.put("nodeId", n.getNodeId());
            ns.put("state", n.getState());
            ns.put("term", n.getCurrentTerm().get());
            ns.put("logSize", n.getLogEntries().size() - 1);
            ns.put("commitIndex", n.getCommitIndex().get());
            nodeStates.add(ns);
        }
        state.put("nodes", nodeStates);
        return state;
    }

    /**
     * 让某节点掉线 (测试用)
     */
    public void killNode(String nodeId) {
        RaftNode n = nodes.get(nodeId);
        if (n != null && n.getState() == NodeState.LEADER) {
            leaderId = null;
        }
    }

    /**
     * 恢复某节点 (测试用)
     */
    public void reviveNode(String nodeId) {
        // 简化: 重置 lastHeartbeat, 触发后续选举
        RaftNode n = nodes.get(nodeId);
        if (n != null) n.touchHeartbeat();
    }
}
