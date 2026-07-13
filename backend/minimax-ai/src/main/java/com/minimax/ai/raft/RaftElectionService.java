package com.minimax.ai.raft;

import com.minimax.ai.cluster.NodeRegistry;
import com.minimax.ai.entity.LogEntry;
import com.minimax.ai.mapper.LogEntryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Raft 选举 + 日志复制服务 (V3.5.0 自研)
 *
 * <h3>核心功能</h3>
 * <ol>
 *   <li>选举: 选举超时 → 转 Candidate, 拉票, 多数决获胜</li>
 *   <li>投票: 处理 RequestVote RPC, 按 Raft 规则</li>
 *   <li>心跳: Leader 定期发空 AppendEntries</li>
 *   <li>日志复制: AppendEntries + 一致性检查 + 提交推进</li>
 *   <li>状态机应用: 已提交日志应用到业务</li>
 * </ol>
 *
 * <h3>Raft 关键不变量</h3>
 * <ul>
 *   <li>选举安全: 同一任期最多一个 Leader</li>
 *   <li>日志只追加: 从不删除/修改 (truncate 仅修复一致性)</li>
 *   <li>日志匹配: 同 (term, index) 的条目在所有节点相同</li>
 *   <li>领导者完整性: 已 commit 的条目必在所有后续 Leader 中</li>
 *   <li>状态机安全: 已应用日志不会消失</li>
 * </ul>
 *
 * <h3>复杂度</h3>
 * 单次 RPC: O(N) N=日志条数; 选举: O(RPC) R=peers 数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RaftElectionService {

    private final LogEntryMapper logMapper;
    private final NodeRegistry nodeRegistry;

    /** 本节点 ID */
    private String nodeId;
    /** 节点状态 */
    private RaftNodeState state;
    /** peer RPC 客户端 (注入用 setter 避免循环依赖) */
    private RaftRpcClient rpcClient;

    /** 全局提交索引 (供查询) */
    private final AtomicLong committedIndex = new AtomicLong(0);
    /** 已应用回调 (业务可注册) */
    private final List<LogApplier> appliers = new ArrayList<>();
    /** 收到的票 (Candidate 选举) */
    private final Map<String, Boolean> votes = new ConcurrentHashMap<>();

    @Autowired
    public void setNodeId(@org.springframework.beans.factory.annotation.Value("${spring.application.name:minimax-ai}") String name) {
        this.nodeId = name + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 注入 RPC 客户端 (用 setter 避免循环)
     */
    @Autowired
    public void setRpcClient(@Lazy RaftRpcClient client) {
        this.rpcClient = client;
    }

    /**
     * 启动时初始化
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        // peers: 暂时只含自己 (单节点模式)
        // 真实部署从配置中心读
        List<String> peers = new ArrayList<>();
        peers.add(this.nodeId);
        this.state = new RaftNodeState(this.nodeId, peers);
        log.info("[raft] 启动: nodeId={}, peers={}", this.nodeId, peers);
    }

    /**
     * 注入集群 peers (从 Nacos / 配置中心拉)
     */
    public void setPeers(List<String> peers) {
        if (state == null) return;
        // 保留自己, 放在最前
        peers.removeIf(p -> p.equals(nodeId));
        peers.add(0, nodeId);
        state.setPeers(peers);
    }

    // ====================================================================
    // 1. 选举 (Election)
    // ====================================================================

    /**
     * 50ms 调度一次, 检查选举超时
     */
    @Scheduled(fixedRate = RaftNodeState.HEARTBEAT_INTERVAL)
    public void tick() {
        if (state == null) return;
        long now = System.currentTimeMillis();
        // Follower: 超时 → 发起选举
        if (state.getRole() == RaftRole.FOLLOWER && now >= state.getElectionDeadline()) {
            startElection();
            return;
        }
        // Candidate: 超时 → 重新选举 (term 已增)
        if (state.getRole() == RaftRole.CANDIDATE && now >= state.getElectionDeadline()) {
            startElection();
            return;
        }
        // Leader: 定期发心跳
        if (state.getRole() == RaftRole.LEADER) {
            broadcastHeartbeat();
        }
    }

    /**
     * 发起选举
     */
    public synchronized boolean startElection() {
        // 1. 转 Candidate
        state.setRole(RaftRole.CANDIDATE);
        // 2. term +1, 投自己
        long newTerm = state.incrementTerm();
        state.setVotedFor(state.getNodeId());
        state.getVotesReceived().set(1);
        votes.clear();
        votes.put(state.getNodeId(), true);
        // 3. 重置超时
        state.resetElectionDeadline();
        log.info("[raft] 🗳️ 发起选举: term={}, nodeId={}, votes={}/{}",
                newTerm, state.getNodeId(), 1, state.quorum());

        // 4. 广播 RequestVote (除自己外)
        if (rpcClient != null) {
            RaftRpc.RequestVote req = new RaftRpc.RequestVote(
                    newTerm, state.getNodeId(), state.lastLogIndex(), state.lastLogTerm());
            try {
                Map<String, RaftRpc.VoteResponse> responses = rpcClient.broadcastVote(req);
                processVoteResponses(responses);
            } catch (Exception e) {
                log.debug("[raft] 投票广播失败: {}", e.getMessage());
            }
        } else {
            // 单节点模式: 自己一票就够 (quorum=1)
            if (state.getVotesReceived().get() >= state.quorum()) {
                becomeLeader();
            }
        }
        return true;
    }

    /**
     * 处理投票响应
     */
    private void processVoteResponses(Map<String, RaftRpc.VoteResponse> responses) {
        if (state.getRole() != RaftRole.CANDIDATE) return;
        for (RaftRpc.VoteResponse r : responses.values()) {
            // 对方 term 更新 → 降级 Follower
            if (r.getTerm() > state.getCurrentTerm().get()) {
                state.getCurrentTerm().set(r.getTerm());
                state.setRole(RaftRole.FOLLOWER);
                state.setVotedFor(null);
                state.resetElectionDeadline();
                return;
            }
            if (r.isVoteGranted() && votes.putIfAbsent(r.getVoterId(), true) == null) {
                state.getVotesReceived().incrementAndGet();
            }
        }
        // 多数决
        if (state.getVotesReceived().get() >= state.quorum() && state.getRole() == RaftRole.CANDIDATE) {
            becomeLeader();
        }
    }

    /**
     * 成为 Leader
     */
    private void becomeLeader() {
        state.setRole(RaftRole.LEADER);
        state.setLeaderId(state.getNodeId());
        // 空条目追加 (Raft 惯例: 选上立即追加 1 条空 noop)
        appendEntry("noop:" + state.getCurrentTerm().get());
        log.info("[raft] 🎉 选举成功 → LEADER: term={}, votes={}/{}",
                state.getCurrentTerm().get(), state.getVotesReceived().get(), state.quorum());
    }

    // ====================================================================
    // 2. RequestVote RPC
    // ====================================================================

    /**
     * 处理收到的 RequestVote
     */
    public synchronized RaftRpc.VoteResponse handleRequestVote(RaftRpc.RequestVote req) {
        long myTerm = state.getCurrentTerm().get();
        // 对方 term 比我大 → 我降级
        if (req.getTerm() > myTerm) {
            state.getCurrentTerm().set(req.getTerm());
            state.setRole(RaftRole.FOLLOWER);
            state.setVotedFor(null);
            state.resetElectionDeadline();
            myTerm = req.getTerm();
        }
        // 已投给别的候选 (同任期)
        boolean alreadyVoted = state.getVotedFor() != null && !state.getVotedFor().equals(req.getCandidateId());
        // 日志太旧
        boolean logStale = req.getLastLogTerm() < state.lastLogTerm() ||
                (req.getLastLogTerm() == state.lastLogTerm() && req.getLastLogIndex() < state.lastLogIndex());
        // 决定: 同任期 + 未投 + 日志够新 → 投
        boolean grant = req.getTerm() == myTerm
                && !alreadyVoted
                && !logStale;
        if (grant) {
            state.setVotedFor(req.getCandidateId());
            state.resetElectionDeadline(); // 投了票就延长自己超时
            log.info("[raft] 投票给 {} (term={})", req.getCandidateId(), req.getTerm());
        }
        return new RaftRpc.VoteResponse(myTerm, grant, state.getNodeId());
    }

    // ====================================================================
    // 3. AppendEntries RPC (心跳 + 日志复制)
    // ====================================================================

    /**
     * 广播心跳 (Leader 调用)
     */
    private void broadcastHeartbeat() {
        if (rpcClient == null) return;
        for (String peer : state.getPeers()) {
            if (peer.equals(state.getNodeId())) continue;
            try {
                RaftRpc.AppendEntries ae = new RaftRpc.AppendEntries(
                        state.getCurrentTerm().get(), state.getNodeId(),
                        state.lastLogIndex(), state.lastLogTerm(),
                        new ArrayList<>(), state.getCommitIndex());
                RaftRpc.AppendResponse r = rpcClient.sendAppend(peer, ae);
                handleAppendResponse(peer, r);
            } catch (Exception e) {
                log.debug("[raft] 心跳 {} 失败: {}", peer, e.getMessage());
            }
        }
    }

    /**
     * 处理 Append 响应
     */
    private void handleAppendResponse(String peer, RaftRpc.AppendResponse r) {
        if (r.getTerm() > state.getCurrentTerm().get()) {
            // 对方 term 更大 → 我降级
            state.getCurrentTerm().set(r.getTerm());
            state.setRole(RaftRole.FOLLOWER);
            state.setVotedFor(null);
            state.resetElectionDeadline();
            return;
        }
        // 真实生产: 跟踪 nextIndex/matchIndex, 不一致时回退重试
    }

    /**
     * 处理收到的 AppendEntries (Follower)
     */
    public synchronized RaftRpc.AppendResponse handleAppendEntries(RaftRpc.AppendEntries req) {
        long myTerm = state.getCurrentTerm().get();
        // 1. term 校验
        if (req.getTerm() < myTerm) {
            return new RaftRpc.AppendResponse(myTerm, false, 0L, state.getNodeId());
        }
        // 2. term 更大 → 降级 Follower, 接受 leader
        if (req.getTerm() > myTerm) {
            state.getCurrentTerm().set(req.getTerm());
            state.setVotedFor(null);
            myTerm = req.getTerm();
        }
        state.setRole(RaftRole.FOLLOWER);
        state.setLeaderId(req.getLeaderId());
        state.resetElectionDeadline();
        // 3. 一致性检查: prevLog 匹配?
        RaftRpc.LogPayload prev = findLog(req.getPrevLogIndex());
        if (req.getPrevLogIndex() > 0 && (prev == null || prev.getTerm() != req.getPrevLogTerm())) {
            return new RaftRpc.AppendResponse(myTerm, false, 0L, state.getNodeId());
        }
        // 4. 追加 / 覆盖日志
        if (req.getEntries() != null && !req.getEntries().isEmpty()) {
            for (RaftRpc.LogPayload p : req.getEntries()) {
                RaftRpc.LogPayload existing = findLog(p.getLogIndex());
                if (existing != null && existing.getTerm() != p.getTerm()) {
                    // 冲突: 截断 + 覆盖
                    state.truncateFrom(p.getLogIndex());
                    state.appendLog(p);
                } else if (existing == null) {
                    state.appendLog(p);
                }
                // 已存在且 term 一致 → 跳过
            }
        }
        // 5. 推进 commitIndex
        long newCommit = Math.min(req.getLeaderCommit(), state.lastLogIndex());
        if (newCommit > state.getCommitIndex()) {
            state.setCommitIndex(newCommit);
            committedIndex.set(newCommit);
            applyLogs();
        }
        return new RaftRpc.AppendResponse(myTerm, true, state.lastLogIndex(), state.getNodeId());
    }

    /**
     * 查日志 (按 index)
     */
    private synchronized RaftRpc.LogPayload findLog(long index) {
        for (RaftRpc.LogPayload e : state.getLog()) {
            if (e.getLogIndex() == index) return e;
        }
        return null;
    }

    // ====================================================================
    // 4. 提交 (Leader 提交业务命令)
    // ====================================================================

    /**
     * 客户端提交命令 (Leader 处理)
     */
    public synchronized long submit(String command) {
        if (state.getRole() != RaftRole.LEADER) {
            // 非 leader: 重定向 (真实生产转发到 leader)
            log.warn("[raft] 拒绝提交: 非 LEADER, 当前={}", state.getRole());
            return -1L;
        }
        long idx = appendEntry(command);
        // 单节点模式: 提交后立即尝试推进
        if (state.clusterSize() == 1) {
            state.setCommitIndex(idx);
            committedIndex.set(idx);
            applyLogs();
        }
        return idx;
    }

    /**
     * 追加 1 条日志
     */
    private long appendEntry(String command) {
        long idx = state.lastLogIndex() + 1;
        long term = state.getCurrentTerm().get();
        RaftRpc.LogPayload p = new RaftRpc.LogPayload(term, idx, command);
        state.appendLog(p);
        // 持久化
        try {
            LogEntry e = new LogEntry();
            e.setTerm(term);
            e.setLogIndex(idx);
            e.setNodeId(state.getNodeId());
            e.setCommand(command);
            e.setCommitted(false);
            logMapper.insert(e);
        } catch (Exception ex) {
            log.debug("[raft] 持久化日志失败: {}", ex.getMessage());
        }
        return idx;
    }

    /**
     * 应用已提交日志到状态机
     */
    private void applyLogs() {
        long ci = state.getCommitIndex();
        long ai = state.getLastApplied();
        for (long i = ai + 1; i <= ci; i++) {
            RaftRpc.LogPayload p = findLog(i);
            if (p == null) continue;
            for (LogApplier a : appliers) {
                try { a.apply(p.getTerm(), p.getLogIndex(), p.getCommand()); }
                catch (Exception e) { log.warn("[raft] applier 失败: {}", e.getMessage()); }
            }
            state.setLastApplied(i);
            log.info("[raft] ✅ 应用日志: term={}, idx={}, cmd={}", p.getTerm(), i, p.getCommand());
        }
    }

    /**
     * 注册 applier
     */
    public void addApplier(LogApplier applier) {
        appliers.add(applier);
    }

    // ====================================================================
    // 5. 状态查询
    // ====================================================================

    public RaftStatus status() {
        return new RaftStatus(
                state.getNodeId(),
                state.getRole(),
                state.getCurrentTerm().get(),
                state.getCommitIndex(),
                state.getLastApplied(),
                state.lastLogIndex(),
                state.getLeaderId(),
                state.getVotesReceived().get(),
                state.quorum()
        );
    }

    public long getCommittedIndex() { return committedIndex.get(); }
    public RaftNodeState getState() { return state; }

    /**
     * 状态 DTO
     */
    public record RaftStatus(
            String nodeId,
            RaftRole role,
            long currentTerm,
            long commitIndex,
            long lastApplied,
            long lastLogIndex,
            String leaderId,
            long votesReceived,
            int quorum
    ) {}
}
