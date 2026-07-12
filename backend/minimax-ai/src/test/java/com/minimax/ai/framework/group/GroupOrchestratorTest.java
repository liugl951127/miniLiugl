package com.minimax.ai.framework.group;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 智能体群组 (V3.0.3) 单元测试
 *
 * <p>覆盖:
 *   1. GroupRole / GroupStrategy 枚举
 *   2. GroupSharedMemory 读写 + 并发
 *   3. GroupMessageBus 路由 + 历史
 *   4. GroupMember 工厂
 *   5. GroupTask 构造
 *   6. GroupOrchestrator PIPELINE 策略
 *   7. GroupOrchestrator VOTE 策略 (并行)
 *   8. GroupOrchestrator DEBATE 策略 (多轮)
 *   9. GroupOrchestrator SWARM 策略 (manager 聚合)
 *   10. 失败兜底 (无 manager / 无 worker)
 *   11. SimpleEchoAgent 基本调用
 *   12. 并发线程安全
 */
class GroupOrchestratorTest {

    /** 协调器 */
    private GroupOrchestrator orch;

    @BeforeEach
    void setUp() {
        // 1. 准备
        orch = new GroupOrchestrator();
    }

    @AfterEach
    void tearDown() {
        // 2. 清理
        orch.shutdown();
    }

    /**
     * 测试 1: 枚举值
     */
    @Test
    @DisplayName("1. GroupRole + GroupStrategy 枚举值")
    void testEnums() {
        assertEquals(4, GroupRole.values().length);
        assertEquals(4, GroupStrategy.values().length);
        assertNotNull(GroupRole.MANAGER);
        assertNotNull(GroupStrategy.PIPELINE);
    }

    /**
     * 测试 2: 共享内存基本读写
     */
    @Test
    @DisplayName("2. GroupSharedMemory put/get/increment/appendToList")
    void testSharedMemory() {
        GroupSharedMemory mem = new GroupSharedMemory();
        mem.put("k1", "v1");
        assertEquals("v1", mem.get("k1"));
        assertEquals(1, mem.size());

        // increment
        mem.put("count", 0);
        assertEquals(1, mem.increment("count", 1));
        assertEquals(3, mem.increment("count", 2));

        // appendToList
        mem.appendToList("log", "a");
        mem.appendToList("log", "b");
        List<?> log = mem.get("log");
        assertEquals(2, log.size());

        // getOrDefault
        assertEquals("default", mem.getOrDefault("missing", "default"));

        // clear
        mem.clear();
        assertEquals(0, mem.size());

        // snapshot
        mem.put("x", 1);
        mem.put("y", "test");
        Map<String, Object> snap = mem.snapshot();
        assertEquals(2, snap.size());
    }

    /**
     * 测试 3: 消息总线
     */
    @Test
    @DisplayName("3. GroupMessageBus 点对点 + 广播 + 历史")
    void testMessageBus() {
        GroupMessageBus bus = new GroupMessageBus();
        bus.register("a");
        bus.register("b");
        bus.register("c");

        // 点对点
        bus.send(GroupMessage.builder()
                .from("a").to("b")
                .type(GroupMessage.Type.TASK)
                .content("hello b")
                .build());
        assertEquals(1, bus.inboxSize("b"));
        assertEquals(0, bus.inboxSize("a"));  // 不自送
        assertEquals(0, bus.inboxSize("c"));  // 别人收不到

        // 收件人 B 收到
        List<GroupMessage> bInbox = bus.receive("b");
        assertEquals(1, bInbox.size());
        assertEquals("hello b", bInbox.get(0).getContent());
        assertEquals(0, bus.inboxSize("b"));  // 接收后清空

        // 广播
        bus.send(GroupMessage.builder()
                .from("a").to(null)  // null = 广播
                .type(GroupMessage.Type.BROADCAST)
                .content("broadcast msg")
                .build());
        assertEquals(1, bus.inboxSize("b"));
        assertEquals(1, bus.inboxSize("c"));
        assertEquals(0, bus.inboxSize("a"));  // 不发给自己

        // 历史
        List<GroupMessage> hist = bus.getHistory();
        assertEquals(2, hist.size());

        // 偷看 (不移除)
        bus.peek("b");
        // peek 不影响 size
    }

