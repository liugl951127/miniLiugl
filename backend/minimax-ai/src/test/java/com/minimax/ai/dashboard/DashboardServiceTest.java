package com.minimax.ai.dashboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据看板 (V3.2.1) 单元测试
 *
 * <p>覆盖:
 *   1. 单个指标计算
 *   2. 全量指标聚合
 *   3. 缓存命中 (5s TTL)
 *   4. 缓存失效 (setter 调用后)
 *   5. 工具使用累加 + Top N
 *   6. 缓存命中率计算
 *   7. 错误率计算
 *   8. 未知指标返回 0
 *   9. 健康检查
 *   10. 缓存清理
 */
class DashboardServiceTest {

    /**
     * 测试 1: 单个指标
     */
    @Test
    @DisplayName("1. 单个指标 getMetric")
    void testGetMetric() {
        DashboardService svc = new DashboardService(null);
        svc.setUserTotal(100);
        svc.setApiRequests(50);
        assertEquals(100.0, svc.getMetric("user.total"));
        assertEquals(50.0, svc.getMetric("api.requests"));
    }

    /**
     * 测试 2: 未知指标返回 0
     */
    @Test
    @DisplayName("2. 未知指标返回 0")
    void testUnknownMetric() {
        DashboardService svc = new DashboardService(null);
        assertEquals(0.0, svc.getMetric("nonexistent"));
    }

    /**
     * 测试 3: 缓存命中 (5s TTL)
     */
    @Test
    @DisplayName("3. 缓存 5s TTL 命中")
    void testCacheTTL() {
        DashboardService svc = new DashboardService(null);
        svc.setUserTotal(100);
        // 1. 第一次
        double v1 = svc.getMetric("user.total");
        assertEquals(100.0, v1);
        // 2. 改值 (setter 失效缓存)
        svc.setUserTotal(200);
        // 3. 缓存失效后, 立即返回 200
        assertEquals(200.0, svc.getMetric("user.total"));
    }

    /**
     * 测试 4: 工具使用累加
     */
    @Test
    @DisplayName("4. 工具使用累加 + Top N")
    void testToolUsage() {
        DashboardService svc = new DashboardService(null);
        svc.incrementToolUsage("ppt.gen");
        svc.incrementToolUsage("ppt.gen");
        svc.incrementToolUsage("nl2sql");
        svc.incrementToolUsage("nl2chart");
        svc.incrementToolUsage("nl2chart");
        svc.incrementToolUsage("nl2chart");
        // 1. 累加值: 总调用 = 2+1+3 = 6
        assertEquals(6.0, svc.getMetric("ai.call.count"));
        // 2. Top 3
        List<Map<String, Object>> top = svc.getToolUsageTop(3);
        assertEquals(3, top.size());
        // 3. 第一个是 nl2chart (3 次)
        assertEquals("nl2chart", top.get(0).get("tool"));
        assertEquals(3L, top.get(0).get("count"));
    }

    /**
     * 测试 5: 缓存命中率
     */
    @Test
    @DisplayName("5. 缓存命中率计算")
    void testCacheHitRate() {
        DashboardService svc = new DashboardService(null);
        svc.setCacheHit(80);
        svc.setCacheMiss(20);
        // 80 / (80 + 20) = 0.8
        assertEquals(0.8, svc.getMetric("cache.hit_rate"), 0.0001);
        // 全 0
        svc.setCacheHit(0);
        svc.setCacheMiss(0);
        assertEquals(0.0, svc.getMetric("cache.hit_rate"));
    }

    /**
     * 测试 6: 错误率
     */
    @Test
    @DisplayName("6. API 错误率计算")
    void testErrorRate() {
        DashboardService svc = new DashboardService(null);
        svc.setApiRequests(100);
        svc.setApiErrors(5);
        Map<String, Object> all = svc.getAll();
        // 5 / 100 = 0.05
        assertEquals(0.05, all.get("errorRate"));
        // 无请求时 0
        svc.setApiRequests(0);
        svc.setApiErrors(0);
        all = svc.getAll();
        assertEquals(0.0, all.get("errorRate"));
    }

    /**
     * 测试 7: 全量指标
     */
    @Test
    @DisplayName("7. 全量指标聚合")
    void testGetAll() {
        DashboardService svc = new DashboardService(null);
        svc.setUserTotal(1000);
        svc.setUserActive(50);
        svc.setApiRequests(5000);
        svc.setApiErrors(10);
        svc.setCacheHit(4500);
        svc.setCacheMiss(500);
        svc.setTrainingRunning(3);
        svc.setTrainingCompleted(12);
        svc.incrementToolUsage("ppt.gen");
        Map<String, Object> all = svc.getAll();
        assertEquals(Double.valueOf(1000), all.get("userTotal"));
        assertEquals(Double.valueOf(50), all.get("userActive"));
        assertEquals(Double.valueOf(5000), all.get("apiRequests"));
        assertEquals(Double.valueOf(10), all.get("apiErrors"));
        assertEquals(Double.valueOf(3), all.get("trainingRunning"));
        assertEquals(Double.valueOf(12), all.get("trainingCompleted"));
        assertNotNull(all.get("toolUsage"));
        assertNotNull(all.get("timestamp"));
    }

    /**
     * 测试 8: 健康检查
     */
    @Test
    @DisplayName("8. 健康检查")
    void testHealth() {
        DashboardService svc = new DashboardService(null);
        Map<String, Object> h = svc.health();
        assertEquals("UP", h.get("status"));
        assertNotNull(h.get("cacheSize"));
        assertNotNull(h.get("toolCount"));
    }

    /**
     * 测试 9: 清缓存
     */
    @Test
    @DisplayName("9. clearCache 后缓存清空")
    void testClearCache() {
        DashboardService svc = new DashboardService(null);
        svc.setUserTotal(100);
        svc.getMetric("user.total");  // 触发缓存
        svc.setUserTotal(200);
        svc.clearCache();
        // 缓存已清, 立即返回新值
        assertEquals(200.0, svc.getMetric("user.total"));
    }

    /**
     * 测试 10: invalidateCache (setter 副作用)
     */
    @Test
    @DisplayName("10. setter 触发 invalidateCache")
    void testInvalidateOnSet() {
        DashboardService svc = new DashboardService(null);
        svc.setUserTotal(100);
        assertEquals(100.0, svc.getMetric("user.total"));
        // setter 后缓存失效
        svc.setUserTotal(200);
        assertEquals(200.0, svc.getMetric("user.total"));
    }

    /**
     * 测试 11: 缓存统计
     */
    @Test
    @DisplayName("11. 缓存统计 (size + hit/miss)")
    void testCacheStats() {
        DashboardService svc = new DashboardService(null);
        svc.setUserTotal(100);
        svc.getMetric("user.total");
        svc.getMetric("api.requests");
        Map<String, Object> stats = svc.getCacheStats();
        assertNotNull(stats.get("hit"));
        assertNotNull(stats.get("miss"));
        assertNotNull(stats.get("total"));
        assertNotNull(stats.get("hitRate"));
        assertNotNull(stats.get("cacheSize"));
    }

    /**
     * 测试 12: 训练运行/完成指标
     */
    @Test
    @DisplayName("12. 训练运行/完成指标")
    void testTrainingMetrics() {
        DashboardService svc = new DashboardService(null);
        svc.setTrainingRunning(5);
        svc.setTrainingCompleted(20);
        assertEquals(5.0, svc.getMetric("training.running"));
        assertEquals(20.0, svc.getMetric("training.completed"));
    }
}
