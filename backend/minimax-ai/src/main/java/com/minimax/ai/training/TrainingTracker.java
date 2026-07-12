package com.minimax.ai.training;

import com.minimax.ai.tensorboard.TfEventWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 训练指标追踪器 (V2.7.5 - TensorBoard 风格)
 *
 * <p>每个训练任务维护一个内存中的指标序列, 通过 REST API 暴露给前端.</p>
 *
 * <h3>指标维度</h3>
 * <ul>
 *   <li>loss (训练集)</li>
 *   <li>val_loss (验证集)</li>
 *   <li>accuracy (可选)</li>
 *   <li>perplexity (困惑度 = exp(loss))</li>
 *   <li>learning_rate (学习率)</li>
 *   <li>epoch (当前 epoch)</li>
 *   <li>step (步数)</li>
 *   <li>elapsed (耗时, 毫秒)</li>
 * </ul>
 *
 * <h3>任务生命周期</h3>
 * <pre>
 *   PENDING -> RUNNING -> COMPLETED / FAILED
 * </pre>
 *
 * <h3>算法</h3>
 * <p>滑动窗口计算 EMA (指数移动平均), 用于平滑曲线展示</p>
 */
@Slf4j
@Component
public class TrainingTracker {

    public static class MetricPoint {
        public int epoch;
        public int step;
        public double loss;
        public double valLoss;
        public double accuracy;
        public double learningRate;
        public long elapsedMs;
        public LocalDateTime timestamp;

        public MetricPoint() {}

        public MetricPoint(int epoch, int step, double loss, double valLoss,
                           double accuracy, double lr, long elapsed) {
            this.epoch = epoch;
            this.step = step;
            this.loss = loss;
            this.valLoss = valLoss;
            this.accuracy = accuracy;
            this.learningRate = lr;
            this.elapsedMs = elapsed;
            this.timestamp = LocalDateTime.now();
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("epoch", epoch);
            m.put("step", step);
            m.put("loss", loss);
            m.put("valLoss", valLoss);
            m.put("accuracy", accuracy);
            m.put("perplexity", loss > 0 ? Math.exp(Math.min(loss, 20)) : 0);
            m.put("learningRate", learningRate);
            m.put("elapsedMs", elapsedMs);
            m.put("timestamp", timestamp != null ? timestamp.toString() : null);
            return m;
        }
    }

