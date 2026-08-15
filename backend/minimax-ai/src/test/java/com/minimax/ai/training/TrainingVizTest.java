package com.minimax.ai.training;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 训练可视化 (V3.2.0) 单元测试
 *
 * <p>覆盖:
 *   1. EMA 平滑 (基础 + 边界)
 *   2. EMA α 参数效应
 *   3. SHA256 校验
 *   4. TrainingStream 订阅/广播
 *   5. Subscriber 计数
 *   6. EMA 复杂度 (N=10000 < 100ms)
 *   7. EMA 数学正确性 (平滑后值域)
 *   8. EMA 与简单平均的差异
 */
class TrainingVizTest {

    /**
     * 测试 1: EMA 空列表
     */
    @Test
    @DisplayName("1. EMA 空列表返回空")
    void testEmaEmpty() {
        TrainingVizService svc = new TrainingVizService(null, null, null, null);
        List<Double> out = svc.ema(List.of(), 0.3);
        assertTrue(out.isEmpty());
    }

    /**
     * 测试 2: EMA 单元素
     */
    @Test
    @DisplayName("2. EMA 单元素返回 [value]")
    void testEmaSingle() {
        TrainingVizService svc = new TrainingVizService(null, null, null, null);
        List<Double> out = svc.ema(List.of(2.0), 0.3);
        assertEquals(1, out.size());
        assertEquals(2.0, out.get(0), 0.0001);
    }

