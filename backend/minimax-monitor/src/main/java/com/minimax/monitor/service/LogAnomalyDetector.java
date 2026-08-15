package com.minimax.monitor.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 运维日志无监督异常检测 (Day 30).
 *
 * <p>使用统计方法对日志指标（错误率/QPS/响应延迟等）进行实时异常检测，
 * 无需人工标注样本，适用于运维场景。
 *
 * <h3>检测算法</h3>
 * <ul>
 *   <li><b>Z-Score</b>: 值偏离均值超过 N 个标准差 → 异常（适合正态分布数据）</li>
 *   <li><b>Moving Average</b>: 滑动窗口均值，偏离超过阈值 → 异常</li>
 *   <li><b>EWMA</b>: 指数加权移动平均，快速响应突变</li>
 *   <li><b>IQR</b>: 四分位距，极端值检测</li>
 *   <li><b>Spike Detection</b>: 相邻差值突变检测</li>
 * </ul>
 *
 * <h3>告警级别</h3>
 * <pre>
 * score > 0.90  → CRITICAL (红色)
 * score > 0.75  → WARNING  (橙色)
 * score > 0.60  → INFO     (黄色)
 * score <= 0.60 → NORMAL   (绿色)
 * </pre>
 *
 * <p>配置项:
 * <pre>
 * minimax.monitor.anomaly.z-threshold=3.0
 * minimax.monitor.anomaly.window-size=60
 * minimax.monitor.anomaly.alpha=0.3
 * minimax.monitor.anomaly.enabled=true
 * minimax.monitor.anomaly.min-samples=10
 * </pre>
 */
@Slf4j
@Service
public class LogAnomalyDetector {

    @Value("${minimax.monitor.anomaly.z-threshold:3.0}")
    private double zThreshold;

    @Value("${minimax.monitor.anomaly.window-size:60}")
    private int windowSize;

    @Value("${minimax.monitor.anomaly.alpha:0.3}")
    private double ewmaAlpha;

    @Value("${minimax.monitor.anomaly.enabled:true}")
    private boolean enabled;

    @Value("${minimax.monitor.anomaly.min-samples:10}")
    private int minSamples;

    // ============== 状态存储 ==============

    /** 每指标一个滑动窗口 (key: metric + ":" + instanceId) */
    private final Map<String, MetricWindow> windows = new ConcurrentHashMap<>();

    /** EWMA 状态 */
    private final Map<String, EwmaState> ewmaStates = new ConcurrentHashMap<>();

    /** 上一时刻的值（用于突变检测） */
    private final Map<String, Double> lastValues = new ConcurrentHashMap<>();

    // ============== 公开 API ==============

    public boolean isEnabled() { return enabled; }

    /**
     * 实时检测单个数据点。
     *
     * @param metric     指标名 (如 "error_rate", "p99_latency", "qps")
     * @param value      当前值
     * @param instanceId 实例标识（可选，如 "auth:192.168.1.1"）
     * @return 检测结果
     */
    public AnomalyResult detect(String metric, double value, String instanceId) {
        if (!enabled) {
            return AnomalyResult.normal(metric, value, instanceId, "disabled");
        }

        String key = buildKey(metric, instanceId);
        MetricWindow window = windows.computeIfAbsent(key, k -> new MetricWindow(windowSize));

        window.add(value);

        if (window.size() < minSamples) {
            return AnomalyResult.normal(metric, value, instanceId, "warming-up");
        }

        return runAllAlgorithms(metric, value, instanceId, window, key);
    }

    /**
     * 批量检测（一次推送多个指标）。
     */
    public List<AnomalyResult> detectBatch(Map<String, Double> metrics, String instanceId) {
        return metrics.entrySet().stream()
                .map(e -> detect(e.getKey(), e.getValue(), instanceId))
                .toList();
    }

    /**
     * 获取指标当前统计摘要。
     */
    public MetricSummary getSummary(String metric, String instanceId) {
        String key = buildKey(metric, instanceId);
        MetricWindow window = windows.get(key);

        if (window == null) {
            return new MetricSummary(metric, instanceId, 0, 0, 0, 0, 0, 0, false);
        }

        double mean = window.mean();
        double std = window.std(mean);
        Double last = lastValues.get(key);
        double z = std > 0 ? Math.abs(last - mean) / std : 0;

        EwmaState ewma = ewmaStates.get(key);
        boolean anomalous = ewma != null && ewma.isAnomalous();

        return new MetricSummary(metric, instanceId, window.size(),
                mean, std, window.min(), window.max(), z, anomalous);
    }

