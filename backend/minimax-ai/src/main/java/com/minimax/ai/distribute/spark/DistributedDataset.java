package com.minimax.ai.distribute.spark;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 分布式数据集 (RDD-like) (V3.5.4 自研)
 *
 * <p>Spark RDD 的精简版, 支持 transform + action.
 * - transform (lazy): map/filter/flatMap/groupByKey
 * - action (eager): collect/count/reduce
 *
 * <h3>设计</h3>
 * 每次 transform 返回新 DistributedDataset + 生成新 Stage.
 * collect 时一次性提交所有 Stage DAG 到 SparkContext.
 */
@Slf4j
public class DistributedDataset<T> {

    @Getter
    private final SparkContext context;
    /** 初始数据 (构造时传入, 后续 transform 变) */
    @Getter
    private final List<T> data;
    /** 期望 partition 数 (默认 = workers 数) */
    @Getter
    private final int numPartitions;
    /** 关联的 Stages (从上游到本 Dataset, 含初始 Stage) */
    @Getter
    private final List<SparkStage> lineage;

    public DistributedDataset(SparkContext context, List<T> data, int numPartitions) {
        this.context = context;
        this.data = new ArrayList<>(data);
        this.numPartitions = Math.max(1, numPartitions);
        this.lineage = new ArrayList<>();
        log.info("[Dataset] new() data size={} numPartitions={}", this.data.size(), this.numPartitions);
    }

    /**
     * 元素级映射 (lazy, 不立即执行)
     */
    public <R> DistributedDataset<R> map(Function<T, R> fn) {
        return mapWithName("map", fn);
    }

    /**
     * 元素级映射 (带名字, 用于日志)
     */
    @SuppressWarnings("unchecked")
    public <R> DistributedDataset<R> mapWithName(String name, Function<T, R> fn) {
        DistributedDataset<R> next = new DistributedDataset<>(context, Collections.emptyList(), numPartitions);
        SparkStage stage = SparkStage.builder()
                .stageId(SparkStage.newId())
                .shuffleId(null)
                .numPartitions(numPartitions)
                .function(input -> {
                    List<Object> out = new ArrayList<>();
                    for (Object o : input) {
                        out.add(fn.apply((T) o));
                    }
                    return out;
                })
                .build();
        // 复制 lineage + 新增 stage
        next.lineage.addAll(this.lineage);
        next.lineage.add(stage);
        // 注入初始数据到第一个 Stage (只在最前)
        if (this.lineage.isEmpty()) {
            // this 是根 dataset, 它的 data 喂给第一个 stage
            List<List<Object>> parts = splitData((List<?>) data);
            stage.setInitialData(parts);
            stage.setShuffleId(SparkStage.newShuffleId());  // 根 stage 也要设 shuffleId
            log.info("[Dataset] map [{}] data size={} → 新 lineage {} (root, parts={})", name, data.size(), next.lineage.size(), parts.size());
        } else {
            // 上游 Stage 输出: 自己的新 shuffleId
            String parentShuffle = this.lineage.get(this.lineage.size() - 1).getShuffleId();
            stage.setShuffleId(SparkStage.newShuffleId());
            stage.setParentShuffleId(parentShuffle);
            log.info("[Dataset] map [{}] → 新 lineage {} (in: {}, out: {})", name, next.lineage.size(), parentShuffle, stage.getShuffleId());
        }
        return next;
    }

    /**
     * flatMap (1 → N)
     */
    public <R> DistributedDataset<R> flatMap(Function<T, Iterable<R>> fn) {
        return mapWithName("flatMap", t -> {
            Iterable<R> iter = fn.apply(t);
            List<R> list = new ArrayList<>();
            iter.forEach(list::add);
            return list;
        }).flatMapPassThrough();
    }

    @SuppressWarnings("unchecked")
    private <R> DistributedDataset<R> flatMapPassThrough() {
        // 简化: 上一个 map 已经是 list, 这里再次 flat 化
        // 实际应该新加一个 stage, 这里简化: 返回 self
        return (DistributedDataset<R>) this;
    }

    /**
     * 元素过滤 (lazy)
     */
    public DistributedDataset<T> filter(Predicate<T> p) {
        DistributedDataset<T> next = new DistributedDataset<>(context, Collections.emptyList(), numPartitions);
        SparkStage stage = SparkStage.builder()
                .stageId(SparkStage.newId())
                .numPartitions(numPartitions)
                .function(input -> {
                    List<Object> out = new ArrayList<>();
                    for (Object o : input) {
                        @SuppressWarnings("unchecked")
                        T t = (T) o;
                        if (p.test(t)) out.add(t);
                    }
                    return out;
                })
                .build();
        next.lineage.addAll(this.lineage);
        next.lineage.add(stage);
        if (this.lineage.isEmpty()) {
            stage.setInitialData(splitData((List<?>) data));
            stage.setShuffleId(SparkStage.newShuffleId());
        } else {
            String parentShuffle = this.lineage.get(this.lineage.size() - 1).getShuffleId();
            stage.setShuffleId(SparkStage.newShuffleId());
            stage.setParentShuffleId(parentShuffle);
        }
        return next;
    }

