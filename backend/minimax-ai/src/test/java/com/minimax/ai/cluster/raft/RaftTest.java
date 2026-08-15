package com.minimax.ai.cluster.raft;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Raft 集群 (V3.5.0) 单元测试
 */
class RaftTest {

    private RaftCluster cluster;

    @BeforeEach
    void setUp() {
        cluster = null;
    }

    @AfterEach
    void tearDown() {
        if (cluster != null) cluster.stop();
    }

    /**
     * 测试 1: 初始状态全 FOLLOWER
     */
    @Test
    @DisplayName("1. 初始所有节点为 FOLLOWER")
    void testInitialState() {
        List<String> ids = Arrays.asList("n1", "n2", "n3");
        for (String id : ids) {
            RaftNode n = new RaftNode(id, ids, 1000, 200);
            assertEquals(NodeState.FOLLOWER, n.getState());
            assertEquals(0, n.getCurrentTerm().get());
            assertEquals(0, n.getLogEntries().size() - 1);  // 只有 sentinel
        }
    }

    /**
     * 测试 2: 多数派计算
     */
    @Test
    @DisplayName("2. 多数派 (3 节点 → 2)")
    void testQuorumSize() {
        RaftNode n3 = new RaftNode("n1", Arrays.asList("n1", "n2", "n3"), 1000, 200);
        assertEquals(2, n3.quorumSize());
        RaftNode n5 = new RaftNode("n1", Arrays.asList("n1", "n2", "n3", "n4", "n5"), 1000, 200);
        assertEquals(3, n5.quorumSize());
    }

    /**
     * 测试 3: 选举 → 1 个 CANDIDATE
     */
    @Test
    @DisplayName("3. becomeCandidate() 任期+1, 投自己")
    void testBecomeCandidate() {
        RaftNode n = new RaftNode("n1", Arrays.asList("n1", "n2", "n3"), 1000, 200);
        assertEquals(0, n.getCurrentTerm().get());
        long newTerm = n.becomeCandidate();
        assertEquals(1, newTerm);
        assertEquals("n1", n.getVotedFor());
        assertEquals(1, n.getVotesReceived());
        assertEquals(NodeState.CANDIDATE, n.getState());
    }

    /**
     * 测试 4: 选举胜出 → LEADER
     */
    @Test
    @DisplayName("4. 获多数派票 → LEADER")
    void testBecomeLeader() {
        RaftNode n = new RaftNode("n1", Arrays.asList("n1", "n2", "n3"), 1000, 200);
        n.becomeCandidate();
        n.recordVote();
        assertTrue(n.hasWonElection(), "2 票 (自己+1) 应满足多数");
        n.becomeLeader();
        assertEquals(NodeState.LEADER, n.getState());
    }

    /**
     * 测试 5: RequestVote 任期检查
     */
    @Test
    @DisplayName("5. RequestVote 旧任期拒绝")
    void testRequestVoteOldTerm() {
        RaftNode n = new RaftNode("n1", Arrays.asList("n1", "n2", "n3"), 1000, 200);
        n.getCurrentTerm().set(5);
        RaftRpc.RequestVote req = RaftRpc.RequestVote.builder()
                .term(3).candidateId("n2")
                .lastLogIndex(0).lastLogTerm(0)
                .build();
        RaftRpc.RequestVoteResponse resp = n.handleRequestVote(req);
        assertFalse(resp.isVoteGranted());
        assertEquals(5, resp.getTerm());
    }