    /**
     * 清除指定指标的历史窗口。
     */
    public void reset(String metric, String instanceId) {
        String key = buildKey(metric, instanceId);
        windows.remove(key);
        ewmaStates.remove(key);
        lastValues.remove(key);
        log.info("[Anomaly] reset {}", key);
    }

    public Set<String> activeMetrics() {
        return new HashSet<>(windows.keySet());
    }

    // ============== 核心检测算法 ==============

    private AnomalyResult runAllAlgorithms(String metric, double value,
                                          String instanceId, MetricWindow window, String key) {
        double mean = window.mean();
        double std = window.std(mean);

        // 1. Z-Score
        double zScore = std > 0 ? Math.abs(value - mean) / std : 0;
        double zScoreResult = normalizeZScore(zScore);

        // 2. EWMA
        double ewmaScore = computeEwmaAnomaly(key, value);

        // 3. IQR (四分位距)
        double iqrScore = computeIqrScore(window, value);

        // 4. 突变检测
        double spikeScore = computeSpikeScore(key, value);

        // 综合评分
        double composite = Math.round(
                (0.35 * zScoreResult + 0.30 * ewmaScore
                        + 0.20 * iqrScore + 0.15 * spikeScore) * 1000.0
        ) / 1000.0;

        // 低波动指标放大敏感度
        if (std > 0 && std < mean * 0.05 && zScore > 1.5) {
            composite = Math.max(composite, zScoreResult * 1.5);
        }

        AnomalyLevel level = classifyLevel(composite);
        String reason = buildReason(zScore, ewmaScore, iqrScore, spikeScore, mean, std, value);

        return new AnomalyResult(metric, value, instanceId, level, composite,
                reason, window.size(), mean, std);
    }

    // ---- Z-Score ----
    private double normalizeZScore(double z) {
        if (z <= zThreshold) return 0;
        return Math.min((z - zThreshold) / (zThreshold * 2), 1.0);
    }

    // ---- EWMA ----
    private double computeEwmaAnomaly(String key, double value) {
        EwmaState state = ewmaStates.computeIfAbsent(key,
                k -> new EwmaState(value));
        double predicted = state.ewma;
        double error = Math.abs(value - predicted);

        double newEwma = ewmaAlpha * value + (1 - ewmaAlpha) * state.ewma;
        double newVariance = ewmaAlpha * error * error + (1 - ewmaAlpha) * state.variance;
        state.update(newEwma, newVariance);

        double sigma = Math.sqrt(Math.max(state.variance, 0.001));
        return Math.min(error / (3 * sigma), 1.0);
    }

    // ---- IQR ----
    private double computeIqrScore(MetricWindow window, double value) {
        double q1 = window.percentile(25);
        double q3 = window.percentile(75);
        double iqr = q3 - q1;
        if (iqr <= 0) return 0;

        double lower = q1 - 1.5 * iqr;
        double upper = q3 + 1.5 * iqr;

        if (value < lower) return Math.min((lower - value) / Math.max(iqr, 0.001), 1.0);
        if (value > upper) return Math.min((value - upper) / Math.max(iqr, 0.001), 1.0);
        return 0;
    }

    // ---- 突变检测 ----
    private double computeSpikeScore(String key, double value) {
        Double last = lastValues.get(key);
        lastValues.put(key, value);
        if (last == null || last == 0) return 0;

        double ratio = Math.abs(value - last) / Math.max(last, 1.0);
        return Math.min(ratio / 2.0, 1.0);  // 变化超过 200% → 1.0
    }

    private AnomalyLevel classifyLevel(double score) {
        if (score > 0.90) return AnomalyLevel.CRITICAL;
        if (score > 0.75) return AnomalyLevel.WARNING;
        if (score > 0.60) return AnomalyLevel.INFO;
        return AnomalyLevel.NORMAL;
    }

    private String buildReason(double zScore, double ewma, double iqr,
                               double spike, double mean, double std, double value) {
        List<String> factors = new ArrayList<>();
        if (zScore > 0.5) factors.add(String.format("z=%.1f", zScore));
        if (ewma > 0.5)   factors.add(String.format("ewma=%.2f", ewma));
        if (iqr > 0.3)    factors.add(String.format("iqr=%.2f", iqr));
        if (spike > 0.5)  factors.add(String.format("spike=%.0f%%", spike * 100));

        String base = String.format("值=%.2f / 均值=%.2f±%.2f", value, mean, std);
        if (!factors.isEmpty()) {
            return base + " | 异常因子: " + String.join(", ", factors);
        }
        return base + " | 正常范围";
    }

