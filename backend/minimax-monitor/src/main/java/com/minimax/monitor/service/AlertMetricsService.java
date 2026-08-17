package com.minimax.monitor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.monitor.entity.AlertEvent;
import com.minimax.monitor.mapper.AlertEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 告警 SLA 统计服务 (Day 43).
 *
 * 计算指标:
 * - MTBF: Mean Time Between Failures (平均故障间隔时间)
 * - MTTR: Mean Time To Recover (平均恢复时间)
 * - 可用率: Uptime / Total Time
 * - 总告警数 / 活跃告警数 / 已恢复数
 *
 * @author Mavis
 * @since Day 43
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertMetricsService {

    private final AlertEventMapper alertEventMapper;

    /** 默认统计窗口(天) */
    private static final int DEFAULT_WINDOW_DAYS = 30;

    /**
     * SLA 统计 (Day 43).
     *
     * @param windowDays 统计窗口，默认 30 天
     * @return SLA 指标 Map
     */
    public Map<String, Object> getSlaMetrics(Integer windowDays) {
        int days = windowDays != null && windowDays > 0 ? windowDays : DEFAULT_WINDOW_DAYS;
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<AlertEvent> q = new LambdaQueryWrapper<>();
        q.ge(AlertEvent::getFiredAt, since).orderByAsc(AlertEvent::getFiredAt);
        List<AlertEvent> events = alertEventMapper.selectList(q);

        if (events.isEmpty()) {
            return defaultSlaMetrics(days);
        }

        // 总告警数
        int total = events.size();

        // 活跃告警 (firing / acked)
        long active = events.stream()
                .filter(e -> "firing".equals(e.getStatus()) || "acked".equals(e.getStatus()))
                .count();

        // 已恢复告警
        long resolved = events.stream()
                .filter(e -> "resolved".equals(e.getStatus()))
                .count();

        // MTTR: 已恢复告警的平均持续时间
        double mttrMinutes = events.stream()
                .filter(e -> "resolved".equals(e.getStatus()) && e.getFiredAt() != null && e.getResolvedAt() != null)
                .mapToLong(e -> Duration.between(e.getFiredAt(), e.getResolvedAt()).toMinutes())
                .average()
                .orElse(0.0);

        // MTBF: 两次告警之间的平均间隔
        // = 窗口总时长 / (恢复的告警数 - 1)
        long totalWindowMs = Duration.between(since, now).toMillis();
        double mtbfHours = resolved > 1
                ? (totalWindowMs / 1000.0 / 3600.0) / (resolved - 1)
                : (totalWindowMs / 1000.0 / 3600.0);

        // 可用率估算: 可用率 ≈ 1 - (MTTR * 总告警数 / 窗口总时长)
        double totalWindowMinutes = totalWindowMs / 1000.0 / 60.0;
        double uptimeRatio = totalWindowMinutes > 0
                ? 1.0 - (mttrMinutes * total / totalWindowMinutes)
                : 1.0;
        uptimeRatio = Math.max(0.0, Math.min(1.0, uptimeRatio));
        double availabilityPct = uptimeRatio * 100.0;

        // 按严重程度统计
        long critical = events.stream().filter(e -> "CRITICAL".equals(e.getSeverity())).count();
        long warning = events.stream().filter(e -> "WARNING".equals(e.getSeverity())).count();
        long info = events.stream().filter(e -> "INFO".equals(e.getSeverity())).count();

        return Map.ofEntries(
                Map.entry("windowDays", days),
                Map.entry("since", since.toString()),
                Map.entry("totalAlerts", total),
                Map.entry("activeAlerts", active),
                Map.entry("resolvedAlerts", resolved),
                Map.entry("mtbfHours", Math.round(mtbfHours * 100.0) / 100.0),
                Map.entry("mttrMinutes", Math.round(mttrMinutes * 100.0) / 100.0),
                Map.entry("availabilityPct", Math.round(availabilityPct * 10000.0) / 10000.0),
                Map.entry("slaGrade", slaGrade(availabilityPct)),
                Map.entry("bySeverity", Map.of("CRITICAL", critical, "WARNING", warning, "INFO", info)),
                Map.entry("generatedAt", now.toString())
        );
    }

    private Map<String, Object> defaultSlaMetrics(int days) {
        return Map.of(
                "windowDays", days,
                "totalAlerts", 0,
                "activeAlerts", 0,
                "resolvedAlerts", 0,
                "mtbfHours", 0.0,
                "mttrMinutes", 0.0,
                "availabilityPct", 100.0,
                "slaGrade", "A+",
                "bySeverity", Map.of("CRITICAL", 0L, "WARNING", 0L, "INFO", 0L),
                "generatedAt", LocalDateTime.now().toString()
        );
    }

    /** SLA 等级 */
    private String slaGrade(double availabilityPct) {
        if (availabilityPct >= 99.9) return "A+";
        if (availabilityPct >= 99.5) return "A";
        if (availabilityPct >= 99.0) return "B";
        if (availabilityPct >= 95.0) return "C";
        if (availabilityPct >= 90.0) return "D";
        return "F";
    }
}