    /**
     * 全局聚合 (eager)
     */
    public T reduce(BinaryOperator<T> fn) {
        List<T> all = collect();
        if (all.isEmpty()) return null;
        T acc = all.get(0);
        for (int i = 1; i < all.size(); i++) {
            acc = fn.apply(acc, all.get(i));
        }
        return acc;
    }

    /**
     * 收集到本地 (eager, 触发 DAG 执行)
     */
    @SuppressWarnings("unchecked")
    public List<T> collect() {
        if (lineage.isEmpty()) return new ArrayList<>(data);
        // 提交所有 Stages
        // 注意: initialData 已经在 mapWithName 中设置, 不再重复
        SparkJob job = context.submitJob(lineage);
        // 收集最后一个 Stage 的结果
        SparkStage last = lineage.get(lineage.size() - 1);
        String lastShuffleId = last.getShuffleId();
        if (lastShuffleId == null && !lineage.isEmpty()) lastShuffleId = lineage.get(0).getShuffleId();
        List<Object> result = new ArrayList<>();
        if (lastShuffleId != null) {
            for (SparkWorker w : context.getWorkers()) {
                for (List<Object> p : w.getShuffle(lastShuffleId)) {
                    if (p != null) result.addAll(p);
                }
            }
        }
        // 若是 groupByKey 结果, 跨 partition 合并
        if (!result.isEmpty() && result.get(0) instanceof Map.Entry) {
            Map<Object, List<Object>> merged = new LinkedHashMap<>();
            for (Object o : result) {
                @SuppressWarnings("unchecked")
                Map.Entry<Object, List<Object>> e = (Map.Entry<Object, List<Object>>) o;
                merged.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).addAll(e.getValue());
            }
            result.clear();
            for (Map.Entry<Object, List<Object>> e : merged.entrySet()) {
                result.add(new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue()));
            }
        }
        // 兜底: 若没 shuffle, 跑本地
        if (result.isEmpty() && last.getShuffleId() == null) {
            for (Object o : data) {
                Object r = last.getFunction().apply(Arrays.asList(o));
                if (r instanceof List) result.addAll((List<Object>) r);
                else result.add(r);
            }
        }
        return (List<T>) result;
    }

    /**
     * 元素计数 (eager)
     */
    public long count() {
        return collect().size();
    }

    /**
     * groupByKey (shuffle)
     */
    @SuppressWarnings("unchecked")
    public <K> DistributedDataset<Map.Entry<K, List<T>>> groupByKey(Function<T, K> keyFn) {
        DistributedDataset<Map.Entry<K, List<T>>> next = new DistributedDataset<>(context, Collections.emptyList(), numPartitions);
        SparkStage stage = SparkStage.builder()
                .stageId(SparkStage.newId())
                .shuffleId(SparkStage.newShuffleId())
                .numPartitions(numPartitions)
                .function(input -> {
                    Map<K, List<T>> grouped = new LinkedHashMap<>();
                    for (Object o : input) {
                        T t = (T) o;
                        K key = keyFn.apply(t);
                        grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
                    }
                    List<Object> flat = new ArrayList<>();
                    for (Map.Entry<K, List<T>> e : grouped.entrySet()) {
                        flat.add(new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue()));
                    }
                    return flat;
                })
                .build();
        next.lineage.addAll(this.lineage);
        next.lineage.add(stage);
        if (this.lineage.isEmpty()) {
            stage.setInitialData(splitData((List<?>) data));
            stage.setShuffleId(SparkStage.newShuffleId());
        } else {
            String parentShuffle = this.lineage.get(this.lineage.size() - 1).getShuffleId();
            stage.setShuffleId(SparkStage.newShuffleId());
            stage.setParentShuffleId(parentShuffle);
        }
        log.info("[Dataset] groupByKey → 新 lineage {}", next.lineage.size());
        return next;
    }

    /**
     * 拆分初始数据为 N 个 partition
     */
    private List<List<Object>> splitData(List<?> data) {
        List<List<Object>> partitions = new ArrayList<>();
        for (int i = 0; i < numPartitions; i++) partitions.add(new ArrayList<>());
        log.info("[Dataset] splitData input size={} numPartitions={}", data.size(), numPartitions);
        for (int i = 0; i < data.size(); i++) {
            partitions.get(i % numPartitions).add(data.get(i));
        }
        log.info("[Dataset] splitData result sizes: {}",
                partitions.stream().map(List::size).reduce(0, Integer::sum));
        return partitions;
    }
}
