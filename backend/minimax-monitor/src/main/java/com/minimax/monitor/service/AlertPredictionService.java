package com.minimax.monitor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.monitor.entity.AlertEvent;
import com.minimax.monitor.mapper.AlertEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 告警趋势预测服务 (Day 53).
 *
 * 基于历史告警时间序列，用指数加权移动平均 (EWMA) 预测未来 N 天的告警趋势。
 *
 * 算法:
 * 1. 取近 N 天历史数据，按日聚合
 * 2. 对每日告警数做指数加权移动平均 (alpha=0.3)
 * 3. 线性回归斜率判断趋势方向 (上升/下降/平稳)
 * 4. 预测未来 N 天的告警数 (基于趋势 + 季节性校正)
 *
 * @author Mavis
 * @since Day 53
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertPredictionService {

    private final AlertEventMapper alertEventMapper;

    /** 默认历史窗口 (天) */
    private static final int DEFAULT_HISTORY_DAYS = 30;
    /** 预测天数 */
    private static final int DEFAULT_FORECAST_DAYS = 7;
    /** EWMA alpha */
    private static final double ALPHA = 0.3;

    /**
     * 告警趋势预测
     *
     * @param historyDays 历史窗口天数 (默认 30)
     * @param forecastDays 预测天数 (默认 7)
     * @param severity 可选，按级别过滤 (CRITICAL/WARNING/INFO)
     * @return 预测结果
     */
    public Map<String, Object> predict(Integer historyDays, Integer forecastDays, String severity) {
        int history = historyDays != null && historyDays > 0 ? historyDays : DEFAULT_HISTORY_DAYS;
        int forecast = forecastDays != null && forecastDays > 0 ? forecastDays : DEFAULT_FORECAST_DAYS;
        forecast = Math.min(forecast, 30); // 最多预测 30 天

        LocalDateTime since = LocalDateTime.now().minusDays(history);
        LocalDateTime now = LocalDateTime.now();

        // 1. 查历史告警
        LambdaQueryWrapper<AlertEvent> q = new LambdaQueryWrapper<>();
        q.ge(AlertEvent::getFiredAt, since).orderByAsc(AlertEvent::getFiredAt);
        if (severity != null && !severity.isBlank()) {
            q.eq(AlertEvent::getSeverity, severity.toUpperCase());
        }
        List<AlertEvent> events = alertEventMapper.selectList(q);

        // 2. 按日聚合
        Map<LocalDate, Long> dailyCounts = events.stream()
                .filter(e -> e.getFiredAt() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        e -> e.getFiredAt().toLocalDate(),
                        java.util.stream.Collectors.counting()));

        // 补齐缺失日期
        List<Map<String, Object>> series = new ArrayList<>();
        LocalDate cursor = since.toLocalDate();
        LocalDate end = now.toLocalDate();
        while (!cursor.isAfter(end)) {
            long count = dailyCounts.getOrDefault(cursor, 0L);
            series.add(Map.of("date", cursor.toString(), "count", count, "dayOfWeek", cursor.getDayOfWeek().getValue()));
            cursor = cursor.plusDays(1);
        }

        if (series.size() < 3) {
            return buildEmptyPrediction(forecast, history);
        }

        // 3. 提取日计数数组
        double[] counts = series.stream().mapToDouble(m -> ((Number) m.get("count")).doubleValue()).toArray();

        // 4. EWMA 平滑
        double ewma = counts[0];
        double[] smoothed = new double[counts.length];
        smoothed[0] = ewma;
        for (int i = 1; i < counts.length; i++) {
            ewma = ALPHA * counts[i] + (1 - ALPHA) * ewma;
            smoothed[i] = ewma;
        }

        // 5. 线性回归斜率 (判断趋势)
        double slope = linearRegressionSlope(smoothed);
        String trend;
        if (slope > 0.1) {
            trend = "RISING";
        } else if (slope < -0.1) {
            trend = "FALLING";
        } else {
            trend = "STABLE";
        }

        // 6. 周均化 (工作日 vs 周末因子)
        double[] weeklyFactor = weeklyPattern(smoothed);

        // 7. 预测未来 N 天
        double lastEwma = smoothed[smoothed.length - 1];
        List<Map<String, Object>> forecasts = new ArrayList<>();
        LocalDate forecastCursor = now.toLocalDate().plusDays(1);
        for (int i = 0; i < forecast; i++) {
            LocalDate date = forecastCursor.plusDays(i);
            // 基础预测 = EWMA + 趋势调整
            double trendAdjust = slope * (i + 1) * 0.1;
            double base = Math.max(0, lastEwma + trendAdjust);
            // 星期因子
            double dayFactor = weeklyFactor.length > 0 ? weeklyFactor[i % weeklyFactor.length] : 1.0;
            double predicted = base * dayFactor;
            long predictedCount = Math.round(predicted);

            forecasts.add(Map.of(
                    "date", date.toString(),
                    "dayOfWeek", date.getDayOfWeek().getValue(),
                    "predicted", predictedCount,
                    "confidence", confidence(smoothed.length, slope),
                    "riskLevel", riskLevel(predictedCount, lastEwma)
            ));
        }

        // 8. 风险预警
        List<String> warnings = new ArrayList<>();
        long recentAvg = Math.round(Arrays.stream(counts).skip(Math.max(0, counts.length - 7)).average().orElse(0));
        for (Map<String, Object> f : forecasts) {
            long p = ((Number) f.get("predicted")).longValue();
            if (p > recentAvg * 2 && recentAvg > 0) {
                warnings.add("⚠️ " + f.get("date") + " 预测告警数 (" + p + ") 显著高于近期均值 (" + recentAvg + ")，建议提前检查");
            }
        }

        return Map.ofEntries(
                Map.entry("historyDays", history),
                Map.entry("forecastDays", forecast),
                Map.entry("severity", severity != null ? severity : "ALL"),
                Map.entry("trend", trend),
                Map.entry("trendSlope", Math.round(slope * 1000.0) / 1000.0),
                Map.entry("recentAvgDaily", recentAvg),
                Map.entry("ewmaSmoothed", Math.round(lastEwma * 100.0) / 100.0),
                Map.entry("historicalSeries", series),
                Map.entry("forecasts", forecasts),
                Map.entry("warnings", warnings),
                Map.entry("confidence", confidence(smoothed.length, slope)),
                Map.entry("generatedAt", now.toString())
        );
    }

    /**
     * 按级别分别预测 (CRITICAL/WARNING/INFO)
     */
    public Map<String, Object> predictBySeverity(Integer historyDays, Integer forecastDays) {
        Map<String, Object> all = predict(historyDays, forecastDays, null);
        Map<String, Object> critical = predict(historyDays, forecastDays, "CRITICAL");
        Map<String, Object> warning = predict(historyDays, forecastDays, "WARNING");
        Map<String, Object> info = predict(historyDays, forecastDays, "INFO");

        return Map.ofEntries(
                Map.entry("all", all),
                Map.entry("critical", Map.of(
                        "trend", critical.get("trend"),
                        "trendSlope", critical.get("trendSlope"),
                        "recentAvgDaily", critical.get("recentAvgDaily"),
                        "forecasts", critical.get("forecasts")
                )),
                Map.entry("warning", Map.of(
                        "trend", warning.get("trend"),
                        "trendSlope", warning.get("trendSlope"),
                        "recentAvgDaily", warning.get("recentAvgDaily"),
                        "forecasts", warning.get("forecasts")
                )),
                Map.entry("info", Map.of(
                        "trend", info.get("trend"),
                        "trendSlope", info.get("trendSlope"),
                        "recentAvgDaily", info.get("recentAvgDaily"),
                        "forecasts", info.get("forecasts")
                )),
                Map.entry("generatedAt", java.time.LocalDateTime.now().toString())
        );
    }

    // ---- private helpers ----

    /** 线性回归斜率 */
    private double linearRegressionSlope(double[] values) {
        int n = values.length;
        if (n < 2) return 0.0;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values[i];
            sumXY += i * values[i];
            sumX2 += i * i;
        }
        double denom = n * sumX2 - sumX * sumX;
        if (Math.abs(denom) < 1e-10) return 0.0;
        return (n * sumXY - sumX * sumY) / denom;
    }

    /** 周模式因子 (工作日权重 vs 周末权重) */
    private double[] weeklyPattern(double[] smoothed) {
        // 简化版: 工作日权重略高，周末略低
        double workDay = 1.1;
        double weekend = 0.8;
        return new double[]{workDay, workDay, workDay, workDay, workDay, weekend, weekend};
    }

    /** 置信度 (0~1) */
    private double confidence(int dataPoints, double slope) {
        double base = Math.min(1.0, dataPoints / 30.0);
        double slopeFactor = 1.0 / (1.0 + Math.abs(slope) * 0.5);
        return Math.round(Math.min(1.0, base * slopeFactor) * 100.0) / 100.0;
    }

    /** 风险等级 */
    private String riskLevel(long predicted, double ewma) {
        if (predicted > ewma * 2.5 && ewma > 0) return "HIGH";
        if (predicted > ewma * 1.5 && ewma > 0) return "MEDIUM";
        return "LOW";
    }

    private Map<String, Object> buildEmptyPrediction(int forecast, int history) {
        List<Map<String, Object>> empty = new ArrayList<>();
        LocalDate cursor = LocalDate.now().plusDays(1);
        for (int i = 0; i < forecast; i++) {
            empty.add(Map.of("date", cursor.plusDays(i).toString(), "dayOfWeek",
                    cursor.plusDays(i).getDayOfWeek().getValue(), "predicted", 0L,
                    "confidence", 0.0, "riskLevel", "LOW"));
        }
        return Map.ofEntries(
                Map.entry("historyDays", history),
                Map.entry("forecastDays", forecast),
                Map.entry("severity", "ALL"),
                Map.entry("trend", "STABLE"),
                Map.entry("trendSlope", 0.0),
                Map.entry("recentAvgDaily", 0L),
                Map.entry("ewmaSmoothed", 0.0),
                Map.entry("historicalSeries", List.of()),
                Map.entry("forecasts", empty),
                Map.entry("warnings", List.of()),
                Map.entry("confidence", 0.0),
                Map.entry("generatedAt", LocalDateTime.now().toString())
        );
    }
}