    public static class TaskInfo {
        public String taskId;
        public String name;
        public String model;
        public String status = "PENDING";      // PENDING / RUNNING / COMPLETED / FAILED
        public int totalEpochs;
        public int currentEpoch;
        public long startTimeMs;
        public long endTimeMs;
        public List<MetricPoint> history = Collections.synchronizedList(new ArrayList<>());
        public String config;                   // JSON 字符串
        public String error;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("taskId", taskId);
            m.put("name", name);
            m.put("model", model);
            m.put("status", status);
            m.put("totalEpochs", totalEpochs);
            m.put("currentEpoch", currentEpoch);
            m.put("startTimeMs", startTimeMs);
            m.put("endTimeMs", endTimeMs);
            m.put("durationMs", (endTimeMs > 0 ? endTimeMs : System.currentTimeMillis()) - startTimeMs);
            m.put("pointCount", history.size());
            m.put("error", error);
            return m;
        }
    }

    private final Map<String, TaskInfo> tasks = new ConcurrentHashMap<>();

    /**
     * V2.8.7: TensorBoard 写入器 (可空, 启动时注入)
     */
    @Autowired(required = false)
    private TfEventWriter tfEventWriter;

    public String createTask(String name, String model, int totalEpochs, String config) {
        String id = "train-" + System.currentTimeMillis() + "-" + Math.abs(name.hashCode() % 10000);
        TaskInfo t = new TaskInfo();
        t.taskId = id;
        t.name = name;
        t.model = model;
        t.totalEpochs = totalEpochs;
        t.config = config;
        t.startTimeMs = System.currentTimeMillis();
        tasks.put(id, t);
        // V2.8.7: 写入 TensorBoard 文本 (config)
        if (tfEventWriter != null) {
            tfEventWriter.writeText(id, "config", 0, config == null ? "" : config);
        }
        log.info("Created training task: {}", id);
        return id;
    }

    public void start(String taskId) {
        TaskInfo t = tasks.get(taskId);
        if (t != null) t.status = "RUNNING";
    }

    public void record(String taskId, MetricPoint p) {
        TaskInfo t = tasks.get(taskId);
        if (t == null) return;
        t.history.add(p);
        t.currentEpoch = p.epoch;
        // V2.8.7: 同步写入 TensorBoard events.tfevents (可供 TensorBoard/WandB 读取)
        if (tfEventWriter != null) {
            int step = p.step > 0 ? p.step : p.epoch;
            tfEventWriter.writeScalar(taskId, "loss", step, p.loss);
            if (p.valLoss > 0) {
                tfEventWriter.writeScalar(taskId, "val_loss", step, p.valLoss);
            }
            if (p.accuracy > 0) {
                tfEventWriter.writeScalar(taskId, "accuracy", step, p.accuracy);
            }
            if (p.learningRate > 0) {
                tfEventWriter.writeScalar(taskId, "learning_rate", step, p.learningRate);
            }
        }
    }

    public void complete(String taskId) {
        TaskInfo t = tasks.get(taskId);
        if (t != null) {
            t.status = "COMPLETED";
            t.endTimeMs = System.currentTimeMillis();
        }
    }

    public void fail(String taskId, String error) {
        TaskInfo t = tasks.get(taskId);
        if (t != null) {
            t.status = "FAILED";
            t.error = error;
            t.endTimeMs = System.currentTimeMillis();
        }
    }

    public TaskInfo get(String taskId) { return tasks.get(taskId); }

    public List<TaskInfo> listAll() {
        return tasks.values().stream()
                .sorted(Comparator.comparingLong((TaskInfo t) -> t.startTimeMs).reversed())
                .collect(Collectors.toList());
    }

    public List<MetricPoint> getHistory(String taskId) {
        TaskInfo t = tasks.get(taskId);
        return t == null ? List.of() : new ArrayList<>(t.history);
    }

    public boolean remove(String taskId) {
        return tasks.remove(taskId) != null;
    }

    /**
     * EMA (Exponential Moving Average) 平滑
     *
     * <p><b>公式</b>: {@code EMA_t = α * value_t + (1 - α) * EMA_{t-1}}
     *
     * <p><b>直觉</b>:
     *   简单滑动平均对窗口内所有值等权; EMA 对近期值赋高权重, 远期值指数衰减.
     *   α 越大越敏感 (跟震荡), α 越小越平滑 (延迟高).
     *
     * <p><b>应用</b>: 训练 loss 曲线平滑, 去除 batch 噪点.
     *
     * <p><b>参数效应</b>:
     * <ul>
     *   <li>α = 0.1 — 强平滑 (推荐, 默认)</li>
     *   <li>α = 0.3 — 中等平滑</li>
     *   <li>α = 0.9 — 几乎不平滑 (接近原始曲线)</li>
     * </ul>
     *
     * <p><b>复杂度</b>: O(N)
     *
     * @param values 原始值序列
     * @param alpha 平滑因子, [0, 1]
     * @return 平滑后的序列
     */
    public static List<Double> ema(List<Double> values, double alpha) {
        if (values.isEmpty()) return List.of();
        List<Double> out = new ArrayList<>(values.size());
        // EMA_0 = value_0 (种子值, 可换成前 N 项平均更准)
        double prev = values.get(0);
        for (double v : values) {
            // 递推公式: prev ← α·v + (1-α)·prev
            prev = alpha * v + (1 - alpha) * prev;
            out.add(prev);
        }
        return out;
    }
}
