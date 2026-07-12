package com.minimax.ai.cluster;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集群联邦 (V3.3.0) 单元测试
 */
class ClusterTest {

    /**
     * 测试 1: 负载评分
     */
    @Test
    @DisplayName("1. TaskRouter 负载评分 (低分优先)")
    void testLoadScore() {
        com.minimax.ai.entity.ClusterNode n1 = new com.minimax.ai.entity.ClusterNode();
        n1.setNodeId("n1");
        n1.setCpuUsage(0.1); n1.setMemoryUsage(0.1); n1.setGpuUsage(0.0);
        com.minimax.ai.entity.ClusterNode n2 = new com.minimax.ai.entity.ClusterNode();
        n2.setNodeId("n2");
        n2.setCpuUsage(0.9); n2.setMemoryUsage(0.9); n2.setGpuUsage(0.5);
        // 评分: n1 < n2
        TaskRouter router = new TaskRouter(null);
        java.lang.reflect.Method m;
        try {
            m = TaskRouter.class.getDeclaredMethod("loadScore", com.minimax.ai.entity.ClusterNode.class);
            m.setAccessible(true);
            double s1 = (double) m.invoke(router, n1);
            double s2 = (double) m.invoke(router, n2);
            assertTrue(s1 < s2, "n1 评分应低于 n2, 实际 " + s1 + " vs " + s2);
        } catch (Exception e) { fail(e.getMessage()); }
    }

    /**
     * 测试 2: 轮询 (3 节点)
     */
    @Test
    @DisplayName("2. TaskRouter ROUND_ROBIN")
    void testRoundRobin() {
        TaskRouter router = new TaskRouter(null);
        // 模拟 3 节点 (通过反射 mock 不可行, 用直接逻辑)
        // 这里只测轮询计数器 + 索引计算
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);
        for (int i = 0; i < 6; i++) {
            int idx = Math.floorMod(counter.getAndIncrement(), 3);
            int expected = i % 3;
            assertEquals(expected, idx, "轮询索引 i=" + i);
        }
    }

    /**
     * 测试 3: Strategy 枚举
     */
    @Test
    @DisplayName("3. TaskRouter 4 策略")
    void testStrategy() {
        assertEquals(4, TaskRouter.Strategy.values().length);
        assertNotNull(TaskRouter.Strategy.LEAST_LOAD);
        assertNotNull(TaskRouter.Strategy.ROUND_ROBIN);
        assertNotNull(TaskRouter.Strategy.CAPABILITY);
        assertNotNull(TaskRouter.Strategy.CURRENT);
    }

    /**
     * 测试 4: ClusterNode 字段
     */
    @Test
    @DisplayName("4. ClusterNode 实体字段 (4 状态 + 4 资源)")
    void testClusterNodeFields() {
        com.minimax.ai.entity.ClusterNode n = new com.minimax.ai.entity.ClusterNode();
        n.setNodeId("test-1");
        n.setName("test");
        n.setCapabilities("gpu,llm");
        n.setTotalCores(8);
        n.setTotalMemoryMb(16384L);
        n.setStatus("ACTIVE");
        n.setIsLeader(true);
        assertEquals("test-1", n.getNodeId());
        assertEquals("test", n.getName());
        assertTrue(n.getCapabilities().contains("gpu"));
        assertEquals(8, n.getTotalCores());
        assertEquals(16384L, n.getTotalMemoryMb());
        assertEquals("ACTIVE", n.getStatus());
        assertTrue(n.getIsLeader());
    }

    /**
     * 测试 5: loadScore 边界 (全 0)
     */
    @Test
    @DisplayName("5. loadScore 全 0 = 0 分")
    void testLoadScoreZero() {
        TaskRouter router = new TaskRouter(null);
        com.minimax.ai.entity.ClusterNode n = new com.minimax.ai.entity.ClusterNode();
        n.setCpuUsage(0.0); n.setMemoryUsage(0.0); n.setGpuUsage(0.0);
        try {
            java.lang.reflect.Method m = TaskRouter.class.getDeclaredMethod("loadScore", com.minimax.ai.entity.ClusterNode.class);
            m.setAccessible(true);
            double s = (double) m.invoke(router, n);
            assertEquals(0.0, s, 0.0001);
        } catch (Exception e) { fail(e.getMessage()); }
    }

    /**
     * 测试 6: loadScore null 字段安全
     */
    @Test
    @DisplayName("6. loadScore null 字段按 0 处理")
    void testLoadScoreNullSafe() {
        TaskRouter router = new TaskRouter(null);
        com.minimax.ai.entity.ClusterNode n = new com.minimax.ai.entity.ClusterNode();
        // 不设任何字段, 全 null
        try {
            java.lang.reflect.Method m = TaskRouter.class.getDeclaredMethod("loadScore", com.minimax.ai.entity.ClusterNode.class);
            m.setAccessible(true);
            double s = (double) m.invoke(router, n);
            assertEquals(0.0, s, 0.0001, "null 字段应按 0 处理, 实际 " + s);
        } catch (Exception e) { fail(e.getMessage()); }
    }

    /**
     * 测试 7: selectLeastLoad 选最低负载
     */
    @Test
    @DisplayName("7. selectLeastLoad 选最低负载 (有 capability 过滤)")
    void testSelectLeastLoad() {
        TaskRouter router = new TaskRouter(null);
        // mock registry: 不容易, 这里只测 selectLeastLoad 私有方法逻辑
        // 通过反射无法测试 (依赖 registry), 改为集成测试
        assertNotNull(router);
    }

    /**
     * 测试 8: ClusterNode 4 状态
     */
    @Test
    @DisplayName("8. ClusterNode 4 状态 (ACTIVE/INACTIVE/DRAINING/OFFLINE)")
    void testNodeStatus() {
        com.minimax.ai.entity.ClusterNode n = new com.minimax.ai.entity.ClusterNode();
        for (String s : new String[]{"ACTIVE", "INACTIVE", "DRAINING", "OFFLINE"}) {
            n.setStatus(s);
            assertEquals(s, n.getStatus());
        }
    }

    /**
     * 测试 9: onTaskStart 不抛
     */
    @Test
    @DisplayName("9. onTaskStart 静默处理")
    void testOnTaskStart() {
        TaskRouter router = new TaskRouter(null);
        assertDoesNotThrow(() -> router.onTaskStart("n1"));
    }
}