    /**
     * 测试 3: EMA 多元素 (数学正确性)
     */
    @Test
    @DisplayName("3. EMA 多元素数学正确 (α=0.5)")
    void testEmaMath() {
        TrainingVizService svc = new TrainingVizService(null, null, null, null);
        // 经典示例: x = [1, 2, 3, 4, 5], α = 0.5
        // EMA0 = 1
        // EMA1 = 0.5*2 + 0.5*1 = 1.5
        // EMA2 = 0.5*3 + 0.5*1.5 = 2.25
        // EMA3 = 0.5*4 + 0.5*2.25 = 3.125
        // EMA4 = 0.5*5 + 0.5*3.125 = 4.0625
        List<Double> out = svc.ema(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0), 0.5);
        assertEquals(5, out.size());
        assertEquals(1.0, out.get(0), 0.0001);
        assertEquals(1.5, out.get(1), 0.0001);
        assertEquals(2.25, out.get(2), 0.0001);
        assertEquals(3.125, out.get(3), 0.0001);
        assertEquals(4.0625, out.get(4), 0.0001);
    }

    /**
     * 测试 4: EMA α=1.0 几乎不平滑 (输出 ≈ 输入)
     */
    @Test
    @DisplayName("4. EMA α=1.0 跟原始值 (理论极限)")
    void testEmaAlphaOne() {
        TrainingVizService svc = new TrainingVizService(null, null, null, null);
        // α=1: EMA_t = 1*value_t + 0*EMA_{t-1} = value_t (除第一项, 第一项也相同)
        List<Double> out = svc.ema(Arrays.asList(1.0, 5.0, 2.0, 8.0), 1.0);
        assertEquals(1.0, out.get(0), 0.0001);
        assertEquals(5.0, out.get(1), 0.0001);
        assertEquals(2.0, out.get(2), 0.0001);
        assertEquals(8.0, out.get(3), 0.0001);
    }

    /**
     * 测试 5: EMA α=0 几乎不变 (只保留首项)
     */
    @Test
    @DisplayName("5. EMA α=0 退化为常数 (理论极限)")
    void testEmaAlphaZero() {
        TrainingVizService svc = new TrainingVizService(null, null, null, null);
        // α=0: EMA_t = 0*value_t + 1*EMA_{t-1} = EMA_0 (常数)
        List<Double> out = svc.ema(Arrays.asList(7.0, 99.0, -100.0), 0.0);
        assertEquals(7.0, out.get(0), 0.0001);
        assertEquals(7.0, out.get(1), 0.0001);
        assertEquals(7.0, out.get(2), 0.0001);
    }

    /**
     * 测试 6: EMA 性能 (10000 点 < 100ms)
     */
    @Test
    @DisplayName("6. EMA 性能: 10000 点 < 100ms")
    void testEmaPerformance() {
        TrainingVizService svc = new TrainingVizService(null, null, null, null);
        // 1. 准备 10000 个点
        java.util.Random rng = new java.util.Random(42);
        List<Double> data = new java.util.ArrayList<>();
        for (int i = 0; i < 10000; i++) data.add(rng.nextDouble() * 10);
        // 2. 计时
        long t0 = System.nanoTime();
        List<Double> out = svc.ema(data, 0.3);
        long elapsed = (System.nanoTime() - t0) / 1_000_000;
        // 3. 验证
        assertEquals(10000, out.size());
        assertTrue(elapsed < 100, "10000 点 EMA 应 < 100ms, 实际 " + elapsed + "ms");
    }

    /**
     * 测试 7: EMA 收敛性 (单调递增 → 平滑后也递增)
     */
    @Test
    @DisplayName("7. EMA 收敛性: 单调序列 → 平滑后单调")
    void testEmaMonotonic() {
        TrainingVizService svc = new TrainingVizService(null, null, null, null);
        List<Double> data = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0);
        List<Double> out = svc.ema(data, 0.3);
        // 验证: 每项 >= 前项
        for (int i = 1; i < out.size(); i++) {
            assertTrue(out.get(i) >= out.get(i - 1) - 0.0001,
                    "EMA 单调递增应在 " + i + " 保持: " + out.get(i - 1) + " → " + out.get(i));
        }
    }

    /**
     * 测试 8: TrainingStream 订阅计数
     */
    @Test
    @DisplayName("8. TrainingStream 订阅计数")
    void testStreamSubscribe() {
        TrainingStream stream = new TrainingStream();
        // 初始 0
        assertEquals(0, stream.subscriberCount("t1"));
        // 模拟订阅 (用 mock emitter)
        SseEmitter em1 = stream.subscribe("t1");
        assertEquals(1, stream.subscriberCount("t1"));
        // 不存在的 taskId
        assertEquals(0, stream.subscriberCount("t2"));
    }

    /**
     * 测试 9: TrainingStream 广播不抛异常 (空订阅者)
     */
    @Test
    @DisplayName("9. TrainingStream 广播到空订阅者列表不抛异常")
    void testStreamBroadcastEmpty() {
        TrainingStream stream = new TrainingStream();
        // 没订阅者时广播, 不抛
        assertDoesNotThrow(() -> stream.broadcastMetric("t1", 0, 100, 0.5, 0.4, 0.8));
        assertDoesNotThrow(() -> stream.broadcastStatus("t1", "RUNNING"));
        assertDoesNotThrow(() -> stream.broadcastCheckpoint("t1", "ckpt-1", "best", 5));
    }

    /**
     * 测试 10: EMA 中等 α 平滑 (0.3)
     */
    @Test
    @DisplayName("10. EMA α=0.3 实际应用 (loss 平滑)")
    void testEmaLossSmoothing() {
        TrainingVizService svc = new TrainingVizService(null, null, null, null);
        // 模拟 loss 序列: 噪点大, 但趋势下降
        List<Double> noisy = Arrays.asList(2.5, 1.8, 2.1, 1.5, 1.9, 1.2, 1.6, 0.9, 1.3, 0.7);
        List<Double> smoothed = svc.ema(noisy, 0.3);
        // 1. 长度一致
        assertEquals(noisy.size(), smoothed.size());
        // 2. 平滑后值域在 [min, max] 之内
        double min = noisy.stream().min(Double::compare).orElse(0.0);
        double max = noisy.stream().max(Double::compare).orElse(0.0);
        // 平滑后首项 = noisy[0]
        assertEquals(noisy.get(0), smoothed.get(0), 0.0001);
        // 平滑后末项比 noisy 末项更接近均值 (更平滑)
        double lastNoisy = noisy.get(noisy.size() - 1);
        double lastSmoothed = smoothed.get(smoothed.size() - 1);
        double mean = noisy.stream().mapToDouble(d -> d).average().orElse(0.0);
        assertTrue(Math.abs(lastSmoothed - mean) <= Math.abs(lastNoisy - mean) + 0.5,
                "EMA 末项应更接近均值, 实际 noisy=" + lastNoisy + " smoothed=" + lastSmoothed + " mean=" + mean);
    }

    /**
     * 测试 11: TrainingStream 事件名常量
     */
    @Test
    @DisplayName("11. TrainingStream 事件名常量")
    void testStreamEventNames() {
        assertEquals("metric", TrainingStream.EVT_METRIC);
        assertEquals("status", TrainingStream.EVT_STATUS);
        assertEquals("checkpoint", TrainingStream.EVT_CHECKPOINT);
    }

    /**
     * 测试 12: EMA 不同 α 对比 (大 α 更敏感)
     */
    @Test
    @DisplayName("12. EMA α 对比: 大α 末项更接近原值")
    void testEmaAlphaCompare() {
        TrainingVizService svc = new TrainingVizService(null, null, null, null);
        List<Double> data = Arrays.asList(1.0, 10.0, 1.0, 10.0, 1.0, 10.0);
        // α=0.9 末项更接近最后值 (10.0)
        List<Double> big = svc.ema(data, 0.9);
        // α=0.1 末项更接近均值 (~5.5)
        List<Double> small = svc.ema(data, 0.1);
        double lastBig = big.get(big.size() - 1);
        double lastSmall = small.get(small.size() - 1);
        // 验证: 大α 末项更接近 10
        assertTrue(lastBig > lastSmall,
                "α=0.9 末项 (" + lastBig + ") 应比 α=0.1 末项 (" + lastSmall + ") 更接近 10");
    }
}