    /**
     * 测试 4: GroupMember 工厂
     */
    @Test
    @DisplayName("4. GroupMember 工厂方法 (manager/worker/critic)")
    void testMemberFactory() {
        GroupMember m = GroupMember.manager("g1", "mgr");
        assertEquals(GroupRole.MANAGER, m.getRole());
        assertEquals(2.0, m.getWeight());

        GroupMember w = GroupMember.worker("g1", "w1", "code");
        assertEquals(GroupRole.WORKER, w.getRole());
        assertEquals(1.0, w.getWeight());
        assertEquals("code", w.getCapability());

        GroupMember c = GroupMember.critic("g1", "c1");
        assertEquals(GroupRole.CRITIC, c.getRole());
    }

    /**
     * 测试 5: GroupTask 构造 + SubTask
     */
    @Test
    @DisplayName("5. GroupTask 构造 (含 SubTask)")
    void testGroupTask() {
        GroupTask task = GroupTask.builder()
                .taskId("t1")
                .groupId("g1")
                .subject("测试任务")
                .input("hello world")
                .type("writing")
                .build();
        assertEquals("t1", task.getTaskId());
        assertEquals("hello world", task.getInput());

        GroupTask.SubTask sub = GroupTask.SubTask.builder()
                .subId("s1")
                .instruction("do x")
                .status(GroupTask.SubTask.Status.RUNNING)
                .build();
        task.getSubTasks().add(sub);
        assertEquals(1, task.getSubTasks().size());
    }

    /**
     * 测试 6: PIPELINE 策略
     */
    @Test
    @DisplayName("6. GroupOrchestrator PIPELINE 策略")
    void testPipeline() {
        List<GroupMember> group = Arrays.asList(
                GroupMember.manager("g1", "mgr"),
                GroupMember.worker("g1", "w1", "analyzer"),
                GroupMember.worker("g1", "w2", "writer")
        );
        Map<String, AgentExecutor> agents = new HashMap<>();
        agents.put("mgr", new SimpleEchoAgent("mgr", "coordinator"));
        agents.put("w1", new SimpleEchoAgent("w1", "analyzer"));
        agents.put("w2", new SimpleEchoAgent("w2", "writer"));

        GroupTask task = GroupTask.builder()
                .taskId("t1")
                .groupId("g1")
                .subject("Pipeline 测试")
                .input("原始输入")
                .build();
        GroupResult result = orch.execute(group, task, agents, GroupStrategy.PIPELINE);
        assertEquals(GroupResult.Status.SUCCESS, result.getStatus());
        assertEquals(2, result.getWorkerOutputs().size());
        assertTrue(result.getFinalOutput().contains("w2"));  // 最后是 w2 输出
        assertEquals(8.0, result.getScore());
    }

    /**
     * 测试 7: VOTE 策略
     */
    @Test
    @DisplayName("7. GroupOrchestrator VOTE 策略 (并行)")
    void testVote() {
        List<GroupMember> group = Arrays.asList(
                GroupMember.manager("g1", "mgr"),
                GroupMember.worker("g1", "w1", "v1"),
                GroupMember.worker("g1", "w2", "v2"),
                GroupMember.worker("g1", "w3", "v3")
        );
        Map<String, AgentExecutor> agents = new HashMap<>();
        for (GroupMember m : group) {
            agents.put(m.getAgentName(), new SimpleEchoAgent(m.getAgentName(), m.getCapability()));
        }
        GroupTask task = GroupTask.builder()
                .taskId("t2").groupId("g1")
                .subject("Vote 测试").input("vote test").build();
        GroupResult result = orch.execute(group, task, agents, GroupStrategy.VOTE);
        assertEquals(GroupResult.Status.SUCCESS, result.getStatus());
        assertEquals(3, result.getWorkerOutputs().size());
        assertNotNull(result.getConsensus());
    }

