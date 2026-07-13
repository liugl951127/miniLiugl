package com.minimax.ai.raft;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Raft 选举 + 日志复制 (V3.5.0) 单元测试
 */
class RaftElectionTest {

    private RaftElectionService service;

    @BeforeEach
    void setUp() {
        service = new RaftElectionService(null, null);
        // 手动 init: 模拟单节点集群
        java.lang.reflect.Method m;
        try {
            m = RaftElectionService.class.getDeclaredMethod("init");
            m.setAccessible(true);
        } catch (Exception e) { /* ignore */ }
        // 反射注入 nodeId
        try {
            java.lang.reflect.Field f = RaftElectionService.class.getDeclaredField("nodeId");
            f.setAccessible(true);
            f.set(service, "test-node-1");
        } catch (Exception e) { fail(e.getMessage()); }
        // 反射 init state
        try {
            java.lang.reflect.Field f = RaftElectionService.class.getDeclaredField("state");
            f.setAccessible(true);
            f.set(service, new RaftNodeState("test-node-1", List.of("test-node-1")));
        } catch (Exception e) { fail(e.getMessage()); }
    }

    /**
     * 测试 1: 初始状态是 Follower
     */
    @Test
    @DisplayName("1. 初始状态 = FOLLOWER")
    void testInitialRole() {
        assertEquals(RaftRole.FOLLOWER, service.getState().getRole());
        assertEquals(0L, service.getState().getCurrentTerm().get());
    }

    /**
     * 测试 2: 单节点模式 startElection 立即成功
     */
    @Test
    @DisplayName("2. 单节点 startElection 立即成功 (quorum=1)")
    void testSingleNodeElection() {
        // rpcClient 为 null, 单节点: 自己一票就够
        boolean ok = service.startElection();
        assertTrue(ok);
        // term +1
        assertEquals(1L, service.getState().getCurrentTerm().get());
        // role = LEADER
        assertEquals(RaftRole.LEADER, service.getState().getRole());
    }

    /**
     * 测试 3: 多数决 (3 节点 quorum=2)
     */
    @Test
    @DisplayName("3. 多数决 (3 节点 quorum=2)")
    void testQuorum() {
        // 重设 state 为 3 节点
        try {
            java.lang.reflect.Field f = RaftElectionService.class.getDeclaredField("state");
            f.setAccessible(true);
            f.set(service, new RaftNodeState("test-node-1", List.of("test-node-1", "test-node-2", "test-node-3")));
        } catch (Exception e) { fail(e.getMessage()); }
        assertEquals(2, service.getState().quorum());
    }

    /**
     * 测试 4: RequestVote: 同一任期, 未投过, 日志够新 → 同意
     */
    @Test
    @DisplayName("4. RequestVote: 同任期 + 未投 + 日志新 → 同意")
    void testVoteGrant() {
        // 候选 term=1
        RaftRpc.RequestVote req = new RaftRpc.RequestVote(1L, "cand-1", 0L, 0L);
        RaftRpc.VoteResponse resp = service.handleRequestVote(req);
        assertTrue(resp.isVoteGranted());
        assertEquals(1L, resp.getTerm());
    }

    /**
     * 测试 5: RequestVote: 同任期 + 已投过 → 拒绝
     */
    @Test
    @DisplayName("5. RequestVote: 同任期已投别人 → 拒绝")
    void testVoteAlreadyVoted() {
        // 先投 A
        service.handleRequestVote(new RaftRpc.RequestVote(1L, "A", 0L, 0L));
        // 再投 B
        RaftRpc.VoteResponse resp2 = service.handleRequestVote(new RaftRpc.RequestVote(1L, "B", 0L, 0L));
        assertFalse(resp2.isVoteGranted());
    }

    /**
     * 测试 6: RequestVote: 对方 term 更新 → 降级 Follower
     */
    @Test
    @DisplayName("6. 更高 term → 自动降级 FOLLOWER")
    void testHigherTermDowngrade() {
        // term 5 来了
        RaftRpc.RequestVote req = new RaftRpc.RequestVote(5L, "new-leader", 0L, 0L);
        service.handleRequestVote(req);
        assertEquals(5L, service.getState().getCurrentTerm().get());
        assertEquals(RaftRole.FOLLOWER, service.getState().getRole());
    }

    /**
     * 测试 7: RequestVote: 候选日志太旧 → 拒绝
     */
    @Test
    @DisplayName("7. 候选日志太旧 → 拒绝")
    void testVoteLogStale() {
        // 我有 term 5
        service.handleRequestVote(new RaftRpc.RequestVote(5L, "old-leader", 10L, 5L));
        // 候选 term 5 但 lastLogIndex=3
        RaftRpc.VoteResponse resp = service.handleRequestVote(new RaftRpc.RequestVote(5L, "new-cand", 3L, 5L));
        assertFalse(resp.isVoteGranted(), "日志太旧应拒绝");
    }

    /**
     * 测试 8: AppendEntries: 空 entries (心跳) → 接受
     */
    @Test
    @DisplayName("8. 心跳 (空 entries) → 接受")
    void testHeartbeat() {
        RaftRpc.AppendEntries ae = new RaftRpc.AppendEntries(1L, "leader-1", 0L, 0L, List.of(), 0L);
        RaftRpc.AppendResponse r = service.handleAppendEntries(ae);
        assertTrue(r.isSuccess());
        assertEquals("leader-1", service.getState().getLeaderId());
    }

