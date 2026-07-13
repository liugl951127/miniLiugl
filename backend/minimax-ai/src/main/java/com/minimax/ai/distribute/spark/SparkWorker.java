package com.minimax.ai.distribute.spark;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spark Worker 节点 (V3.5.4 自研多机并行)
 *
 * <h3>职责</h3>
 * - 执行 Task (Stage 的最小执行单元)
 * - 存储 Shuffle 数据 (跨节点 exchange)
 * - 心跳上报到 Master
 *
 * <h3>沙箱模式</h3>
 * 真实生产用 Netty / Akka; 沙箱用 in-memory transport (ConcurrentHashMap)
 */
@Slf4j
public class SparkWorker {

    @Getter
    private final String workerId;
    @Getter
    private final String address;  // host:port
    @Getter
    private final int cores;
    @Getter
    private final long memoryMb;
    @Getter
    private volatile boolean alive = true;
    @Getter
    private volatile long lastHeartbeat = System.currentTimeMillis();

    /** Task 缓存: taskId → result */
    private final Map<String, Object> taskResults = new ConcurrentHashMap<>();
    /** Shuffle 数据: shuffleId → List<Partition> */
    private final Map<String, List<List<Object>>> shuffleData = new ConcurrentHashMap<>();
    /** 统计 */
    @Getter
    private long tasksExecuted = 0;
    @Getter
    private long tasksFailed = 0;

    public SparkWorker(String workerId, String address, int cores, long memoryMb) {
        this.workerId = workerId;
        this.address = address;
        this.cores = cores;
        this.memoryMb = memoryMb;
    }

    /**
     * 执行一个 Task
     */
    public TaskResult executeTask(SparkTask task) {
        if (!alive) {
            return TaskResult.failed("worker not alive: " + workerId);
        }
        touch();
        long start = System.currentTimeMillis();
        try {
            Object result = task.getFunction().apply(task.getPartition());
            long duration = System.currentTimeMillis() - start;
            taskResults.put(task.getTaskId(), result);
            tasksExecuted++;
            return TaskResult.success(result, duration);
        } catch (Exception e) {
            tasksFailed++;
            log.warn("[Worker-{}] Task 失败: {}", workerId, e.getMessage());
            return TaskResult.failed(e.getMessage());
        }
    }

    /**
     * 存 Shuffle 输出
     */
    public void putShuffle(String shuffleId, int partitionIdx, List<Object> data) {
        shuffleData.computeIfAbsent(shuffleId, k -> new ArrayList<>());
        List<List<Object>> partitions = shuffleData.get(shuffleId);
        while (partitions.size() <= partitionIdx) {
            partitions.add(new ArrayList<>());
        }
        partitions.set(partitionIdx, data);
        log.info("[Worker-{}] shuffle={} partition={} 存 {} 条", workerId, shuffleId, partitionIdx, data.size());
    }

    /**
     * 读 Shuffle 数据
     */
    public List<List<Object>> getShuffle(String shuffleId) {
        return shuffleData.getOrDefault(shuffleId, Collections.emptyList());
    }

    /**
     * 读 Shuffle 某个 partition
     */
    public List<Object> getShufflePartition(String shuffleId, int partitionIdx) {
        List<List<Object>> all = shuffleData.get(shuffleId);
        if (all == null || partitionIdx >= all.size()) return Collections.emptyList();
        return all.get(partitionIdx);
    }

    /**
     * 杀掉 worker (测试用)
     */
    public void kill() {
        alive = false;
    }

    /**
     * 重启 worker
     */
    public void revive() {
        alive = true;
        touch();
    }

    public void touch() {
        lastHeartbeat = System.currentTimeMillis();
    }
}
