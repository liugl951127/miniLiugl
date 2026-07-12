package com.minimax.ai.distributed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分布式训练 (V3.3.3) 单元测试
 *
 * <p>覆盖:
 *   1. DataSharder 4 策略
 *   2. 分片大小正确
 *   3. ROUND_ROBIN 覆盖所有数据
 *   4. CONTIGUOUS 连续
 *   5. RANDOM 种子可复现
 *   6. HASH 同 key 同 shard
 *   7. AllReduce SUM
 *   8. AllReduce MEAN
 *   9. AllReduce MAX
 *   10. AllReduce 维度校验
 *   11. AllReduce norm
 *   12. 4 策略枚举
 *   13. 4 reduce op
 *   14. shardSizes 计算
 *   15. shardIdx 越界
 */
class DistributedTest {

    /**
     * 测试 1: 4 策略枚举
     */
    @Test
    @DisplayName("1. DataSharder 4 策略")
    void testStrategies() {
        assertEquals(4, DataSharder.Strategy.values().length);
        assertNotNull(DataSharder.Strategy.ROUND_ROBIN);
        assertNotNull(DataSharder.Strategy.CONTIGUOUS);
        assertNotNull(DataSharder.Strategy.RANDOM);
        assertNotNull(DataSharder.Strategy.HASH);
    }

    /**
     * 测试 2: 4 reduce op
     */
    @Test
    @DisplayName("2. AllReduce 4 操作")
    void testReduceOps() {
        assertEquals(4, AllReduce.ReduceOp.values().length);
        assertNotNull(AllReduce.ReduceOp.SUM);
        assertNotNull(AllReduce.ReduceOp.MEAN);
        assertNotNull(AllReduce.ReduceOp.MAX);
        assertNotNull(AllReduce.ReduceOp.MIN);
    }

    /**
     * 测试 3: ROUND_ROBIN 切分
     */
    @Test
    @DisplayName("3. ROUND_ROBIN 切分 (4 shards × 10)")
    void testRoundRobin() {
        DataSharder s = new DataSharder();
        int[] shard0 = s.shard(40, 4, 0, DataSharder.Strategy.ROUND_ROBIN);
        int[] shard1 = s.shard(40, 4, 1, DataSharder.Strategy.ROUND_ROBIN);
        int[] shard2 = s.shard(40, 4, 2, DataSharder.Strategy.ROUND_ROBIN);
        int[] shard3 = s.shard(40, 4, 3, DataSharder.Strategy.ROUND_ROBIN);
        assertEquals(10, shard0.length);
        assertEquals(10, shard1.length);
        assertEquals(10, shard2.length);
        assertEquals(10, shard3.length);
        // 验证: 第一个 shard 拿 0, 4, 8, 12, ...
        assertEquals(0, shard0[0]);
        assertEquals(4, shard0[1]);
        assertEquals(36, shard0[9]);
    }

    /**
     * 测试 4: CONTIGUOUS 连续
     */
    @Test
    @DisplayName("4. CONTIGUOUS 切分 (10→3,3,4)")
    void testContiguous() {
        DataSharder s = new DataSharder();
        int[] s0 = s.shard(10, 3, 0, DataSharder.Strategy.CONTIGUOUS);
        int[] s1 = s.shard(10, 3, 1, DataSharder.Strategy.CONTIGUOUS);
        int[] s2 = s.shard(10, 3, 2, DataSharder.Strategy.CONTIGUOUS);
        assertEquals(3, s0.length);
        assertEquals(3, s1.length);
        assertEquals(4, s2.length);
        // 验证
        assertEquals(0, s0[0]);
        assertEquals(2, s0[2]);
        assertEquals(3, s1[0]);
        assertEquals(6, s2[0]);
        assertEquals(9, s2[3]);
    }

    /**
     * 测试 5: RANDOM 种子可复现
     */
    @Test
    @DisplayName("5. RANDOM 种子可复现")
    void testRandomReproducible() {
        DataSharder s = new DataSharder();
        int[] r1 = s.shard(100, 4, 0, DataSharder.Strategy.RANDOM, 42L);
        int[] r2 = s.shard(100, 4, 0, DataSharder.Strategy.RANDOM, 42L);
        assertArrayEquals(r1, r2);
    }

    /**
     * 测试 6: HASH 同 key 同 shard
     */
    @Test
    @DisplayName("6. HASH 切分 (i mod N == shardIdx)")
    void testHash() {
        DataSharder s = new DataSharder();
        int[] shard0 = s.shard(20, 4, 0, DataSharder.Strategy.HASH);
        for (int i : shard0) {
            assertEquals(0, Math.floorMod(i, 4), "idx " + i + " 不属于 shard 0");
        }
    }

