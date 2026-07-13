package com.minimax.ai.cluster.raft;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Raft 核心节点 (V3.5.0 自研)
 *
 * <h3>关键状态</h3>
 * <ul>
 *   <li>currentTerm  - 当前任期 (单调递增)</li>
 *   <li>votedFor     - 本任期内投给了谁</li>
 *   <li>log[]        - 日志条目 (按 index 排序)</li>
 *   <li>commitIndex  - 已提交的日志 index</li>
 *   <li>lastApplied  - 已应用到状态机的 index</li>
 *   <li>nextIndex[]  - Leader 维护: 下一条要发给 Follower 的 index</li>
 *   <li>matchIndex[] - Leader 维护: Follower 已复制的 index</li>
 * </ul>
 *
 * <h3>状态机规则</h3>
 * <ol>
 *   <li>所有节点启动时为 FOLLOWER</li>
 *   <li>选举超时未收到心跳 → 转 CANDIDATE 发起选举</li>
 *   <li>获多数派票 → 转 LEADER</li>
 *   <li>发现更高 term → 退回 FOLLOWER</li>
 * </ol>
 *
 * <h3>复杂度</h3>
 * 选举 O(N) 投票 RPC; 日志复制 O(N) AppendEntries.
 */
@Slf4j
public class RaftNode {

    /** 节点 ID (集群内唯一) */
    @Getter
    private final String nodeId;

    /** 集群所有节点 (含自己) */
    @Getter
    private final List<String> peers;

    /** 选举超时 (ms) */
    @Getter
    private final long electionTimeoutMs;

    /** 心跳间隔 (ms) */
    @Getter
    private final long heartbeatIntervalMs;

    /** 当前节点状态 */
    @Getter
    private volatile NodeState state = NodeState.FOLLOWER;

    /** 当前任期 (原子, 线程安全) */
    @Getter
    private final AtomicLong currentTerm = new AtomicLong(0);

    /** 本任期投票给了谁 (null = 未投) */
    @Getter
    private volatile String votedFor = null;

    /** 日志: index → LogEntry */
    @Getter
    private final List<LogEntry> logEntries = new ArrayList<>();

    /** 起始占位 (index 0) */
    @Getter
    private final LogEntry sentinel = LogEntry.builder().index(0).term(0).build();

    /** 已 commit 的最大 index */
    @Getter
    private final AtomicLong commitIndex = new AtomicLong(0);

    /** 已 apply 的最大 index */
    @Getter
    private final AtomicLong lastApplied = new AtomicLong(0);

    /** Leader 维护: peer → nextIndex */
    private final Map<String, Long> nextIndex = new ConcurrentHashMap<>();
    /** Leader 维护: peer → matchIndex */
    private final Map<String, Long> matchIndex = new ConcurrentHashMap<>();

    /** 收到的选票数 (本轮选举) */
    @Getter
    private volatile int votesReceived = 0;

    /** 最后一次收到心跳/投票的时间 (ms) */
    @Getter
    private volatile long lastHeartbeat = System.currentTimeMillis();

    /** 已应用的命令回调 (状态机) */
    private final List<LogEntry> appliedEntries = Collections.synchronizedList(new ArrayList<>());

    public RaftNode(String nodeId, List<String> peers,
                    long electionTimeoutMs, long heartbeatIntervalMs) {
        this.nodeId = nodeId;
        this.peers = new ArrayList<>(peers);
        this.electionTimeoutMs = electionTimeoutMs;
        this.heartbeatIntervalMs = heartbeatIntervalMs;
        // 占位 entry (index 0, 方便 index 计算)
        this.logEntries.add(sentinel);
        // 初始化 nextIndex/matchIndex
        for (String p : peers) {
            if (!p.equals(nodeId)) {
                nextIndex.put(p, 1L);
                matchIndex.put(p, 0L);
            }
        }
    }

    /**
     * 获取最后一条日志
     */
    public LogEntry getLastLogEntry() {
        return logEntries.isEmpty() ? sentinel : logEntries.get(logEntries.size() - 1);
    }

    /**
     * 获取最后日志 index
     */
    public long getLastLogIndex() {
        return getLastLogEntry().getIndex();
    }

    /**
     * 获取最后日志 term
     */
    public long getLastLogTerm() {
        return getLastLogEntry().getTerm();
    }

    /**
     * 根据 index 取日志
     */
    public LogEntry getLogAt(long index) {
        int idx = (int) index;
        if (idx < 0 || idx >= logEntries.size()) return null;
        return logEntries.get(idx);
    }