    /**
     * 测试 6: RequestVote 同一任期只投一票
     */
    @Test
    @DisplayName("6. RequestVote 同任期只投一票")
    void testRequestVoteOnlyOnce() {
        RaftNode n = new RaftNode("n1", Arrays.asList("n1", "n2", "n3"), 1000, 200);
        RaftRpc.RequestVote req1 = RaftRpc.RequestVote.builder()
                .term(1).candidateId("n2")
                .lastLogIndex(0).lastLogTerm(0).build();
        RaftRpc.RequestVoteResponse r1 = n.handleRequestVote(req1);
        assertTrue(r1.isVoteGranted());

        RaftRpc.RequestVote req2 = RaftRpc.RequestVote.builder()
                .term(1).candidateId("n3")
                .lastLogIndex(0).lastLogTerm(0).build();
        RaftRpc.RequestVoteResponse r2 = n.handleRequestVote(req2);
        assertFalse(r2.isVoteGranted(), "已投给 n2, 应拒绝 n3");
    }

    /**
     * 测试 7: 更高 term 触发退回 FOLLOWER
     */
    @Test
    @DisplayName("7. 更高 term → 退回 FOLLOWER")
    void testStepDownOnHigherTerm() {
        RaftNode n = new RaftNode("n1", Arrays.asList("n1", "n2", "n3"), 1000, 200);
        n.becomeCandidate();
        n.becomeLeader();
        assertEquals(NodeState.LEADER, n.getState());

        RaftRpc.AppendEntries ae = RaftRpc.AppendEntries.builder()
                .term(99).leaderId("n2")
                .prevLogIndex(0).prevLogTerm(0)
                .entries(RaftRpc.EMPTY_ENTRIES)
                .leaderCommit(0).build();
        RaftRpc.AppendEntriesResponse resp = n.handleAppendEntries(ae);
        assertEquals(NodeState.FOLLOWER, n.getState());
        assertEquals(99, n.getCurrentTerm().get());
    }

    /**
     * 测试 8: 客户端提交命令 (仅 LEADER)
     */
    @Test
    @DisplayName("8. submitCommand 仅 LEADER 成功")
    void testSubmitCommand() {
        RaftNode n = new RaftNode("n1", Arrays.asList("n1", "n2", "n3"), 1000, 200);
        long idx1 = n.submitCommand("cmd1");
        assertEquals(-1, idx1, "FOLLOWER 提交应被拒");

        n.becomeLeader();
        long idx2 = n.submitCommand("cmd1");
        assertEquals(1, idx2);
        long idx3 = n.submitCommand("cmd2");
        assertEquals(2, idx3);
        assertEquals(2, n.getLogEntries().size() - 1);
    }

    /**
     * 测试 9: AppendEntries 追加日志
     */
    @Test
    @DisplayName("9. AppendEntries 追加日志")
    void testAppendEntries() {
        RaftNode n = new RaftNode("n1", Arrays.asList("n1", "n2", "n3"), 1000, 200);
        n.becomeLeader();
        LogEntry e1 = LogEntry.builder().index(1).term(1).command("cmd1").build();
        LogEntry e2 = LogEntry.builder().index(2).term(1).command("cmd2").build();
        RaftRpc.AppendEntries ae = RaftRpc.AppendEntries.builder()
                .term(1).leaderId("n1")
                .prevLogIndex(0).prevLogTerm(0)
                .entries(Arrays.asList(e1, e2))
                .leaderCommit(0).build();
        RaftRpc.AppendEntriesResponse resp = n.handleAppendEntries(ae);
        // LEADER 处理自己的 AppendEntries 仍应成功
        assertTrue(resp.isSuccess());
    }

    /**
     * 测试 10: 日志冲突删除
     */
    @Test
    @DisplayName("10. 日志冲突 → 删除并替换")
    void testLogConflict() {
        RaftNode n = new RaftNode("n1", Arrays.asList("n1", "n2", "n3"), 1000, 200);
        // 旧日志
        n.getLogEntries().add(LogEntry.builder().index(1).term(1).command("old").build());
        n.getLogEntries().add(LogEntry.builder().index(2).term(1).command("old").build());
        // 新的 AppendEntries 在 index 2 用 term 2 替换
        LogEntry e2 = LogEntry.builder().index(2).term(2).command("new").build();
        RaftRpc.AppendEntries ae = RaftRpc.AppendEntries.builder()
                .term(2).leaderId("n2")
                .prevLogIndex(1).prevLogTerm(1)
                .entries(Arrays.asList(e2))
                .leaderCommit(0).build();
        RaftRpc.AppendEntriesResponse resp = n.handleAppendEntries(ae);
        assertTrue(resp.isSuccess());
        assertEquals(2, n.getLogAt(2).getTerm());
        assertEquals("new", n.getLogAt(2).getCommand());
    }