    /**
     * 测试 7: AllReduce SUM
     */
    @Test
    @DisplayName("7. AllReduce SUM")
    void testAllReduceSum() {
        AllReduce ar = new AllReduce();
        java.util.Map<String, double[]> grads = new java.util.HashMap<>();
        grads.put("w0", new double[]{1.0, 2.0, 3.0});
        grads.put("w1", new double[]{4.0, 5.0, 6.0});
        grads.put("w2", new double[]{7.0, 8.0, 9.0});
        double[] r = ar.allReduce(grads, AllReduce.ReduceOp.SUM);
        assertEquals(12.0, r[0], 0.0001);
        assertEquals(15.0, r[1], 0.0001);
        assertEquals(18.0, r[2], 0.0001);
    }

    /**
     * 测试 8: AllReduce MEAN
     */
    @Test
    @DisplayName("8. AllReduce MEAN (= SUM / N)")
    void testAllReduceMean() {
        AllReduce ar = new AllReduce();
        java.util.Map<String, double[]> grads = new java.util.HashMap<>();
        grads.put("w0", new double[]{1.0, 2.0});
        grads.put("w1", new double[]{3.0, 4.0});
        double[] r = ar.allReduce(grads, AllReduce.ReduceOp.MEAN);
        assertEquals(2.0, r[0], 0.0001);  // (1+3)/2
        assertEquals(3.0, r[1], 0.0001);  // (2+4)/2
    }

    /**
     * 测试 9: AllReduce MAX / MIN
     */
    @Test
    @DisplayName("9. AllReduce MAX / MIN")
    void testAllReduceMaxMin() {
        AllReduce ar = new AllReduce();
        java.util.Map<String, double[]> grads = new java.util.HashMap<>();
        grads.put("w0", new double[]{1.0, 5.0});
        grads.put("w1", new double[]{3.0, 2.0});
        grads.put("w2", new double[]{2.0, 4.0});
        double[] maxR = ar.allReduce(grads, AllReduce.ReduceOp.MAX);
        assertEquals(3.0, maxR[0], 0.0001);
        assertEquals(5.0, maxR[1], 0.0001);
        double[] minR = ar.allReduce(grads, AllReduce.ReduceOp.MIN);
        assertEquals(1.0, minR[0], 0.0001);
        assertEquals(2.0, minR[1], 0.0001);
    }

    /**
     * 测试 10: AllReduce 维度校验
     */
    @Test
    @DisplayName("10. AllReduce 维度校验抛异常")
    void testAllReduceDimMismatch() {
        AllReduce ar = new AllReduce();
        java.util.Map<String, double[]> grads = new java.util.HashMap<>();
        grads.put("w0", new double[]{1.0, 2.0});
        grads.put("w1", new double[]{1.0, 2.0, 3.0});
        assertThrows(IllegalArgumentException.class, () -> ar.allReduce(grads, AllReduce.ReduceOp.MEAN));
    }

    /**
     * 测试 11: AllReduce norm
     */
    @Test
    @DisplayName("11. AllReduce.norm L2")
    void testNorm() {
        AllReduce ar = new AllReduce();
        // [3, 4] → L2 = 5
        assertEquals(5.0, ar.norm(new double[]{3.0, 4.0}), 0.0001);
        assertEquals(0.0, ar.norm(new double[]{0, 0, 0}), 0.0001);
    }

    /**
     * 测试 12: shardSizes
     */
    @Test
    @DisplayName("12. shardSizes 各分片大小 (10→3+3+4)")
    void testShardSizes() {
        DataSharder s = new DataSharder();
        int[] sizes = s.shardSizes(10, 3);
        assertEquals(3, sizes[0]);
        assertEquals(3, sizes[1]);
        assertEquals(4, sizes[2]);
    }

    /**
     * 测试 13: shardIdx 越界
     */
    @Test
    @DisplayName("13. shardIdx 越界抛异常")
    void testShardIdxOutOfBounds() {
        DataSharder s = new DataSharder();
        assertThrows(IllegalArgumentException.class,
                () -> s.shard(10, 3, 5, DataSharder.Strategy.ROUND_ROBIN));
    }

    /**
     * 测试 14: Ring AllReduce
     */
    @Test
    @DisplayName("14. Ring AllReduce (中央汇总)")
    void testRingAllReduce() {
        AllReduce ar = new AllReduce();
        double[][] grads = {
                {1.0, 2.0},
                {3.0, 4.0}
        };
        double[] r = ar.ringAllReduce(grads, AllReduce.ReduceOp.SUM);
        assertEquals(4.0, r[0], 0.0001);
        assertEquals(6.0, r[1], 0.0001);
    }

    /**
     * 测试 15: shard 索引求和 == totalSize
     */
    @Test
    @DisplayName("15. 4 策略各分片大小之和 = totalSize")
    void testShardSumEqualsTotal() {
        DataSharder s = new DataSharder();
        int total = 1000;
        for (DataSharder.Strategy st : DataSharder.Strategy.values()) {
            int sum = 0;
            for (int i = 0; i < 4; i++) {
                sum += s.shard(total, 4, i, st).length;
            }
            assertEquals(total, sum, "策略 " + st + " 总和不等于 " + total);
        }
    }
}