    /**
     * 测试 9: AppendEntries: term 比我小 → 拒绝
     */
    @Test
    @DisplayName("9. AppendEntries term 比我小 → 拒绝")
    void testAppendLowerTerm() {
        // 我 term=5
        service.getState().getCurrentTerm().set(5L);
        RaftRpc.AppendEntries ae = new RaftRpc.AppendEntries(3L, "old-leader", 0L, 0L, List.of(), 0L);
        RaftRpc.AppendResponse r = service.handleAppendEntries(ae);
        assertFalse(r.isSuccess());
    }

    /**
     * 测试 10: AppendEntries: 写入日志并提交
     */
    @Test
    @DisplayName("10. AppendEntries 写入 1 条日志 + 提交")
    void testAppendAndCommit() {
        List<RaftRpc.LogPayload> entries = List.of(
                new RaftRpc.LogPayload(1L, 1L, "set x=1"));
        RaftRpc.AppendEntries ae = new RaftRpc.AppendEntries(1L, "leader-1", 0L, 0L, entries, 1L);
        service.handleAppendEntries(ae);
        assertEquals(1L, service.getState().getCommitIndex());
        assertEquals(1L, service.getState().getLastApplied());
    }

    /**
     * 测试 11: 非 Leader 拒绝提交
     */
    @Test
    @DisplayName("11. 非 LEADER 拒绝 submit")
    void testSubmitNotLeader() {
        // 当前是 FOLLOWER
        long idx = service.submit("test cmd");
        assertEquals(-1L, idx);
    }

    /**
     * 测试 12: Leader submit
     */
    @Test
    @DisplayName("12. LEADER 提交 → logIndex 单调增")
    void testSubmitAsLeader() {
        service.startElection();
        // 现在是 LEADER. startElection 会加 1 条 noop
        long noopIdx = service.getState().lastLogIndex();
        long idx1 = service.submit("cmd-1");
        long idx2 = service.submit("cmd-2");
        assertTrue(idx1 > noopIdx);
        assertTrue(idx2 > idx1);
        // 2 个 submit + 1 个 noop = 3
        assertEquals(noopIdx + 2L, service.getState().lastLogIndex());
    }

    /**
     * 测试 13: 状态 DTO
     */
    @Test
    @DisplayName("13. 状态 DTO 字段完整")
    void testStatusDto() {
        service.startElection();
        RaftElectionService.RaftStatus s = service.status();
        assertEquals("test-node-1", s.nodeId());
        assertEquals(RaftRole.LEADER, s.role());
        assertEquals(1L, s.currentTerm());
        assertEquals(1, s.quorum());
    }

    /**
     * 测试 14: LogApplier 触发
     */
    @Test
    @DisplayName("14. LogApplier 在提交时触发")
    void testApplierFires() {
        AtomicInteger count = new AtomicInteger();
        service.addApplier((term, index, cmd) -> count.incrementAndGet());
        // 直接提交 (走 handleAppendEntries)
        List<RaftRpc.LogPayload> entries = List.of(new RaftRpc.LogPayload(1L, 1L, "test"));
        service.handleAppendEntries(new RaftRpc.AppendEntries(1L, "leader-1", 0L, 0L, entries, 1L));
        assertEquals(1, count.get());
    }

    /**
     * 测试 15: RaftRole 枚举
     */
    @Test
    @DisplayName("15. RaftRole 3 个值")
    void testRoleEnum() {
        assertEquals(3, RaftRole.values().length);
        assertNotNull(RaftRole.valueOf("FOLLOWER"));
        assertNotNull(RaftRole.valueOf("CANDIDATE"));
        assertNotNull(RaftRole.valueOf("LEADER"));
    }

    /**
     * 测试 16: term 单调增
     */
    @Test
    @DisplayName("16. term 单调增")
    void testTermMonotonic() {
        long t1 = service.getState().incrementTerm();
        long t2 = service.getState().incrementTerm();
        assertEquals(t1 + 1, t2);
    }

    /**
     * 测试 17: 设置 peers
     */
    @Test
    @DisplayName("17. setPeers (含自己)")
    void testSetPeers() {
        // setPeers 自动保留自己
        service.setPeers(new ArrayList<>(List.of("a", "b", "c")));
        // peers 应含 4 个 (a/b/c + 自己 test-node-1)
        assertEquals(4, service.getState().clusterSize());
    }

    /**
     * 测试 18: prevLogIndex 不匹配 → 拒绝
     */
    @Test
    @DisplayName("18. prevLogIndex 不匹配 → 拒绝")
    void testAppendPrevLogMismatch() {
        // 我没日志, leader 说 prev=5
        RaftRpc.AppendEntries ae = new RaftRpc.AppendEntries(1L, "leader-1", 5L, 1L, List.of(), 0L);
        RaftRpc.AppendResponse r = service.handleAppendEntries(ae);
        assertFalse(r.isSuccess());
    }
}