    /**
     * 测试 11: 选举超时检测
     */
    @Test
    @DisplayName("11. 选举超时检测")
    void testElectionTimeout() {
        RaftNode n = new RaftNode("n1", Arrays.asList("n1", "n2", "n3"), 50, 25);
        assertFalse(n.isElectionTimeout());
        try { TimeUnit.MILLISECONDS.sleep(80); } catch (Exception ignored) {}
        assertTrue(n.isElectionTimeout());
    }

    /**
     * 测试 12: 集群选举 (E2E)
     */
    @Test
    @DisplayName("12. 集群 3 节点 → 选出 1 Leader")
    void testClusterElection() throws Exception {
        cluster = new RaftCluster(Arrays.asList("n1", "n2", "n3"), 200, 80);
        cluster.start();
        TimeUnit.SECONDS.sleep(2);
        RaftNode leader = cluster.findLeader();
        assertNotNull(leader, "2s 内应选出 Leader");
        assertEquals(leader.getNodeId(), cluster.getLeaderId());
        Map<String, Object> state = cluster.clusterState();
        assertEquals(3, state.get("totalNodes"));
        assertNotNull(state.get("leaderId"));
    }

    /**
     * 测试 13: 集群提交命令 (E2E)
     */
    @Test
    @DisplayName("13. 集群提交命令 → 复制到多数")
    void testClusterSubmit() throws Exception {
        cluster = new RaftCluster(Arrays.asList("n1", "n2", "n3"), 200, 80);
        cluster.start();
        TimeUnit.SECONDS.sleep(2);
        long idx = cluster.submit("hello-raft");
        assertTrue(idx > 0, "LEADER 应接受命令, index=" + idx);
        TimeUnit.MILLISECONDS.sleep(500);
        // 多数派应 commit
        RaftNode leader = cluster.findLeader();
        assertNotNull(leader);
        assertTrue(leader.getCommitIndex().get() >= 1);
    }

    /**
     * 测试 14: 多数派提交 + 全局应用
     */
    @Test
    @DisplayName("14. 多数派提交 → 全局 applied log")
    void testClusterCommitAndApply() throws Exception {
        cluster = new RaftCluster(Arrays.asList("n1", "n2", "n3"), 200, 80);
        cluster.start();
        TimeUnit.SECONDS.sleep(2);
        long i1 = cluster.submit("cmd-A");
        long i2 = cluster.submit("cmd-B");
        long i3 = cluster.submit("cmd-C");
        assertTrue(i1 > 0 && i2 > 0 && i3 > 0);
        TimeUnit.MILLISECONDS.sleep(800);
        assertTrue(cluster.getGlobalApplied().size() >= 3,
                "3 命令应都被应用, 实际 " + cluster.getGlobalApplied().size());
    }

    /**
     * 测试 15: 集群状态查询
     */
    @Test
    @DisplayName("15. clusterState() 包含所有节点")
    void testClusterState() throws Exception {
        cluster = new RaftCluster(Arrays.asList("n1", "n2", "n3"), 200, 80);
        cluster.start();
        TimeUnit.SECONDS.sleep(1);
        Map<String, Object> state = cluster.clusterState();
        assertEquals(3, state.get("totalNodes"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) state.get("nodes");
        assertEquals(3, nodes.size());
        for (Map<String, Object> n : nodes) {
            assertNotNull(n.get("state"));
            assertNotNull(n.get("term"));
        }
    }
}
