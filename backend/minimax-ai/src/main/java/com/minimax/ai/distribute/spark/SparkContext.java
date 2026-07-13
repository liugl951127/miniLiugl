package com.minimax.ai.distribute.spark;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Spark 风格分布式计算引擎 (V3.5.4 自研多机并行)
 *
 * <h3>架构</h3>
 * - SparkContext: 主入口, 客户端
 * - SparkWorker × N: 执行节点
 * - MasterScheduler: DAG 调度 (本类内)
 * - 沙箱模式: 3 worker 模拟多机, in-memory transport
 *
 * <h3>API (Spark 风格)</h3>
 * <ul>
 *   <li>{@link #parallelize} - 集合并行化 (numSlices 个 partition)</li>
 *   <li>{@link #textFile} - 文本并行读取 (按行切分)</li>
 *   <li>{@link DistributedDataset#map} - 元素级映射</li>
 *   <li>{@link DistributedDataset#flatMap} - 元素级 1→N 映射</li>
 *   <li>{@link DistributedDataset#filter} - 元素过滤</li>
 *   <li>{@link DistributedDataset#reduce} - 全局聚合</li>
 *   <li>{@link DistributedDataset#groupByKey} - 按 key 分组 (shuffle)</li>
 *   <li>{@link DistributedDataset#collect} - 收集到本地</li>
 *   <li>{@link DistributedDataset#count} - 元素计数</li>
 *   <li>{@link DistributedDataset#saveAsTextFile} - 持久化</li>
 * </ul>
 *
 * <h3>复杂度</h3>
 * map/filter: O(N/N) 每 worker, 0 shuffle
 * reduce: O(N/N) 每 worker 局部 reduce + O(N) driver collect
 * groupByKey: O(N) shuffle + O(N/K) reduce (K=partition 数)
 */
@Slf4j
@Service
public class SparkContext {

    /** Master 节点 ID (本进程模拟) */
    @Getter
    private final String masterId = "master-" + UUID.randomUUID().toString().substring(0, 6);

    /** Worker 列表 (默认 3 个, 模拟多机) */
    @Getter
    private final List<SparkWorker> workers;

    /** Job 提交历史 (jobId → Job) */
    @Getter
    private final Map<String, SparkJob> jobs = new ConcurrentHashMap<>();

    /** 任务调度线程池 */
    private final ExecutorService executor;

    /** 统计 */
    @Getter
    private final Stats stats = new Stats();

    public SparkContext() {
        this(3);
    }

    public SparkContext(int numWorkers) {
        this.workers = new ArrayList<>();
        for (int i = 0; i < numWorkers; i++) {
            String id = "worker-" + i;
            String addr = "localhost:" + (9000 + i);
            workers.add(new SparkWorker(id, addr, 4, 2048));
        }
        this.executor = Executors.newFixedThreadPool(numWorkers * 2, r -> {
            Thread t = new Thread(r, "spark-task");
            t.setDaemon(true);
            return t;
        });
        log.info("[SparkContext] 启动 master={}, workers={}", masterId, numWorkers);
    }

    /**
     * 集合并行化
     */
    public DistributedDataset parallelize(List<?> data, int numSlices) {
        return new DistributedDataset(this, data, numSlices);
    }

    /**
     * 文本文件并行读取 (按行)
     */
    public DistributedDataset textFile(String content, int numSlices) {
        String[] lines = content.split("\n");
        return new DistributedDataset(this, Arrays.asList(lines), numSlices);
    }

    /**
     * 提交一个 Job (Stages DAG)
     */
    public SparkJob submitJob(List<SparkStage> stages) {
        String jobId = "job-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
        SparkJob job = new SparkJob();
        job.setId(jobId);
        job.setStages(stages);
        job.setCompleted(false);
        jobs.put(jobId, job);
        log.info("[SparkContext] 提交 Job {}, {} 个 Stage", jobId, stages.size());
        executeJob(job);
        return job;
    }

    /**
     * 执行 Job (DAG 拓扑排序)
     */
    private void executeJob(SparkJob job) {
        // 拓扑排序 (Stage 依赖)
        List<SparkStage> sorted = topoSort(job.getStages());
        log.info("[SparkContext] Job {} 拓扑序: {}", job.getId(), sorted.size());
        // 顺序执行每个 Stage
        for (SparkStage stage : sorted) {
            executeStage(job, stage);
        }
        job.setCompleted(true);
        stats.recordJob(sorted.size());
    }

    /**
     * 执行单个 Stage
     */
    private void executeStage(SparkJob job, SparkStage stage) {
        log.info("[SparkContext] Stage {} ({} partitions) 开始", stage.getStageId(), stage.getNumPartitions());
        // 0. 提前设 shuffleId
        if (stage.getShuffleId() == null) {
            stage.setShuffleId(SparkStage.newShuffleId());
        }
        String stageShuffleId = stage.getShuffleId();
        // 1. 拿输入数据
        List<List<Object>> inputPartitions = getInputPartitions(stage);
        log.info("[SparkContext] Stage {} inputPartitions sizes: {}",
                stage.getStageId(),
                inputPartitions.stream().map(List::size).reduce(0, Integer::sum));
        // 2. 生成 Tasks
        List<SparkTask> tasks = new ArrayList<>();
        for (int i = 0; i < stage.getNumPartitions(); i++) {
            String taskId = "task-" + stage.getStageId() + "-" + i;
            List<Object> input = i < inputPartitions.size() ? inputPartitions.get(i) : Collections.emptyList();
            tasks.add(SparkTask.builder()
                    .taskId(taskId)
                    .stageId(stage.getStageId())
                    .jobId(job.getId())
                    .partition(input)
                    .partitionIdx(i)
                    .function(stage.getFunction())
                    .build());
        }
        // 3. 分发到 workers
        List<CompletableFuture<TaskResult>> futures = new ArrayList<>();
        Map<Integer, List<Object>> shuffleOutputs = new ConcurrentHashMap<>();
        for (int i = 0; i < tasks.size(); i++) {
            SparkTask task = tasks.get(i);
            SparkWorker worker = workers.get(i % workers.size());
            int partitionIdx = i;
            CompletableFuture<TaskResult> future = CompletableFuture.supplyAsync(() -> {
                TaskResult r = worker.executeTask(task);
                if (r.isSuccess() && r.getResult() instanceof List) {
                    shuffleOutputs.put(partitionIdx, (List<Object>) r.getResult());
                }
                return r;
            }, executor);
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        // 4. 存 shuffle 到 worker
        for (Map.Entry<Integer, List<Object>> e : shuffleOutputs.entrySet()) {
            SparkWorker storeWorker = workers.get(e.getKey() % workers.size());
            storeWorker.putShuffle(stageShuffleId, e.getKey(), e.getValue());
        }
        stage.setCompleted(true);
        stats.recordStage(futures.size());
        log.info("[SparkContext] Stage {} 完成 ({} tasks, shuffle={})", stage.getStageId(), futures.size(), stageShuffleId);
    }

    /**
     * 拿 Stage 输入 (从父 Stage shuffle 或初始 dataset)
     */
    private List<List<Object>> getInputPartitions(SparkStage stage) {
        // 优先用 parentShuffleId (从父 Stage 输出读)
        if (stage.getParentShuffleId() != null) {
            return readShuffleFromAllWorkers(stage.getParentShuffleId());
        }
        // 根 Stage: 优先用 initialData (来自 dataset)
        if (stage.getInitialData() != null) {
            return stage.getInitialData();
        }
        if (stage.getShuffleId() != null) {
            return readShuffleFromAllWorkers(stage.getShuffleId());
        }
        // 根 Stage, 走 dataset 初始 partition
        if (stage.getInitialData() != null) {
            return stage.getInitialData();
        }
        return Collections.emptyList();
    }

    /**
     * 从所有 worker 聚合 shuffle 数据
     */
    private List<List<Object>> readShuffleFromAllWorkers(String shuffleId) {
        log.info("[SparkContext] readShuffleFromAllWorkers shuffleId={}", shuffleId);
        Map<Integer, List<Object>> merged = new HashMap<>();
        for (SparkWorker w : workers) {
            List<List<Object>> data = w.getShuffle(shuffleId);
            log.info("[SparkContext] worker={} shuffleData sizes: {}", w.getWorkerId(),
                    data.stream().map(List::size).reduce(0, Integer::sum));
            for (int i = 0; i < data.size(); i++) {
                merged.computeIfAbsent(i, k -> new ArrayList<>()).addAll(data.get(i));
            }
        }
        List<List<Object>> sorted = new ArrayList<>();
        for (int i = 0; i < merged.size(); i++) {
            sorted.add(merged.getOrDefault(i, Collections.emptyList()));
        }
        return sorted;
    }

    /**
     * 拓扑排序
     */
    private List<SparkStage> topoSort(List<SparkStage> stages) {
        Map<String, SparkStage> byId = new HashMap<>();
        for (SparkStage s : stages) byId.put(s.getStageId(), s);
        List<SparkStage> sorted = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (SparkStage s : stages) dfs(s, byId, visited, sorted);
        return sorted;
    }

    private void dfs(SparkStage s, Map<String, SparkStage> byId, Set<String> visited, List<SparkStage> sorted) {
        if (visited.contains(s.getStageId())) return;
        visited.add(s.getStageId());
        if (s.getParentId() != null) {
            SparkStage parent = byId.get(s.getParentId());
            if (parent != null) dfs(parent, byId, visited, sorted);
        }
        sorted.add(s);
    }

    /**
     * 关闭
     */
    public void stop() {
        executor.shutdownNow();
        log.info("[SparkContext] 关闭");
    }

    /**
     * 统计
     */
    @Getter
    public static class Stats {
        private final AtomicLong jobs = new AtomicLong(0);
        private final AtomicLong stages = new AtomicLong(0);
        private final AtomicLong tasks = new AtomicLong(0);

        public void recordJob(int numStages) {
            jobs.incrementAndGet();
        }

        public void recordStage(int numTasks) {
            stages.incrementAndGet();
            tasks.addAndGet(numTasks);
        }

        public Map<String, Object> snapshot() {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("jobs", jobs.get());
            s.put("stages", stages.get());
            s.put("tasks", tasks.get());
            return s;
        }
    }
}