    private String buildKey(String metric, String instanceId) {
        return (instanceId != null ? instanceId + ":" : "") + metric;
    }

    // ============== 数据结构 ==============

    /** 滑动窗口（环形缓冲区） */
    private static class MetricWindow {
        private final double[] data;
        private int head = 0, count = 0;
        private final int capacity;

        MetricWindow(int capacity) { this.capacity = capacity; this.data = new double[capacity]; }

        synchronized void add(double v) {
            data[head] = v;
            head = (head + 1) % capacity;
            if (count < capacity) count++;
        }

        synchronized int size() { return count; }
        synchronized double get(int i) {
            return data[(head - count + i + capacity) % capacity];
        }

        synchronized List<Double> snapshot() {
            List<Double> out = new ArrayList<>(count);
            for (int i = 0; i < count; i++) out.add(get(i));
            return out;
        }

        synchronized double min() {
            return snapshot().stream().mapToDouble(Double::doubleValue).min().orElse(0);
        }

        synchronized double max() {
            return snapshot().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        }

        synchronized double mean() {
            if (count == 0) return 0;
            return snapshot().stream().mapToDouble(Double::doubleValue).sum() / count;
        }

        synchronized double std(double mean) {
            if (count < 2) return 0;
            double variance = snapshot().stream()
                    .mapToDouble(d -> (d - mean) * (d - mean)).sum() / (count - 1);
            return Math.sqrt(Math.max(variance, 0));
        }

        synchronized double percentile(int p) {
            List<Double> sorted = snapshot().stream().sorted().toList();
            if (sorted.isEmpty()) return 0;
            int idx = Math.max(0, Math.min((int) Math.ceil(p / 100.0 * sorted.size()) - 1, sorted.size() - 1));
            return sorted.get(idx);
        }
    }

    /** EWMA 状态 */
    private static class EwmaState {
        double ewma;
        double variance;
        boolean anomalous;

        EwmaState(double initial) { this.ewma = initial; this.variance = 0.01; }

        void update(double newEwma, double newVariance) {
            this.ewma = newEwma;
            this.variance = newVariance;
            this.anomalous = newVariance > 0.1;
        }

        boolean isAnomalous() { return anomalous; }
    }

    // ============== 结果类 ==============

    @Getter
    public static class AnomalyResult {
        private final String metric;
        private final double value;
        private final String instanceId;
        private final AnomalyLevel level;
        private final double score;     // 0.0 ~ 1.0
        private final String reason;
        private final int windowSize;
        private final double mean;
        private final double std;

        public AnomalyResult(String metric, double value, String instanceId,
                            AnomalyLevel level, double score, String reason,
                            int windowSize, double mean, double std) {
            this.metric = metric;
            this.value = value;
            this.instanceId = instanceId;
            this.level = level;
            this.score = Math.max(0.0, Math.min(1.0, score));
            this.reason = reason;
            this.windowSize = windowSize;
            this.mean = mean;
            this.std = std;
        }

        public boolean isAnomalous() { return level != AnomalyLevel.NORMAL; }
        public boolean needsAlert() { return level == AnomalyLevel.WARNING || level == AnomalyLevel.CRITICAL; }

        public static AnomalyResult normal(String metric, double value, String instanceId, String reason) {
            return new AnomalyResult(metric, value, instanceId, AnomalyLevel.NORMAL, 0.0, reason, 0, 0, 0);
        }
    }

    @Getter
    public static class MetricSummary {
        private final String metric;
        private final String instanceId;
        private final int sampleCount;
        private final double mean;
        private final double std;
        private final double minValue;
        private final double maxValue;
        private final double currentZScore;
        private final boolean currentlyAnomalous;

        public MetricSummary(String metric, String instanceId, int sampleCount,
                            double mean, double std, double minValue, double maxValue,
                            double currentZScore, boolean currentlyAnomalous) {
            this.metric = metric;
            this.instanceId = instanceId;
            this.sampleCount = sampleCount;
            this.mean = mean;
            this.std = std;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.currentZScore = currentZScore;
            this.currentlyAnomalous = currentlyAnomalous;
        }
    }

    public enum AnomalyLevel {
        NORMAL,   // ≤ 0.60
        INFO,     // 0.60 ~ 0.75
        WARNING,  // 0.75 ~ 0.90
        CRITICAL  // > 0.90
    }
}
