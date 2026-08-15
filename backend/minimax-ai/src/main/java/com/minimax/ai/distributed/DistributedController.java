package com.minimax.ai.distributed;

import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 分布式训练 REST API (V3.3.3)
 */
@Tag(name = "分布式训练")
@RestController
@RequestMapping("/api/v1/ai/distributed")
@RequiredArgsConstructor
public class DistributedController {

    private final DataSharder sharder;
    private final AllReduce allReduce;
    private final DistributedTrainer trainer;

    /**
     * 分片 (单独 API)
     */
    @Operation(summary = "数据分片")
    @PostMapping("/shard")
    public Result<Map<String, Object>> shard(@RequestBody Map<String, Object> body) {
        int totalSize = ((Number) body.get("totalSize")).intValue();
        int numShards = ((Number) body.get("numShards")).intValue();
        int shardIdx = ((Number) body.getOrDefault("shardIdx", 0)).intValue();
        String strategy = (String) body.getOrDefault("strategy", "ROUND_ROBIN");
        long seed = body.get("seed") == null ? 42L : ((Number) body.get("seed")).longValue();
        int[] indices = sharder.shard(totalSize, numShards, shardIdx,
                DataSharder.Strategy.valueOf(strategy), seed);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("shardIdx", shardIdx);
        out.put("numShards", numShards);
        out.put("totalSize", totalSize);
        out.put("size", indices.length);
        out.put("indices", indices);
        return Result.ok(out);
    }

    /**
     * All-Reduce (单独 API)
     */
    @Operation(summary = "All-Reduce 梯度归约")
    @PostMapping("/all-reduce")
    public Result<double[]> allReduce(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        Map<String, List<Number>> raw = (Map<String, List<Number>>) body.get("gradients");
        String op = (String) body.getOrDefault("op", "MEAN");
        // 转 double[][]
        Map<String, double[]> grads = new HashMap<>();
        for (Map.Entry<String, List<Number>> e : raw.entrySet()) {
            double[] arr = new double[e.getValue().size()];
            for (int i = 0; i < arr.length; i++) arr[i] = e.getValue().get(i).doubleValue();
            grads.put(e.getKey(), arr);
        }
        return Result.ok(allReduce.allReduce(grads, AllReduce.ReduceOp.valueOf(op)));
    }

    /**
     * 模拟训练 step
     */
    @Operation(summary = "模拟分布式训练 step")
    @PostMapping("/train/step")
    public Result<Map<String, Object>> trainStep(@RequestBody Map<String, Object> body) {
        int totalData = ((Number) body.getOrDefault("totalData", 1000)).intValue();
        int numWorkers = ((Number) body.getOrDefault("numWorkers", 4)).intValue();
        int gradDim = ((Number) body.getOrDefault("gradDim", 10)).intValue();
        double[] grad = trainer.trainStep(totalData, numWorkers, gradDim);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("syncedGrad", grad);
        out.put("norm", allReduce.norm(grad));
        return Result.ok(out);
    }

    /**
     * 分片信息
     */
    @Operation(summary = "分片信息 (4 策略 + 各分片大小)")
    @GetMapping("/shard/info")
    public Result<Map<String, Object>> shardInfo(@RequestParam(defaultValue = "1000") int totalSize,
                                                   @RequestParam(defaultValue = "4") int numShards) {
        return Result.ok(trainer.shardInfo(totalSize, numShards));
    }
}