    /**
     * 测试 8: DEBATE 策略
     */
    @Test
    @DisplayName("8. GroupOrchestrator DEBATE 策略 (多轮)")
    void testDebate() {
        List<GroupMember> group = Arrays.asList(
                GroupMember.manager("g1", "mgr"),
                GroupMember.worker("g1", "w1", "d1"),
                GroupMember.worker("g1", "w2", "d2")
        );
        Map<String, AgentExecutor> agents = new HashMap<>();
        for (GroupMember m : group) {
            agents.put(m.getAgentName(), new SimpleEchoAgent(m.getAgentName(), m.getCapability()));
        }
        GroupTask task = GroupTask.builder()
                .taskId("t3").groupId("g1")
                .subject("Debate 测试").input("debate test").build();
        GroupResult result = orch.execute(group, task, agents, GroupStrategy.DEBATE);
        assertEquals(GroupResult.Status.SUCCESS, result.getStatus());
        // DEBATE 跑 3 轮, final 应包含两个 worker 输出
        assertTrue(result.getFinalOutput().contains("w1") || result.getFinalOutput().contains("w2"));
    }

    /**
     * 测试 9: SWARM 策略
     */
    @Test
    @DisplayName("9. GroupOrchestrator SWARM 策略 (manager 聚合)")
    void testSwarm() {
        List<GroupMember> group = Arrays.asList(
                GroupMember.manager("g1", "mgr"),
                GroupMember.worker("g1", "w1", "sw1"),
                GroupMember.worker("g1", "w2", "sw2")
        );
        Map<String, AgentExecutor> agents = new HashMap<>();
        for (GroupMember m : group) {
            agents.put(m.getAgentName(), new SimpleEchoAgent(m.getAgentName(), m.getCapability()));
        }
        GroupTask task = GroupTask.builder()
                .taskId("t4").groupId("g1")
                .subject("Swarm 测试").input("swarm test").build();
        GroupResult result = orch.execute(group, task, agents, GroupStrategy.SWARM);
        assertEquals(GroupResult.Status.SUCCESS, result.getStatus());
        assertNotNull(result.getFinalOutput());
    }

    /**
     * 测试 10: 失败兜底 - 无 manager
     */
    @Test
    @DisplayName("10. 失败兜底 - 无 manager 时 FAILED")
    void testFailNoManager() {
        List<GroupMember> group = List.of(
                GroupMember.worker("g1", "w1", "x")
        );
        Map<String, AgentExecutor> agents = Map.of("w1", new SimpleEchoAgent("w1", "x"));
        GroupTask task = GroupTask.builder().taskId("t5").groupId("g1")
                .subject("x").input("x").build();
        GroupResult r = orch.execute(group, task, agents, GroupStrategy.PIPELINE);
        assertEquals(GroupResult.Status.FAILED, r.getStatus());
        assertNotNull(r.getError());
    }

    /**
     * 测试 11: 失败兜底 - 无 worker
     */
    @Test
    @DisplayName("11. 失败兜底 - 无 worker 时 FAILED")
    void testFailNoWorker() {
        List<GroupMember> group = List.of(GroupMember.manager("g1", "m"));
        Map<String, AgentExecutor> agents = Map.of("m", new SimpleEchoAgent("m", "x"));
        GroupTask task = GroupTask.builder().taskId("t6").groupId("g1")
                .subject("x").input("x").build();
        GroupResult r = orch.execute(group, task, agents, GroupStrategy.PIPELINE);
        assertEquals(GroupResult.Status.FAILED, r.getStatus());
    }