    /**
     * 检查选举超时
     */
    public boolean isElectionTimeout() {
        return System.currentTimeMillis() - lastHeartbeat > electionTimeoutMs;
    }

    /**
     * 推进心跳时间
     */
    public void touchHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }

    /**
     * 转为 FOLLOWER (发现更高 term 或收到合法 Leader)
     */
    public void becomeFollower(long newTerm) {
        this.state = NodeState.FOLLOWER;
        this.currentTerm.set(newTerm);
        this.votedFor = null;
        this.votesReceived = 0;
        touchHeartbeat();
        log.debug("[Raft-{}] → FOLLOWER, term={}", nodeId, newTerm);
    }

    /**
     * 转为 CANDIDATE (发起选举)
     */
    public long becomeCandidate() {
        this.state = NodeState.CANDIDATE;
        long newTerm = currentTerm.incrementAndGet();
        this.votedFor = nodeId;  // 投自己
        this.votesReceived = 1;  // 自带 1 票
        touchHeartbeat();
        log.info("[Raft-{}] → CANDIDATE, term={}", nodeId, newTerm);
        return newTerm;
    }

    /**
     * 转为 LEADER (获多数派票)
     */
    public void becomeLeader() {
        this.state = NodeState.LEADER;
        // 重置 nextIndex/matchIndex
        long lastIdx = getLastLogIndex();
        for (String p : peers) {
            if (!p.equals(nodeId)) {
                nextIndex.put(p, lastIdx + 1);
                matchIndex.put(p, 0L);
            }
        }
        touchHeartbeat();
        log.info("[Raft-{}] → LEADER, term={}", nodeId, currentTerm.get());
    }

    /**
     * 处理 RequestVote RPC
     *
     * <p>投票规则 (Raft 论文 §5.4):
     * <ol>
     *   <li>rpc.term < currentTerm → 拒绝</li>
     *   <li>votedFor == null || votedFor == candidateId → 同意</li>
     *   <li>候选人日志至少和自己一样新 (lastLogTerm > 自己的 || lastLogIndex >= 自己的) → 同意</li>
     * </ol>
     */
    public RaftRpc.RequestVoteResponse handleRequestVote(RaftRpc.RequestVote req) {
        long myTerm = currentTerm.get();
        // 1. 任期检查
        if (req.getTerm() < myTerm) {
            return RaftRpc.RequestVoteResponse.builder()
                    .term(myTerm).voteGranted(false).build();
        }
        // 2. 更高 term → 退回 Follower
        if (req.getTerm() > myTerm) {
            becomeFollower(req.getTerm());
        }
        // 3. 投票规则
        boolean canVote = (votedFor == null || votedFor.equals(req.getCandidateId()));
        boolean logUpToDate = isLogUpToDate(req.getLastLogIndex(), req.getLastLogTerm());
        if (canVote && logUpToDate) {
            votedFor = req.getCandidateId();
            touchHeartbeat();
            log.info("[Raft-{}] 投票给 {}, term={}", nodeId, req.getCandidateId(), req.getTerm());
            return RaftRpc.RequestVoteResponse.builder()
                    .term(currentTerm.get()).voteGranted(true).build();
        }
        return RaftRpc.RequestVoteResponse.builder()
                .term(currentTerm.get()).voteGranted(false).build();
    }

    /**
     * 候选人日志是否"至少和自己一样新"
     */
    private boolean isLogUpToDate(long lastLogIndex, long lastLogTerm) {
        long myLastTerm = getLastLogTerm();
        long myLastIndex = getLastLogIndex();
        if (lastLogTerm != myLastTerm) {
            return lastLogTerm > myLastTerm;
        }
        return lastLogIndex >= myLastIndex;
    }

    /**
     * 处理 AppendEntries RPC (心跳 + 日志复制)
     */
    public RaftRpc.AppendEntriesResponse handleAppendEntries(RaftRpc.AppendEntries req) {
        long myTerm = currentTerm.get();
        // 1. 任期检查
        if (req.getTerm() < myTerm) {
            return RaftRpc.AppendEntriesResponse.builder()
                    .term(myTerm).success(false).build();
        }
        // 2. 更高 term 或合法 Leader → 退回 Follower
        if (req.getTerm() > myTerm || state != NodeState.FOLLOWER) {
            becomeFollower(req.getTerm());
        }
        touchHeartbeat();

        // 3. 一致性检查: prevLogIndex 处的日志 term 必须匹配
        if (req.getPrevLogIndex() > 0) {
            LogEntry prev = getLogAt(req.getPrevLogIndex());
            if (prev == null || prev.getTerm() != req.getPrevLogTerm()) {
                // 加速回退: 找到冲突 term 的第一个 index
                long conflictIdx = req.getPrevLogIndex();
                long conflictTerm2 = prev != null ? prev.getTerm() : 0;
                return RaftRpc.AppendEntriesResponse.builder()
                        .term(currentTerm.get()).success(false)
                        .conflictIndex(conflictIdx).conflictTerm(conflictTerm2).build();
            }
        }

        // 4. 追加新 entries (删除冲突, 追加新条目)
        long currentIdx = req.getPrevLogIndex();
        for (LogEntry e : req.getEntries()) {
            currentIdx++;
            if (currentIdx < logEntries.size()) {
                LogEntry existing = logEntries.get((int) currentIdx);
                if (existing.getTerm() != e.getTerm()) {
                    // 冲突: 删除此 index 及之后所有
                    while (logEntries.size() > currentIdx) logEntries.remove(logEntries.size() - 1);
                    logEntries.add(e);
                }
                // term 相同: 已存在, 跳过
            } else {
                logEntries.add(e);
            }
        }

        // 5. 更新 commitIndex
        if (req.getLeaderCommit() > commitIndex.get()) {
            long newCommit = Math.min(req.getLeaderCommit(), getLastLogIndex());
            advanceCommit(newCommit);
        }
        return RaftRpc.AppendEntriesResponse.builder()
                .term(currentTerm.get()).success(true).build();
    }

    /**
     * 推进 commitIndex 并应用
     */
    public void advanceCommit(long newCommit) {
        long old = commitIndex.get();
        if (newCommit > old && newCommit <= getLastLogIndex()) {
            commitIndex.set(newCommit);
            applyCommitted();
        }
    }

    /**
     * 应用已 commit 但未 applied 的日志
     */
    public void applyCommitted() {
        long applied = lastApplied.get();
        long commit = commitIndex.get();
        while (applied < commit) {
            applied++;
            LogEntry e = getLogAt(applied);
            if (e != null && e.getIndex() > 0) {
                appliedEntries.add(e);
            }
        }
        lastApplied.set(applied);
    }

    /**
     * 客户端提交命令 (仅 Leader 可调用)
     *
     * @return LogEntry index (负数表示非 Leader 拒绝)
     */
    public long submitCommand(Object command) {
        if (state != NodeState.LEADER) return -1;
        long newIdx = getLastLogIndex() + 1;
        LogEntry entry = LogEntry.builder()
                .index(newIdx)
                .term(currentTerm.get())
                .command(command)
                .timestamp(System.currentTimeMillis())
                .build();
        logEntries.add(entry);
        log.info("[Raft-{}] LEADER 提交命令 → index={}, term={}", nodeId, newIdx, entry.getTerm());
        return newIdx;
    }

    /**
     * 记录收到选票 (CANDIDATE 用)
     */
    public void recordVote() {
        votesReceived++;
    }

    /**
     * 多数派数量 (N/2 + 1)
     */
    public int quorumSize() {
        return peers.size() / 2 + 1;
    }

    /**
     * 是否获多数派票
     */
    public boolean hasWonElection() {
        return votesReceived >= quorumSize();
    }

    /**
     * 复制 index 到 peer (LEADER 用)
     */
    public void recordMatchIndex(String peerId, long index) {
        matchIndex.put(peerId, index);
        nextIndex.put(peerId, index + 1);
    }

    /**
     * 复制失败, nextIndex 回退 (LEADER 用)
     */
    public void recordMatchFailure(String peerId) {
        Long cur = nextIndex.get(peerId);
        if (cur != null && cur > 1) {
            nextIndex.put(peerId, cur - 1);
        }
    }

    public Long getNextIndex(String peerId) {
        return nextIndex.get(peerId);
    }

    public Long getMatchIndex(String peerId) {
        return matchIndex.get(peerId);
    }

    /**
     * 找出可提交的 index (多数派 matchIndex 最小值)
     */
    public long findCommitIndex() {
        List<Long> allMatches = new ArrayList<>(matchIndex.values());
        allMatches.add(getLastLogIndex());  // 算 Leader 自己
        Collections.sort(allMatches);
        int n = allMatches.size();
        int idx = n - quorumSize();
        if (idx < 0) return commitIndex.get();
        return allMatches.get(idx);
    }

    /**
     * 取出已应用的 entry (测试用)
     */
    public List<LogEntry> getAppliedEntries() {
        synchronized (appliedEntries) {
            return new ArrayList<>(appliedEntries);
        }
    }
}
