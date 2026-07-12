package com.minimax.ai.distributed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 分布式训练器 (V3.3.3 数据并行协调)
 *
 * <p>协调多个 worker 同步训练:
 *   1. 数据分片 (DataSharder)
 *   2. 各 worker 并行计算梯度 (mock)
 *   3. All-Reduce 同步梯度 (AllReduce)
 *   4. 各 worker 用同步后梯度更新参数
 *
 * <h3>模拟</h3>
 * <p>沙箱不真跑训练, 用 mock 梯度生成
 * <p>生产: 接 PyTorch DDP / Horovod / DeepSpeed
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedTrainer {

    private final DataSharder sharder;
    private final AllReduce allReduce;

    /**
     * 模拟一次训练 step
     *
     * @param totalDataSize   总样本数
     * @param numWorkers      worker 数 (e.g. = GPU 数)
     * @param gradDim         梯度维度
     * @return 同步后梯度
     */
    public double[] trainStep(int totalDataSize, int numWorkers, int gradDim) {
        // 1. 每个 worker 拿分片
        Map<Integer, int[]> shards = new HashMap<>();
        for (int w = 0; w < numWorkers; w++) {
            shards.put(w, sharder.shard(totalDataSize, numWorkers, w,
                    DataSharder.Strategy.ROUND_ROBIN));
        }
        log.info("[dist-train] 分片: total={}, workers={}, 各分片大小={}",
                totalDataSize, numWorkers, shards.values().stream().mapToInt(a -> a.length).toArray());
        // 2. 各 worker 计算梯度 (mock: 随机梯度)
        Map<String, double[]> gradients = new HashMap<>();
        Random rng = new Random();
        for (int w = 0; w < numWorkers; w++) {
            int[] shard = shards.get(w);
            double[] grad = new double[gradDim];
            // 用 shard 大小加权 (大 shard 贡献大)
            double weight = (double) shard.length / totalDataSize;
            for (int i = 0; i < gradDim; i++) {
                grad[i] = (rng.nextDouble() - 0.5) * weight;
            }
            gradients.put("w" + w, grad);
        }
        // 3. All-Reduce
        double[] synced = allReduce.allReduce(gradients, AllReduce.ReduceOp.MEAN);
        log.info("[dist-train] 同步完成: 梯度 norm={}", allReduce.norm(synced));
        return synced;
    }

    /**
     * 模拟多步训练
     */
    public List<double[]> trainMultiStep(int steps, int totalDataSize, int numWorkers, int gradDim) {
        List<double[]> history = new ArrayList<>();
        for (int s = 0; s < steps; s++) {
            history.add(trainStep(totalDataSize, numWorkers, gradDim));
        }
        return history;
    }

    /**
     * 计算分片信息
     */
    public Map<String, Object> shardInfo(int totalSize, int numShards) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalSize", totalSize);
        out.put("numShards", numShards);
        out.put("shardSizes", sharder.shardSizes(totalSize, numShards));
        // 4 策略下各分片索引
        Map<String, int[]> byStrategy = new LinkedHashMap<>();
        for (DataSharder.Strategy st : DataSharder.Strategy.values()) {
            int[] idx0 = sharder.shard(totalSize, numShards, 0, st);
            byStrategy.put(st.name(), idx0);
        }
        out.put("shard0Indices", byStrategy);
        return out;
    }
}
