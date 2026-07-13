package com.minimax.ai.distributed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 数据分片器 (V3.5.5+ 完整注释版)
 *
 * <h2>背景: 为什么需要数据分片?</h2>
 * 分布式训练中, 数据量太大单节点跑不完.
 * 数据并行 (Data Parallel) 把数据集切成 N 份, 每 worker 跑一份, 最后用 AllReduce 同步梯度.
 * 这个"切"的操作就是数据分片.
 *
 * <h2>4 种分片策略</h2>
 * <ul>
 *   <li>{@link Strategy#ROUND_ROBIN} 轮询分配: 均匀, 适合独立样本 (iid 数据)</li>
 *   <li>{@link Strategy#CONTIGUOUS} 连续块: 按顺序切, 适合时序数据 (避免破坏顺序)</li>
 *   <li>{@link Strategy#RANDOM}     随机分配: 避免数据顺序偏置, 适合 SGD (常用)</li>
 *   <li>{@link Strategy#HASH}       按 key 哈希: 保证同 key 同 shard, 适合 KV 数据</li>
 * </ul>
 *
 * <h2>复杂度</h2>
 * O(N) 其中 N=数据条数. RANDOM 策略因洗牌多 O(N).
 *
 * <h2>示例 (totalSize=10, numShards=3)</h2>
 * <pre>
 *   ROUND_ROBIN:
 *     shard 0: [0, 3, 6, 9]
 *     shard 1: [1, 4, 7]
 *     shard 2: [2, 5, 8]
 *
 *   CONTIGUOUS:
 *     shard 0: [0, 1, 2, 3]
 *     shard 1: [4, 5, 6]
 *     shard 2: [7, 8, 9]
 *
 *   HASH (idx % 3 == shardIdx):
 *     shard 0: [0, 3, 6, 9]
 *     shard 1: [1, 4, 7]
 *     shard 2: [2, 5, 8]
 * </pre>
 *
 * @author MiniMax
 * @since V3.3.3
 */
@Slf4j
@Component
public class DataSharder {

    /**
     * 分片策略枚举
     */
    public enum Strategy {
        /** 轮询分配: 均匀, 适合独立样本 */
        ROUND_ROBIN,
        /** 连续块: 按顺序切, 适合时序数据 */
        CONTIGUOUS,
        /** 随机分配: 同 seed 可复现, 适合 SGD */
        RANDOM,
        /** 哈希分配: 同 key 同 shard, 适合 KV */
        HASH
    }

    /** RANDOM 策略的默认种子 (保证训练可复现) */
    private static final long DEFAULT_RANDOM_SEED = 42L;

    // ============== 公共 API ==============

    /**
     * 数据分片 (主入口)
     *
     * <p>根据策略返回当前 shard 应包含的数据索引数组
     *
     * @param totalSize 总数据条数, 必须 ≥ 0
     * @param numShards 分片数, e.g. GPU 数 × 每 GPU replica 数
     *                  必须 > 0, 否则返空数组
     * @param shardIdx  当前分片索引, 0-based, 范围 [0, numShards)
     * @param strategy  分片策略
     * @return 当前 shard 应包含的索引数组 (升序)
     * @throws IllegalArgumentException shardIdx 越界时
     */
    public int[] shard(int totalSize, int numShards, int shardIdx, Strategy strategy) {
        // 1. 入参校验
        // 1.1 numShards 必须 > 0
        if (numShards <= 0) {
            log.warn("numShards={} <= 0, 返空数组", numShards);
            return new int[0];
        }
        // 1.2 shardIdx 必须在 [0, numShards)
        if (shardIdx < 0 || shardIdx >= numShards) {
            throw new IllegalArgumentException(
                    "shardIdx 越界: " + shardIdx + " (有效范围 0.." + (numShards - 1) + ")"
            );
        }

        // 2. switch 路由到对应策略
        return switch (strategy) {
            case ROUND_ROBIN -> shardRoundRobin(totalSize, numShards, shardIdx);
            case CONTIGUOUS  -> shardContiguous(totalSize, numShards, shardIdx);
            case RANDOM      -> shardRandom(totalSize, numShards, shardIdx, DEFAULT_RANDOM_SEED);
            case HASH        -> shardHash(totalSize, numShards, shardIdx);
        };
    }

    /**
     * 数据分片 (RANDOM 策略可指定 seed, 方便实验复现)
     *
     * <p>非 RANDOM 策略忽略 seed 参数
     *
     * @param totalSize 总数据条数
     * @param numShards 分片数
     * @param shardIdx  当前分片索引
     * @param strategy  策略
     * @param seed      RANDOM 用的随机种子
     * @return 索引数组
     */
    public int[] shard(int totalSize, int numShards, int shardIdx, Strategy strategy, long seed) {
        // RANDOM 才用 seed, 其他策略走默认入口
        if (strategy == Strategy.RANDOM) {
            return shardRandom(totalSize, numShards, shardIdx, seed);
        }
        return shard(totalSize, numShards, shardIdx, strategy);
    }

    // ============== 私有策略实现 ==============

    /**
     * 轮询分配 (ROUND_ROBIN)
     *
     * <p>公式: shard k 拿所有满足 (i % numShards == k) 的 i
     *
     * <p>特点: 均匀, 各 shard 大小差 ≤ 1
     * 适合: 独立同分布 (iid) 数据, 如图像分类
     *
     * @param totalSize 总数据条数
     * @param numShards 分片数
     * @param shardIdx  当前分片
     * @return shardIdx 包含的索引 (升序)
     */
    private int[] shardRoundRobin(int totalSize, int numShards, int shardIdx) {
        // 预分配: 每 shard 约 totalSize/numShards 个
        List<Integer> indices = new ArrayList<>(totalSize / numShards + 1);

        // 从 shardIdx 开始, 每次 +numShards, 直到 totalSize
        // 例: numShards=3, shardIdx=1 → 1, 4, 7, 10, ...
        for (int i = shardIdx; i < totalSize; i += numShards) {
            indices.add(i);
        }

        // List<Integer> → int[] (避免 boxing 开销)
        return indices.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * 连续块 (CONTIGUOUS)
     *
     * <p>公式: shard k 拿 [k*totalSize/numShards, (k+1)*totalSize/numShards)
     *
     * <p>特点: 保持数据原始顺序, 适合时序数据 (LSTM/RNN 不能打乱时间)
     *
     * <p>注意: 用 long 计算避免 totalSize * shardIdx 溢出
     *
     * @param totalSize 总数据条数
     * @param numShards 分片数
     * @param shardIdx  当前分片
     * @return shardIdx 包含的索引 (升序, 连续区间)
     */
    private int[] shardContiguous(int totalSize, int numShards, int shardIdx) {
        // 1. 算 start 和 end (用 long 防止溢出)
        int start = (int) ((long) shardIdx * totalSize / numShards);
        int end   = (int) ((long) (shardIdx + 1) * totalSize / numShards);

        // 2. 填充 [start, end) 区间
        int size = end - start;
        int[] indices = new int[size];
        for (int i = 0; i < size; i++) {
            indices[i] = start + i;
        }
        return indices;
    }

    /**
     * 随机分配 (RANDOM)
     *
     * <p>算法:
     * <ol>
     *   <li>生成 0..totalSize-1 索引数组</li>
     *   <li>Fisher-Yates 洗牌 (同 seed 保证可复现)</li>
     *   <li>按 CONTIGUOUS 方式切分</li>
     * </ol>
     *
     * <p>为什么用同 seed: 实验可复现 (论文必备), debug 也能复现 bug
     *
     * <p>复杂度: O(totalSize) 洗牌 + O(size) 切片
     *
     * @param totalSize 总数据条数
     * @param numShards 分片数
     * @param shardIdx  当前分片
     * @param seed      随机种子
     * @return shardIdx 包含的索引
     */
    private int[] shardRandom(int totalSize, int numShards, int shardIdx, long seed) {
        // 1. 生成 0..totalSize-1 索引数组
        int[] all = new int[totalSize];
        for (int i = 0; i < totalSize; i++) {
            all[i] = i;
        }

        // 2. Fisher-Yates 洗牌 (从后向前, 每次随机选 [0, i+1) 交换)
        //    同 seed 保证不同运行结果一致
        Random rng = new Random(seed);
        for (int i = totalSize - 1; i > 0; i--) {
            // j = [0, i] 范围内随机整数
            int j = rng.nextInt(i + 1);
            // 交换 all[i] 和 all[j]
            int tmp = all[i];
            all[i] = all[j];
            all[j] = tmp;
        }

        // 3. 按 CONTIGUOUS 方式切: shard k 拿 [k*totalSize/numShards, (k+1)*totalSize/numShards)
        int start = (int) ((long) shardIdx * totalSize / numShards);
        int end   = (int) ((long) (shardIdx + 1) * totalSize / numShards);

        // copyOfRange 是左闭右开 [start, end)
        return Arrays.copyOfRange(all, start, end);
    }

    /**
     * 哈希分配 (HASH)
     *
     * <p>公式: idx 属于 shard (idx % numShards)
     *
     * <p>特点: 确定性, 同 idx 永远在同一 shard
     * 适合: KV 数据 (key 决定存储位置)
     *
     * <p>注意: 用 Math.floorMod 而不是 %, 避免负数问题
     *
     * @param totalSize 总数据条数
     * @param numShards 分片数
     * @param shardIdx  当前分片
     * @return shardIdx 包含的索引
     */
    private int[] shardHash(int totalSize, int numShards, int shardIdx) {
        // 收集所有 idx % numShards == shardIdx 的索引
        List<Integer> indices = new ArrayList<>(totalSize / numShards + 1);
        for (int i = 0; i < totalSize; i++) {
            // floorMod: 永远返非负 (Java 8+)
            if (Math.floorMod(i, numShards) == shardIdx) {
                indices.add(i);
            }
        }
        return indices.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * 计算每个分片的大小 (用于预分配 / 监控)
     *
     * <p>公式: shardSizes[k] = ((k+1) * totalSize / numShards) - (k * totalSize / numShards)
     * 整数除法自动向下取整, 末尾 shard 会多分 1 条 (让总数精确等于 totalSize)
     *
     * @param totalSize 总数据条数
     * @param numShards 分片数
     * @return 各 shard 大小数组
     */
    public int[] shardSizes(int totalSize, int numShards) {
        // 入参防御
        if (numShards <= 0) {
            return new int[0];
        }

        int[] sizes = new int[numShards];
        for (int i = 0; i < numShards; i++) {
            // 算 shard i 的 end - start
            long end = (long) (i + 1) * totalSize / numShards;
            long start = (long) i * totalSize / numShards;
            sizes[i] = (int) (end - start);
        }
        return sizes;
    }
}