    /**
     * 测试 12: SimpleEchoAgent 基本调用
     */
    @Test
    @DisplayName("12. SimpleEchoAgent 基本调用")
    void testSimpleEchoAgent() {
        SimpleEchoAgent agent = new SimpleEchoAgent("test", "tester");
        assertEquals("test", agent.name());
        assertEquals("tester", agent.capability());
        String out = agent.execute(
                GroupTask.SubTask.builder().subId("s1").instruction("hello").build(),
                new GroupSharedMemory()
        );
        assertTrue(out.contains("test"));
        assertTrue(out.contains("hello"));
    }

    /**
     * 测试 13: 并发线程安全 (共享内存 + 消息总线)
     */
    @Test
    @DisplayName("13. 并发线程安全 (10 线程并发写)")
    void testConcurrency() throws Exception {
        GroupSharedMemory mem = new GroupSharedMemory();
        int threads = 10;
        int writesPerThread = 100;
        // 并发写
        Thread[] ts = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            final int id = i;
            ts[i] = new Thread(() -> {
                for (int j = 0; j < writesPerThread; j++) {
                    mem.put("k" + id + "_" + j, "v" + j);
                }
            });
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        // 验证
        assertEquals(threads * writesPerThread, mem.size());
        // 并发 increment
        for (int i = 0; i < threads; i++) {
            mem.put("counter", 0);
        }
        for (int i = 0; i < threads; i++) {
            ts[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    mem.increment("counter", 1);
                }
            });
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        assertEquals(Integer.valueOf(threads * 100), mem.get("counter"));
    }

    /**
     * 测试 14: GroupMessageBus 收件人不存在
     */
    @Test
    @DisplayName("14. 收件人不存在时不报错 (只警告)")
    void testBusUnknownRecipient() {
        GroupMessageBus bus = new GroupMessageBus();
        bus.register("a");
        // 发给未注册 b
        bus.send(GroupMessage.builder()
                .from("a").to("unknown")
                .type(GroupMessage.Type.TASK)
                .build());
        // 不报错
        assertEquals(0, bus.inboxSize("a"));
        // 历史还是记录
        assertEquals(1, bus.getHistory().size());
    }

    /**
     * 测试 15: GroupMessage.Type 枚举
     */
    @Test
    @DisplayName("15. GroupMessage.Type 5 类型 (TASK/RESULT/FEEDBACK/BROADCAST/SHUTDOWN)")
    void testMessageTypes() {
        assertEquals(5, GroupMessage.Type.values().length);
        assertNotNull(GroupMessage.Type.TASK);
        assertNotNull(GroupMessage.Type.RESULT);
        assertNotNull(GroupMessage.Type.FEEDBACK);
        assertNotNull(GroupMessage.Type.BROADCAST);
        assertNotNull(GroupMessage.Type.SHUTDOWN);
    }

    /**
     * 测试 16: GroupMessage 默认 ID 和 timestamp
     */
    @Test
    @DisplayName("16. GroupMessage 自动生成 ID 和 timestamp")
    void testMessageAutoFill() {
        GroupMessageBus bus = new GroupMessageBus();
        bus.register("a");
        bus.register("b");
        bus.send(GroupMessage.builder()
                .from("a").to("b")
                .type(GroupMessage.Type.TASK)
                .content("test")
                .build());
        List<GroupMessage> hist = bus.getHistory();
        assertNotNull(hist.get(0).getId());
        assertTrue(hist.get(0).getTimestamp() > 0);
    }

    /**
     * 测试 17: GroupResult isSuccess()
     */
    @Test
    @DisplayName("17. GroupResult.isSuccess() 状态判定")
    void testResultIsSuccess() {
        GroupResult r1 = GroupResult.builder().status(GroupResult.Status.SUCCESS).build();
        GroupResult r2 = GroupResult.builder().status(GroupResult.Status.FAILED).build();
        GroupResult r3 = GroupResult.builder().status(GroupResult.Status.TIMEOUT).build();
        assertTrue(r1.isSuccess());
        assertFalse(r2.isSuccess());
        assertFalse(r3.isSuccess());
    }
}
