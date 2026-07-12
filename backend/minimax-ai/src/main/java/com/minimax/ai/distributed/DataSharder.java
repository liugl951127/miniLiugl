package com.minimax.ai.distributed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 数据分片器 (V3.3.3 数据并行)
 *
 * <p>把大数据集切成多份, 每节点/每 GPU 跑一份, 提升吞吐
 *
 * <h3>分片策略</h3>
 * <ul>
 *   <li>ROUND_ROBIN - 轮询 (均匀, 适合独立样本)</li>
 *   <li>CONTIGUOUS  - 连续块 (按顺序切, 适合时序数据)</li>
 *   <li>RANDOM       - 随机 (避免数据顺序偏置, 适合 SGD)</li>
 *   <li>HASH        - 按 key 哈希 (保证同 key 同 shard, 适合 KV 数据)</li>
 * </ul>
 *
 * <h3>复杂度</h3>
 *   O(N)  N=数据条数
 */
@Slf4j
@Component
public class DataSharder {

    public enum Strategy { ROUND_ROBIN, CONTIGUOUS, RANDOM, HASH }

    /**
     * 切分数据 (列表按索引分配)
     *
     * @param totalSize   总数据条数
     * @param numShards   分片数 (e.g. = GPU 数 × replica 数)
     * @param shardIdx    当前分片索引 (0-based)
     * @param strategy    切分策略
     * @return 当前分片包含的索引 (升序)
     */
    public int[] shard(int totalSize, int numShards, int shardIdx, Strategy strategy) {
        if (numShards <= 0) return new int[0];
        if (shardIdx < 0 || shardIdx >= numShards) {
            throw new IllegalArgumentException("shardIdx 越界: " + shardIdx + "/" + numShards);
        }
        return switch (strategy) {
            case ROUND_ROBIN -> shardRoundRobin(totalSize, numShards, shardIdx);
            case CONTIGUOUS -> shardContiguous(totalSize, numShards, shardIdx);
            case RANDOM -> shardRandom(totalSize, numShards, shardIdx, 42L);
            case HASH -> shardHash(totalSize, numShards, shardIdx);
        };
    }

    /**
     * 切分数据 (带种子随机)
     */
    public int[] shard(int totalSize, int numShards, int shardIdx, Strategy strategy, long seed) {
        if (strategy == Strategy.RANDOM) {
            return shardRandom(totalSize, numShards, shardIdx, seed);
        }
        return shard(totalSize, numShards, shardIdx, strategy);
    }

    /**
     * 轮询: 第 i 条 → shard (i % numShards)
     */
    private int[] shardRoundRobin(int totalSize, int numShards, int shardIdx) {
        List<Integer> idx = new ArrayList<>();
        for (int i = shardIdx; i < totalSize; i += numShards) {
            idx.add(i);
        }
        return idx.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * 连续: shard k 拿 [k*size/N, (k+1)*size/N)
     */
    private int[] shardContiguous(int totalSize, int numShards, int shardIdx) {
        int start = (int) ((long) shardIdx * totalSize / numShards);
        int end = (int) ((long) (shardIdx + 1) * totalSize / numShards);
        int[] idx = new int[end - start];
        for (int i = 0; i < idx.length; i++) idx[i] = start + i;
        return idx;
    }

    /**
     * 随机: 同 seed 保证可复现
     *
     * <p>把所有索引打乱后, 取 shardIdx 那段
     */
    private int[] shardRandom(int totalSize, int numShards, int shardIdx, long seed) {
        // 1. 生成 0..N-1 索引
        int[] all = new int[totalSize];
        for (int i = 0; i < totalSize; i++) all[i] = i;
        // 2. Fisher-Yates 洗牌 (用 seed)
        Random rng = new Random(seed);
        for (int i = totalSize - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = all[i]; all[i] = all[j]; all[j] = tmp;
        }
        // 3. 连续切
        int start = (int) ((long) shardIdx * totalSize / numShards);
        int end = (int) ((long) (shardIdx + 1) * totalSize / numShards);
        return Arrays.copyOfRange(all, start, end);
    }

    /**
     * 哈希: idx.hashCode() % numShards == shardIdx
     */
    private int[] shardHash(int totalSize, int numShards, int shardIdx) {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < totalSize; i++) {
            if (Math.floorMod(i, numShards) == shardIdx) {
                idx.add(i);
            }
        }
        return idx.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * 计算每分片大小
     */
    public int[] shardSizes(int totalSize, int numShards) {
        int[] sizes = new int[numShards];
        for (int i = 0; i < numShards; i++) {
            sizes[i] = (int) ((long) (i + 1) * totalSize / numShards) - (int) ((long) i * totalSize / numShards);
        }
        return sizes;
    }
}
