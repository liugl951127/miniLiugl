package com.minimax.monitor.alert;

import com.minimax.monitor.collector.MetricsCollector;
import com.minimax.monitor.config.AlertStreamRegistry;
import com.minimax.monitor.entity.AlertEvent;
import com.minimax.monitor.entity.AlertRule;
import com.minimax.monitor.mapper.AlertEventMapper;
import com.minimax.monitor.mapper.AlertRuleMapper;
import com.minimax.monitor.service.AlertRcaService;
import com.minimax.monitor.service.AlertRcaService.RcaResult;
import com.minimax.monitor.service.LogAnomalyDetector;
import com.minimax.monitor.service.LogAnomalyDetector.AnomalyResult;
import com.minimax.monitor.service.LogAnomalyDetector.AnomalyLevel;
import com.minimax.monitor.service.SnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警引擎。
 *
 * 1) 加载启用的规则
 * 2) 对每条规则取最新指标值
 * 3) 比对阈值, 触发 → 写 alert_event (考虑冷却)
 * 4) 解决: 指标恢复后状态变为 resolved
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEngine {

    private final AlertRuleMapper ruleMapper;
    private final AlertEventMapper eventMapper;
    private final SnapshotService snapshotService;
    private final MetricsCollector collector;
    private final AlertNotifierManager notifierManager;
    private final AlertStreamRegistry streamRegistry;
    private final AlertRcaService rcaService;
    private final LogAnomalyDetector anomalyDetector;

    /** 每 30s 检查一次 */
    @Scheduled(fixedDelay = 30_000, initialDelay = 15_000)
    public void evaluate() {
        try {
            List<AlertRule> rules = ruleMapper.selectEnabled();
            for (AlertRule r : rules) {
                evaluateRule(r);
            }
        } catch (Exception e) {
            log.warn("alert eval fail: {}", e.getMessage());
        }
    }

    public void evaluateRule(AlertRule r) {
        // Day 35: 规则级静默检查
        if (r.getSilencedUntil() != null && r.getSilencedUntil().isAfter(LocalDateTime.now())) {
            log.debug("[Alert] rule {} is silenced until {}, skip evaluation", r.getName(), r.getSilencedUntil());
            return;
        }

        Double v = readMetric(r.getMetricName(), r.getService());
        if (v == null) return;

        // Day 31: 异常检测 — 指标值送入 LogAnomalyDetector
        // 如果 anomaly score >= WARNING，额外触发一条 anomaly 事件
        if (anomalyDetector.isEnabled()) {
            try {
                AnomalyResult ar = anomalyDetector.detect(r.getMetricName(), v, r.getService());
                if (ar.needsAlert()) {
                    fireAnomalyAlert(r, v, ar);
                }
            } catch (Exception anomEx) {
                log.debug("[Anomaly] {} detection error: {}", r.getMetricName(), anomEx.getMessage());
            }
        }

        boolean trigger = compare(v, r.getOperator(), r.getThreshold().doubleValue());

        AlertEvent latest = eventMapper.selectLatestByRule(r.getId());
        if (trigger) {
            // 是否在冷却期
            if (latest != null && "firing".equals(latest.getStatus())
                    && latest.getFiredAt() != null
                    && latest.getFiredAt().isAfter(LocalDateTime.now().minusMinutes(r.getCooldownMinutes()))) {
                return; // 还在冷却
            }
            // 新事件
            AlertEvent e = new AlertEvent();
            e.setRuleId(r.getId());
            e.setRuleName(r.getName());
            e.setSeverity(r.getSeverity());
            e.setMetricName(r.getMetricName());
            e.setMetricValue(BigDecimal.valueOf(v));
            e.setThreshold(r.getThreshold());
            e.setStatus("firing");
            e.setMessage(String.format("%s: %s %s %s (current=%.2f)",
                    r.getName(), r.getMetricName(), r.getOperator(), r.getThreshold(), v));
            eventMapper.insert(e);
            log.warn("ALERT FIRED: {}", e.getMessage());

            // Day 31: 自动触发 RCA 根因分析
            try {
                RcaResult rca = rcaService.analyze(e);
                if (rca != null && rca.isAnalyzed()) {
                    log.info("[RCA] alertId={} category={} cause='{}' actions={}ms (method={})",
                            e.getId(), rca.getCategory(), truncate(rca.getCause(), 100),
                            rca.getAnalysisMs(), rca.getMethod());
                    // 将 RCA 原因追加到告警消息（方便前端展示）
                    if (e.getMessage() != null && rca.getCause() != null) {
                        String enhancedMsg = e.getMessage() + " | RCA: " + truncate(rca.getCause(), 200);
                        e.setMessage(enhancedMsg);
                        eventMapper.updateById(e);
                    }
                } else if (rca != null && rca.getError() != null) {
                    log.warn("[RCA] alertId={} skipped: {}", e.getId(), rca.getError());
                }
            } catch (Exception rcaEx) {
                log.warn("[RCA] alertId={} analysis error: {}", e.getId(), rcaEx.getMessage());
            }
            // V5.33: 触发所有通知渠道 (邮件/钉钉)
            try {
                notifierManager.notifyAll(e);
            } catch (Exception ex) {
                log.warn("alert notification error: {}", ex.getMessage());
            }
            // Day 27: 实时推送给在线前端 (SSE)
            try {
                streamRegistry.broadcast(e);
            } catch (Exception ex) {
                log.debug("alert stream error: {}", ex.getMessage());
            }
        } else {
            // 指标恢复, 解决 firing 事件
            if (latest != null && "firing".equals(latest.getStatus())) {
                latest.setStatus("resolved");
                latest.setResolvedAt(LocalDateTime.now());
                eventMapper.updateById(latest);
                log.info("ALERT RESOLVED: {}", r.getName());
            }
        }
    }

    private Double readMetric(String name, String service) {
        // 优先从 collector 拿实时
        if (service == null || service.isEmpty()) {
            switch (name) {
                case "jvm_heap_usage":   return readJvmHeapPercent();
                case "disk_usage":       return readDiskPercent();
                case "cpu_usage":        return readCpuPercent();
                case "http_5xx_rate":    return collector.getHttp5xx();
                case "chat_messages_total": return collector.getChatMessages();
                case "tool_calls_total": return collector.getToolCalls();
            }
        }
        // 兜底: 查最近一条快照
        try {
            List<Map<String, Object>> agg = snapshotService.trend(name, service, 5);
            if (agg.isEmpty()) return null;
            Object v = agg.get(0).get("avg_val");
            return v == null ? null : ((Number) v).doubleValue();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean compare(double v, String op, double threshold) {
        return switch (op) {
            case ">"  -> v > threshold;
            case ">=" -> v >= threshold;
            case "<"  -> v < threshold;
            case "<=" -> v <= threshold;
            case "="  -> v == threshold;
            case "!=" -> v != threshold;
            default -> false;
        };
    }

    // ---- 系统读数 ----

    private Double readJvmHeapPercent() {
        try {
            long max = Runtime.getRuntime().maxMemory();
            long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            return max > 0 ? (used * 100.0 / max) : null;
        } catch (Exception e) { return null; }
    }

    private Double readDiskPercent() {
        try {
            long total = new java.io.File("/").getTotalSpace();
            long free = new java.io.File("/").getUsableSpace();
            return total > 0 ? ((total - free) * 100.0 / total) : null;
        } catch (Exception e) { return null; }
    }

    private Double readCpuPercent() {
        try {
            com.sun.management.OperatingSystemMXBean os =
                (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            return os.getCpuLoad() * 100;
        } catch (Exception e) { return null; }
    }

    // ---- API ----

    public List<AlertEvent> recentEvents(int limit) {
        if (limit <= 0 || limit > 200) limit = 20;
        return eventMapper.selectRecent(limit);
    }

    public List<AlertEvent> firingEvents(int limit) {
        if (limit <= 0 || limit > 200) limit = 20;
        return eventMapper.selectByStatus("firing", limit);
    }

    public List<AlertRule> rules() {
        return ruleMapper.selectEnabled();
    }

    public Map<String, Object> summary() {
        Map<String, Object> r = new HashMap<>();
        r.put("totalRules", ruleMapper.selectEnabled().size());
        r.put("firingCount", eventMapper.selectByStatus("firing", 1000).size());
        r.put("resolvedCount", eventMapper.selectByStatus("resolved", 1000).size());
        return r;
    }

    // ── V5.9 规则 CRUD ──────────────────────────────────────────────────

    /** 全部规则 (含禁用) */
    public List<AlertRule> allRules() {
        return ruleMapper.selectList(null);  // V5.30.7: AlertRuleMapper 没 selectAll, 用 BaseMapper.selectList(null)
    }

    /** 创建规则 */
    public AlertRule createRule(AlertRule rule) {
        if (rule.getEnabled() == null) rule.setEnabled(1);
        if (rule.getCooldownMinutes() == null) rule.setCooldownMinutes(15);
        if (rule.getSeverity() == null) rule.setSeverity("warning");
        ruleMapper.insert(rule);
        return rule;
    }

    /** 更新规则 */
    public AlertRule updateRule(Long id, AlertRule patch) {
        AlertRule existing = ruleMapper.selectById(id);
        if (existing == null) throw new IllegalArgumentException("rule not found: " + id);
        if (patch.getName() != null) existing.setName(patch.getName());
        if (patch.getDescription() != null) existing.setDescription(patch.getDescription());
        if (patch.getMetricName() != null) existing.setMetricName(patch.getMetricName());
        if (patch.getService() != null) existing.setService(patch.getService());
        if (patch.getOperator() != null) existing.setOperator(patch.getOperator());
        if (patch.getThreshold() != null) existing.setThreshold(patch.getThreshold());
        if (patch.getSeverity() != null) existing.setSeverity(patch.getSeverity());
        if (patch.getEnabled() != null) existing.setEnabled(patch.getEnabled());
        if (patch.getCooldownMinutes() != null) existing.setCooldownMinutes(patch.getCooldownMinutes());
        if (patch.getNotifyChannel() != null) existing.setNotifyChannel(patch.getNotifyChannel());
        ruleMapper.updateById(existing);
        return existing;
    }

    /** 软删除 */
    public void deleteRule(Long id) {
        ruleMapper.deleteById(id);
    }

    // ── 辅助方法 ─────────────────────────────────────────────────────────

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }

    // ── 异常检测告警 ─────────────────────────────────────────────────────

    /**
     * 触发异常检测告警（独立于规则告警）。
     * 与普通规则告警共享冷却机制，防止风暴。
     */
    private void fireAnomalyAlert(AlertRule r, Double value, AnomalyResult ar) {
        // Day 35: 规则级静默检查
        if (r.getSilencedUntil() != null && r.getSilencedUntil().isAfter(LocalDateTime.now())) {
            log.debug("[Anomaly] rule {} is silenced, skip", r.getName());
            return;
        }
        // 复用规则的冷却检测（通过 metricName + service 找最新事件）
        List<AlertEvent> recent = eventMapper.selectRecent(10);
        AlertEvent latestSame = recent.stream()
                .filter(e -> r.getMetricName().equals(e.getMetricName())
                        && (r.getService() == null || r.getService().equals(e.getMetricName())))
                .findFirst().orElse(null);

        // 检查冷却（用 metricName 做 key，5 分钟冷却）
        if (latestSame != null && "firing".equals(latestSame.getStatus())
                && latestSame.getFiredAt() != null
                && latestSame.getFiredAt().isAfter(LocalDateTime.now().minusMinutes(5))) {
            return; // 还在冷却，跳过
        }

        AlertEvent e = new AlertEvent();
        e.setRuleId(r.getId());
        e.setRuleName("ANOMALY:" + r.getName());
        e.setSeverity(ar.getLevel() == AnomalyLevel.CRITICAL ? "critical" : "warning");
        e.setMetricName(r.getMetricName());
        e.setMetricValue(BigDecimal.valueOf(value));
        e.setThreshold(BigDecimal.valueOf(ar.getScore()));
        e.setStatus("firing");
        String msg = String.format("异常检测告警 [%s]: %s=%.4f (score=%.3f) %s",
                ar.getLevel(), r.getMetricName(), value, ar.getScore(), ar.getReason());
        e.setMessage(msg);

        eventMapper.insert(e);
        log.warn("[ANOMALY] fired: {} score={} level={}", msg, ar.getScore(), ar.getLevel());

        // 触发通知 + 推送
        try { notifierManager.notifyAll(e); } catch (Exception ex) { /* already logged elsewhere */ }
        try { streamRegistry.broadcast(e); } catch (Exception ex) { /* already logged elsewhere */ }
    }
}
