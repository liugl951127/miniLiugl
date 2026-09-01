package com.minimax.monitor.controller;

import com.minimax.common.result.Result;
import com.minimax.monitor.alert.AlertEngine;
import com.minimax.monitor.alert.AlertNotifierManager;
import com.minimax.monitor.config.AlertStreamRegistry;
import com.minimax.monitor.client.ServiceEndpoints;
import com.minimax.monitor.collector.MetricsCollector;
import com.minimax.monitor.entity.AlertChannel;
import com.minimax.monitor.entity.AlertEvent;
import com.minimax.monitor.entity.AlertRcaKnowledge;
import com.minimax.monitor.mapper.AlertRcaKnowledgeMapper;
import com.minimax.monitor.service.AlertMetricsService;
import com.minimax.monitor.service.AlertPredictionService;
import com.minimax.monitor.mapper.AlertEventMapper;
import com.minimax.monitor.entity.AlertRule;
import com.minimax.monitor.entity.MetricSnapshot;
import com.minimax.monitor.health.HealthDetailService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.minimax.monitor.mapper.AlertRuleMapper;
import com.minimax.monitor.service.AlertChannelService;
import com.minimax.monitor.service.AlertRcaService;
import com.minimax.monitor.service.AlertRcaService.RcaResult;
import com.minimax.monitor.service.AlertRcaKnowledgeService;
import com.minimax.monitor.service.AlertRcaKnowledgeService.KnowledgeEntry;
import com.minimax.monitor.service.LogAnomalyDetector;
import com.minimax.monitor.service.LogAnomalyDetector.AnomalyResult;
import com.minimax.monitor.service.SnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Monitor 控制器 (Day 12 完整版).
 *
 * 健康:
 *   GET  /monitor/health              深度健康
 *   GET  /monitor/health/database     DB
 *   GET  /monitor/health/jvm          JVM
 *   GET  /monitor/health/disk         磁盘
 *
 * 指标:
 *   GET  /monitor/metrics             业务指标 (实时 + 计数)
 *   GET  /monitor/metrics/snapshot    指标快照 (DB)
 *   GET  /monitor/metrics/trend       趋势聚合
 *   POST /monitor/metrics/inc         自助计数
 *
 * 告警:
 *   GET  /monitor/alerts              最近告警
 *   GET  /monitor/alerts/firing       firing
 *   GET  /monitor/alerts/rules        启用规则
 *   GET  /monitor/alerts/summary      告警摘要
 *
 * 实用:
 *   GET  /monitor/info                服务自身信息
 */
@Tag(name = "系统监控")
@RestController
@RequestMapping("/api/v1/monitor")
@RequiredArgsConstructor
@Slf4j
public class MonitorController {

    private final HealthDetailService health;
    private final MetricsCollector collector;
    private final SnapshotService snapshotService;
    private final AlertEngine alert;
    private final ServiceEndpoints endpoints;
    private final AlertChannelService alertChannelService;
    private final AlertNotifierManager notifierManager;
    private final AlertStreamRegistry alertStreamRegistry;
    private final AlertEventMapper alertEventMapper;
    private final AlertRuleMapper ruleMapper;
    private final AlertRcaService rcaService;
    private final AlertRcaKnowledgeMapper rcaKnowledgeMapper;
    private final LogAnomalyDetector anomalyDetector;
    private final AlertMetricsService alertMetricsService;
    private final AlertPredictionService predictionService;
    private final AlertRcaKnowledgeService rcaKnowledgeService;

    // V5.10: Java HttpClient 复用 (跨服务调 /actuator/prometheus)
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // ---------- 健康 ----------

    @Operation(summary = "深度健康检查")
    @GetMapping("/health")
    public Result<Map<String, Object>> deepHealth() {
        return Result.ok(health.deepCheck());
    }

    @Operation(summary = "数据库健康检查")
    @GetMapping("/health/database")
    public Result<Map<String, Object>> db() {
        return Result.ok(health.checkDatabase());
    }

    @Operation(summary = "JVM健康检查")
    @GetMapping("/health/jvm")
    public Result<Map<String, Object>> jvm() {
        return Result.ok(health.checkJvm());
    }

    @Operation(summary = "磁盘健康检查")
    @GetMapping("/health/disk")
    public Result<Map<String, Object>> disk() {
        return Result.ok(health.checkDisk());
    }

    // ---------- 指标 ----------

    @Operation(summary = "获取业务指标")
    @GetMapping("/metrics")
    public Result<Map<String, Object>> metrics() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("chat_messages_total", collector.getChatMessages());
        r.put("tool_calls_total", collector.getToolCalls());
        r.put("rag_queries_total", collector.getRagQueries());
        r.put("llm_tokens_total", collector.getLlmTokens());
        r.put("http_5xx_total", collector.getHttp5xx());
        r.put("http_4xx_total", collector.getHttp4xx());
        r.put("uploads_total", collector.getUploads());
        r.put("active_sessions", collector.getActiveSessions());
        r.put("kb_count", collector.getKbCount());
        r.put("user_count", collector.getUserCount());
        r.put("memory_count", collector.getMemoryCount());
        return Result.ok(r);
    }

    @Operation(summary = "获取指标快照")
    @GetMapping("/metrics/snapshot")
    public Result<List<MetricSnapshot>> snapshot(
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) String service,
            @RequestParam(defaultValue = "60") int sinceMinutes,
            @RequestParam(defaultValue = "100") int limit) {
        return Result.ok(snapshotService.recent(metricName, service, sinceMinutes, limit));
    }

    @Operation(summary = "获取指标趋势")
    @GetMapping("/metrics/trend")
    public Result<List<Map<String, Object>>> trend(
            @RequestParam String metricName,
            @RequestParam(required = false) String service,
            @RequestParam(defaultValue = "60") int sinceMinutes) {
        return Result.ok(snapshotService.trend(metricName, service, sinceMinutes));
    }

    @Operation(summary = "自助计数")
    @PostMapping("/metrics/inc")
    public Result<Void> inc(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Long delta = body.get("delta") == null ? 1L : ((Number) body.get("delta")).longValue();
        if (name == null) return Result.ok();
        switch (name) {
            case "chat_messages" -> collector.incChatMessages(delta);
            case "tool_calls"    -> { for (long i = 0; i < delta; i++) collector.incToolCalls(); }
            case "rag_queries"   -> collector.incRagQueries();
            case "llm_tokens"    -> collector.incLlmTokens(delta);
            case "http_5xx"      -> { for (long i = 0; i < delta; i++) collector.incHttp5xx(); }
            case "http_4xx"      -> { for (long i = 0; i < delta; i++) collector.incHttp4xx(); }
            case "uploads"       -> { for (long i = 0; i < delta; i++) collector.incUploads(); }
            default -> { return Result.ok(); }
        }
        return Result.ok();
    }

    // ---------- 告警 ----------

    @Operation(summary = "最近告警事件")
    @GetMapping("/alerts")
    public Result<List<AlertEvent>> alerts(@RequestParam(defaultValue = "20") int limit) {
        return Result.ok(alert.recentEvents(limit));
    }

    /**
     * 告警详情 (Day 41)
     * 包含告警基本信息 + RCA 分析结果 + 关联日志片段
     */
    @Operation(summary = "告警详情（含 RCA + 日志上下文）")
    @GetMapping("/alerts/{id}")
    public Result<Map<String, Object>> alertDetail(@PathVariable Long id) {
        AlertEvent evt = alertEventMapper.selectById(id);
        if (evt == null) return Result.fail("告警不存在: " + id);

        // RCA 分析（可能耗时，LLM 调用）
        RcaResult rca = null;
        try {
            rca = rcaService.analyzeRca(evt);
        } catch (Exception e) {
            log.warn("RCA 分析失败 alertId={}: {}", id, e.getMessage());
        }

        // 构建详情响应
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", evt.getId());
        detail.put("ruleId", evt.getRuleId());
        detail.put("ruleName", evt.getRuleName());
        detail.put("severity", evt.getSeverity());
        detail.put("metricName", evt.getMetricName());
        detail.put("metricValue", evt.getMetricValue());
        detail.put("threshold", evt.getThreshold());
        detail.put("message", evt.getMessage());
        detail.put("status", evt.getStatus());
        detail.put("firedAt", evt.getFiredAt());
        detail.put("resolvedAt", evt.getResolvedAt());
        detail.put("ackedAt", evt.getAckedAt());
        detail.put("ackedBy", evt.getAckedBy());
        detail.put("notes", evt.getNotes());
        detail.put("duration", evt.getDuration());
        detail.put("silencedUntil", evt.getSilencedUntil());

        // RCA 信息
        if (rca != null) {
            detail.put("rca", Map.of(
                "rootCause", rca.getRootCause() != null ? rca.getRootCause() : "分析中...",
                "category", rca.getCategory() != null ? rca.getCategory() : "UNKNOWN",
                "confidence", rca.getConfidence(),
                "suggestions", rca.getSuggestions() != null ? rca.getSuggestions() : List.of()
            ));
        } else {
            detail.put("rca", Map.of(
                "rootCause", "RCA 分析不可用",
                "category", "UNKNOWN",
                "confidence", 0.0,
                "suggestions", List.of("手动排查该告警")
            ));
        }

        return Result.ok(detail);
    }

    @Operation(summary = "当前触发中的告警")
    @GetMapping("/alerts/firing")
    public Result<List<AlertEvent>> firing(@RequestParam(defaultValue = "20") int limit) {
        return Result.ok(alert.firingEvents(limit));
    }

    @Operation(summary = "启用中的告警规则")
    @GetMapping("/alerts/rules")
    public Result<List<AlertRule>> rules() {
        return Result.ok(alert.rules());
    }

    @Operation(summary = "告警摘要统计")
    @GetMapping("/alerts/summary")
    public Result<Map<String, Object>> alertSummary() {
        return Result.ok(alert.summary());
    }

    // ---------- Day 43: SLA 统计 ----------
    @Operation(summary = "告警 SLA 统计 (MTBF / MTTR / 可用率)")
    @GetMapping("/alerts/sla")
    public Result<Map<String, Object>> slaMetrics(
            @RequestParam(required = false) Integer windowDays) {
        return Result.ok(alertMetricsService.getSlaMetrics(windowDays));
    }

    // ---------- Day 47: 告警统计概览 ----------
    @Operation(summary = "告警统计概览（总数/级别/活跃/Top规则）")
    @GetMapping("/alerts/statistics")
    public Result<Map<String, Object>> alertStatistics(
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        return Result.ok(alertMetricsService.getStatistics(days));
    }

    // ---------- Day 48: 告警时间序列（ECharts 趋势图） ----------
    @Operation(summary = "告警时间序列（按日聚合，适合 ECharts 趋势图）")
    @GetMapping("/alerts/timeseries")
    public Result<List<Map<String, Object>>> alertTimeSeries(
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        return Result.ok(alertMetricsService.getTimeSeries(days));
    }

    // ---------- V5.9 告警规则 CRUD ----------

    @Operation(summary = "全部告警规则 (含禁用, V5.9)")
    @GetMapping("/alerts/rules/all")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Result<List<AlertRule>> allRules() {
        return Result.ok(alert.allRules());
    }

    @Operation(summary = "创建告警规则 (V5.9)")
    @PostMapping("/alerts/rules")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Result<AlertRule> createRule(@RequestBody AlertRule rule) {
        return Result.ok(alert.createRule(rule));
    }

    @Operation(summary = "更新告警规则 (V5.9)")
    @PutMapping("/alerts/rules/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Result<AlertRule> updateRule(@PathVariable("id") Long id, @RequestBody AlertRule patch) {
        return Result.ok(alert.updateRule(id, patch));
    }

    @Operation(summary = "删除告警规则 (V5.9)")
    @DeleteMapping("/alerts/rules/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteRule(@PathVariable("id") Long id) {
        alert.deleteRule(id);
        return Result.ok();
    }

    // ---------- V5.33 告警通知渠道 CRUD ----------

    @Operation(summary = "列出告警通知渠道 (V5.33)")
    @GetMapping("/alerts/channels")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Result<List<AlertChannel>> listChannels() {
        return Result.ok(alertChannelService.list());
    }

    @Operation(summary = "获取告警渠道详情 (V5.33)")
    @GetMapping("/alerts/channels/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Result<AlertChannel> getChannel(@PathVariable Long id) {
        return Result.ok(alertChannelService.getById(id));
    }

    @Operation(summary = "创建告警渠道 (V5.33)")
    @PostMapping("/alerts/channels")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Result<AlertChannel> createChannel(@RequestBody AlertChannel ch) {
        return Result.ok(alertChannelService.create(ch));
    }

    @Operation(summary = "更新告警渠道 (V5.33)")
    @PutMapping("/alerts/channels/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Result<AlertChannel> updateChannel(@PathVariable Long id, @RequestBody AlertChannel patch) {
        return Result.ok(alertChannelService.update(id, patch));
    }

    @Operation(summary = "删除告警渠道 (V5.33)")
    @DeleteMapping("/alerts/channels/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteChannel(@PathVariable Long id) {
        alertChannelService.delete(id);
        return Result.ok();
    }

    // ---------- Day 31: RCA 根因分析 + 异常检测 ----------

    /**
     * 对指定告警事件进行 RCA 根因分析 (Day 31).
     */
    @Operation(summary = "告警 RCA 分析 (Day 31)")
    @PostMapping("/alerts/{id}/rca")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> rcaAnalysis(@PathVariable Long id,
                                                    @RequestBody(required = false) Map<String, Object> body) {
        AlertEvent event = alertEventMapper.selectById(id);
        if (event == null) {
            return Result.fail("告警事件不存在: " + id);
        }
        // 取最近 10 条同类告警做上下文
        List<AlertEvent> recent = alert.recentEvents(10).stream()
                .filter(e -> e.getMetricName() != null
                        && e.getMetricName().equals(event.getMetricName())
                        && e.getId() != id)
                .toList();
        RcaResult rca = rcaService.analyze(event, recent);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("alertId", id);
        resp.put("alert", Map.of(
                "ruleName", event.getRuleName(),
                "metricName", event.getMetricName(),
                "severity", event.getSeverity(),
                "metricValue", event.getMetricValue(),
                "message", event.getMessage()
        ));
        resp.put("rca", Map.of(
                "category", rca.getCategory() != null ? rca.getCategory().name() : null,
                "cause", rca.getCause(),
                "suggestedActions", rca.getSuggestedActions(),
                "analysisMs", rca.getAnalysisMs(),
                "method", rca.getMethod(),
                "confidence", rca.getConfidence(),
                "error", rca.getError()
        ));
        return Result.ok(resp);
    }

    /**
     * Day 57: 知识库条目触发 RCA 分析 — 根据 metricName 查找最近告警并触发 RCA.
     * 用于知识库 Tab 点击「触发 RCA」按钮，联动 RCA 分析。
     */
    @Operation(summary = "知识库条目触发 RCA（按 metricName 触发同类告警根因分析）")
    @GetMapping("/alerts/rca/by-metric")
    public Result<Map<String, Object>> rcaByMetric(
            @RequestParam String metricName,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false, defaultValue = "30") Integer historyDays) {

        // 优先找最近一条 firing 态的同类告警
        AlertEvent event = alertEventMapper.selectLatestByMetric(metricName, severity, "firing");
        // 没有 firing 则找最近一条
        if (event == null) {
            event = alertEventMapper.selectLatestByMetric(metricName, severity, null);
        }
        if (event == null) {
            return Result.fail("未找到指标「" + metricName + "」相关的告警记录");
        }

        // 复用现有 RCA 逻辑
        List<AlertEvent> recent = alertEventMapper.selectAdvanced(
                severity, metricName, null, null, null, 10).stream()
                .filter(e -> e.getId() != null && !e.getId().equals(event.getId()))
                .toList();

        RcaResult rca = rcaService.analyze(event, recent);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("alertId", event.getId());
        resp.put("source", "by-metric");  // 标识来源：知识库触发
        resp.put("alert", Map.of(
                "ruleName", event.getRuleName(),
                "metricName", event.getMetricName(),
                "severity", event.getSeverity(),
                "metricValue", event.getMetricValue(),
                "message", event.getMessage()
        ));
        resp.put("rca", Map.of(
                "category", rca.getCategory() != null ? rca.getCategory().name() : null,
                "cause", rca.getCause(),
                "suggestedActions", rca.getSuggestedActions(),
                "analysisMs", rca.getAnalysisMs(),
                "method", rca.getMethod(),
                "confidence", rca.getConfidence(),
                "error", rca.getError()
        ));
        return Result.ok(resp);
    }

    /**
     * Day 58: RCA 分析结果一键保存为知识条目.
     * 前端 RCA 抽屉点击「保存到知识库」触发。
     */
    @Operation(summary = "RCA 分析结果保存为知识库条目 (Day 58)")
    @PostMapping("/alerts/rca/save-to-knowledge")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> saveRcaToKnowledge(
            @RequestBody Map<String, Object> body) {
        Long alertId = body.get("alertId") != null ? ((Number) body.get("alertId")).longValue() : null;
        if (alertId == null) {
            return Result.fail("alertId 不能为空");
        }
        AlertEvent event = alertEventMapper.selectById(alertId);
        if (event == null) {
            return Result.fail("告警事件不存在: " + alertId);
        }
        // 复用已有 RCA 分析逻辑
        List<AlertEvent> recent = alertEventMapper.selectAdvanced(
                null, event.getMetricName(), null, null, null, 10).stream()
                .filter(e -> e.getId() != null && !e.getId().equals(alertId))
                .toList();
        RcaResult rca = rcaService.analyze(event, recent);
        // 保存人 ID
        Long savedBy = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            try { savedBy = Long.valueOf(auth.getName()); } catch (NumberFormatException ignored) {}
        }
        AlertRcaKnowledge saved = rcaService.saveRcaKnowledge(event, rca, savedBy);
        log.info("[rca-knowledge] 用户 {} 将 alertId={} 的 RCA 保存为知识条目 id={}",
                savedBy, alertId, saved.getId());
        return Result.ok(Map.of(
                "id", saved.getId(),
                "alertId", alertId,
                "metricName", event.getMetricName(),
                "category", saved.getCategory(),
                "createdAt", saved.getCreatedAt()
        ));
    }

    /**
     * Day 58: 查询已保存的 RCA 知识条目列表.
     */
    @Operation(summary = "查询已保存的 RCA 知识条目 (Day 58)")
    @GetMapping("/alerts/rca/knowledge/list")
    public Result<List<Map<String, Object>>> listRcaKnowledge(
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) Long savedBy,
            @RequestParam(required = false, defaultValue = "50") Integer limit) {
        List<AlertRcaKnowledge> entries;
        if (metricName != null && !metricName.isBlank()) {
            entries = rcaKnowledgeMapper.selectByMetricName(metricName, limit);
        } else if (savedBy != null) {
            entries = rcaKnowledgeMapper.selectBySavedBy(savedBy, limit);
        } else {
            entries = rcaKnowledgeMapper.selectList(null).stream().limit(limit).toList();
        }
        List<Map<String, Object>> result = entries.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("alertId", e.getAlertId());
            m.put("metricName", e.getMetricName());
            m.put("ruleName", e.getRuleName());
            m.put("severity", e.getSeverity());
            m.put("category", e.getCategory());
            m.put("cause", e.getCause());
            m.put("confidence", e.getConfidence());
            m.put("method", e.getMethod());
            m.put("createdAt", e.getCreatedAt());
            return m;
        }).toList();
        return Result.ok(result);
    }

    /**
     * 手动触发异常检测 (Day 31).
     */
    @Operation(summary = "手动异常检测 (Day 31)")
    @PostMapping("/anomaly/detect")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Result<AnomalyResult> detectAnomaly(@RequestBody Map<String, Object> body) {
        String metric = (String) body.get("metric");
        Double value = body.get("value") != null ? ((Number) body.get("value")).doubleValue() : null;
        String instanceId = (String) body.get("instanceId");

        if (metric == null || value == null) {
            return Result.fail("metric 和 value 不能为空");
        }
        return Result.ok(anomalyDetector.detect(metric, value, instanceId));
    }

    /**
     * 异常检测指标摘要 (Day 31).
     */
    @Operation(summary = "异常检测摘要 (Day 31)")
    @GetMapping("/anomaly/summary")
    public Result<Map<String, Object>> anomalySummary(
            @RequestParam String metric,
            @RequestParam(required = false) String instanceId) {
        LogAnomalyDetector.MetricSummary summary = anomalyDetector.getSummary(metric, instanceId);
        return Result.ok(Map.of(
                "metric", summary.getMetric(),
                "instanceId", summary.getInstanceId(),
                "sampleCount", summary.getSampleCount(),
                "mean", summary.getMean(),
                "std", summary.getStd(),
                "min", summary.getMinValue(),
                "max", summary.getMaxValue(),
                "currentZScore", summary.getCurrentZScore(),
                "currentlyAnomalous", summary.isCurrentlyAnomalous()
        ));
    }

    /**
     * 异常检测活跃指标列表 (Day 31).
     */
    @Operation(summary = "活跃异常检测指标 (Day 31)")
    @GetMapping("/anomaly/active-metrics")
    public Result<Set<String>> activeAnomalyMetrics() {
        return Result.ok(anomalyDetector.activeMetrics());
    }

    // ---------- 服务信息 ----------

    @Operation(summary = "服务信息")
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("service", "minimax-monitor");
        r.put("version", "1.0.0");
        r.put("port", 8089);
        r.put("endpoints", java.util.List.of(
                "GET /monitor/health", "GET /monitor/metrics", "GET /monitor/alerts",
                "GET /actuator/prometheus", "GET /monitor/forward-prometheus",
                "GET /monitor/api-docs"  // V5.11: API 文档聚合
        ));
        return Result.ok(r);
    }

    // ---------- V5.10 Prometheus 转发 ----------

    /**
     * 跨服务调 /actuator/prometheus 返回文本 (供前端 Metrics Dashboard 使用).
     * <pre>
     *   GET /monitor/forward-prometheus?service=minimax-auth
     *   返回: text/plain (Prometheus 文本格式)
     * </pre>
     */
    // ════════════════════════════════════════════════════════════
    // V3.5.8 新增: 告警管理 (前端 monitor.js 调用)
    // ════════════════════════════════════════════════════════════

    /**
     * 确认告警 (标记已处理, 含备注)
     *
     * @param id 告警 ID
     * @param notes 确认备注 (可选, Day 34)
     * @return 成功状态
     */
    @Operation(summary = "确认告警")
    @PostMapping("/alerts/{id}/ack")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> acknowledgeAlert(@PathVariable Long id,
                                            @RequestBody(required = false) Map<String, String> body) {
        log.info("[monitor] acknowledge alert id={} notes={}", id, body);
        AlertEvent e = alertEventMapper.selectById(id);
        if (e == null) {
            return Result.fail("告警事件不存在: " + id);
        }
        e.setStatus("acked");
        e.setAckedAt(java.time.LocalDateTime.now());
        // Day 34: 从安全上下文拿确认人 ID
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            e.setAckedBy((Long) auth.getPrincipal());
        }
        // Day 34: notes
        if (body != null && body.get("notes") != null) {
            e.setNotes(body.get("notes"));
        }
        alertEventMapper.updateById(e);
        log.info("[monitor] alert {} acked by {}", id, e.getAckedBy());
        return Result.ok(true);
    }

    // ---------- Day 35: 静默功能 ----------

    /**
     * 静默告警事件 (实例级, 防止同一 rule 重复触发).
     * @param id 告警 ID
     * @param body minutes=静默时长(分钟), 默认 60; 或 endTime=截止时间戳(ms)
     */
    @Operation(summary = "静默告警事件 (Day 35)")
    @PostMapping("/alerts/{id}/silence")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> silenceAlert(@PathVariable Long id,
                                        @RequestBody(required = false) Map<String, Object> body) {
        log.info("[monitor] silence alert id={} body={}", id, body);
        AlertEvent e = alertEventMapper.selectById(id);
        if (e == null) {
            return Result.fail("告警事件不存在: " + id);
        }
        LocalDateTime until;
        if (body != null && body.get("endTime") != null) {
            // endTime: 毫秒时间戳
            long ts = ((Number) body.get("endTime")).longValue();
            until = java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(ts),
                    java.time.ZoneId.systemDefault());
        } else {
            int minutes = 60;
            if (body != null && body.get("minutes") != null) {
                minutes = ((Number) body.get("minutes")).intValue();
            }
            until = java.time.LocalDateTime.now().plusMinutes(minutes);
        }
        e.setSilencedUntil(until);
        alertEventMapper.updateById(e);
        log.info("[monitor] alert {} silenced until {}", id, until);
        return Result.ok(true);
    }

    /**
     * 取消静默告警事件.
     */
    @Operation(summary = "取消静默告警事件 (Day 35)")
    @PostMapping("/alerts/{id}/unsilence")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> unsilenceAlert(@PathVariable Long id) {
        log.info("[monitor] unsilence alert id={}", id);
        AlertEvent e = alertEventMapper.selectById(id);
        if (e == null) {
            return Result.fail("告警事件不存在: " + id);
        }
        e.setSilencedUntil(null);
        alertEventMapper.updateById(e);
        return Result.ok(true);
    }

    /**
     * 静默告警规则 (规则级, 期间内该规则所有告警不触发).
     * @param id 规则 ID
     * @param body minutes=静默时长(分钟), 默认 60; 或 endTime=截止时间戳(ms)
     */
    @Operation(summary = "静默告警规则 (Day 35)")
    @PostMapping("/alerts/rules/{id}/silence")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> silenceRule(@PathVariable("id") Long id,
                                        @RequestBody(required = false) Map<String, Object> body) {
        log.info("[monitor] silence rule id={} body={}", id, body);
        AlertRule r = ruleMapper.selectById(id);
        if (r == null) {
            return Result.fail("告警规则不存在: " + id);
        }
        LocalDateTime until;
        if (body != null && body.get("endTime") != null) {
            long ts = ((Number) body.get("endTime")).longValue();
            until = java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(ts),
                    java.time.ZoneId.systemDefault());
        } else {
            int minutes = 60;
            if (body != null && body.get("minutes") != null) {
                minutes = ((Number) body.get("minutes")).intValue();
            }
            until = java.time.LocalDateTime.now().plusMinutes(minutes);
        }
        r.setSilencedUntil(until);
        ruleMapper.updateById(r);
        log.info("[monitor] rule {} silenced until {}", id, until);
        return Result.ok(true);
    }

    /**
     * 取消静默告警规则.
     */
    @Operation(summary = "取消静默告警规则 (Day 35)")
    @PostMapping("/alerts/rules/{id}/unsilence")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> unsilenceRule(@PathVariable("id") Long id) {
        log.info("[monitor] unsilence rule id={}", id);
        AlertRule r = ruleMapper.selectById(id);
        if (r == null) {
            return Result.fail("告警规则不存在: " + id);
        }
        r.setSilencedUntil(null);
        ruleMapper.updateById(r);
        return Result.ok(true);
    }

    /**
     * 测试告警通道 (发送测试消息)
     *
     * @param id 告警通道 ID
     * @return 发送结果
     */
    @Operation(summary = "测试告警通道")
    @PostMapping("/alerts/channels/{id}/test")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> testAlertChannel(@PathVariable Long id) {
        log.info("[monitor] test alert channel id={}", id);
        AlertChannel ch = alertChannelService.getById(id);
        if (ch == null) {
            return Result.fail("渠道不存在: " + id);
        }
        try {
            notifierManager.sendTest(ch);
            return Result.ok(true);
        } catch (Exception e) {
            log.warn("[monitor] test alert channel failed: id={} err={}", id, e.getMessage());
            return Result.fail("测试消息发送失败: " + e.getMessage());
        }
    }

    /**
     * 告警历史趋势 (Day 44) — 按天聚合，用于折线图
     * 返回近 N 天每天各严重级别的告警数量
     */
    @Operation(summary = "告警历史趋势（按天聚合）")
    @GetMapping("/alerts/trend")
    public Result<List<Map<String, Object>>> getAlertTrend(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) String severity) {
        LocalDate since = LocalDate.now().minusDays(days);
        List<AlertEvent> events = alertEventMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AlertEvent>()
                        .ge("fired_at", since.atStartOfDay())
                        .orderByAsc("fired_at"));
        if (severity != null && !severity.isBlank()) {
            events = events.stream().filter(e -> severity.equalsIgnoreCase(e.getSeverity())).toList();
        }
        // 按天分组
        Map<LocalDate, Map<String, Long>> daily = new LinkedHashMap<>();
        for (AlertEvent e : events) {
            LocalDate d = e.getFiredAt().toLocalDate();
            daily.computeIfAbsent(d, k -> {
                Map<String, Long> m = new LinkedHashMap<>();
                m.put("date", (long) d.toEpochDay());
                m.put("CRITICAL", 0L);
                m.put("WARNING", 0L);
                m.put("INFO", 0L);
                m.put("total", 0L);
                return m;
            });
            String sev = e.getSeverity() != null ? e.getSeverity().toUpperCase() : "INFO";
            daily.get(d).merge(sev, 1L, Long::sum);
            daily.get(d).merge("total", 1L, Long::sum);
        }
        List<Map<String, Object>> result = daily.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("date", entry.getKey().toString());
                    m.putAll(entry.getValue());
                    return m;
                })
                .toList();
        return Result.ok(result);
    }

    /**
     * 告警历史 (已恢复的告警)
     */
    @Operation(summary = "告警历史")
    @GetMapping("/alerts/history")
    public Result<List<Map<String, Object>>> getAlertHistory(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "100") int limit) {
        // Day 27: 查真实 alert_event 表
        List<AlertEvent> events = alert.recentEvents(Math.max(limit, 200));
        List<Map<String, Object>> history = events.stream()
                .filter(e -> !"firing".equals(e.getStatus())) // 只返回已解决的
                .limit(limit)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", e.getId());
                    m.put("rule", e.getRuleName());
                    m.put("severity", e.getSeverity());
                    m.put("status", e.getStatus());
                    m.put("message", e.getMessage());
                    m.put("triggeredAt", e.getFiredAt() != null ? e.getFiredAt().toString() : null);
                    m.put("recoveredAt", e.getResolvedAt() != null ? e.getResolvedAt().toString() : null);
                    m.put("ackedAt", e.getAckedAt() != null ? e.getAckedAt().toString() : null);
                    m.put("duration", e.getDuration());
                    return m;
                })
                .toList();
        return Result.ok(history);
    }

    // ---------- Day 54: 告警根因知识库 (同类告警历史处理经验) ----------

    /**
     * 查询告警知识库：同类历史告警的处理经验.
     */
    @Operation(summary = "查询告警知识库（同指标历史处理经验）")
    @GetMapping("/alerts/rca/knowledge")
    public Result<List<Map<String, Object>>> rcaKnowledge(
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false, defaultValue = "30") Integer historyDays,
            @RequestParam(required = false) Integer limit) {
        List<KnowledgeEntry> entries = rcaKnowledgeService.queryKnowledge(metricName, historyDays, limit);
        List<Map<String, Object>> items = entries.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("alertId", e.alertId());
            m.put("metricName", e.metricName());
            m.put("ruleName", e.ruleName());
            m.put("severity", e.severity());
            m.put("status", e.status());
            m.put("message", e.message());
            m.put("firedAt", e.firedAt());
            m.put("resolvedAt", e.resolvedAt());
            m.put("duration", e.duration());
            m.put("notes", e.notes());
            m.put("resolvedBy", e.resolvedBy());
            return m;
        }).toList();
        return Result.ok(items);
    }

    /**
     * 根据当前告警 ID 查找同类历史告警处理经验.
     */
    @Operation(summary = "同类告警查找（根据告警ID找历史处理记录）")
    @GetMapping("/alerts/rca/similar")
    public Result<List<Map<String, Object>>> rcaSimilar(
            @RequestParam Long alertId,
            @RequestParam(required = false, defaultValue = "30") Integer historyDays,
            @RequestParam(required = false) Integer limit) {
        List<KnowledgeEntry> entries = rcaKnowledgeService.findSimilar(alertId, historyDays, limit);
        List<Map<String, Object>> items = entries.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("alertId", e.alertId());
            m.put("metricName", e.metricName());
            m.put("ruleName", e.ruleName());
            m.put("severity", e.severity());
            m.put("status", e.status());
            m.put("message", e.message());
            m.put("firedAt", e.firedAt());
            m.put("resolvedAt", e.resolvedAt());
            m.put("duration", e.duration());
            m.put("notes", e.notes());
            m.put("resolvedBy", e.resolvedBy());
            return m;
        }).toList();
        return Result.ok(items);
    }

    /**
     * 指标告警知识摘要（高频级别/平均恢复时长/常见原因）.
     */
    @Operation(summary = "告警知识摘要（高频级别/平均恢复时长/常见原因）")
    @GetMapping("/alerts/rca/summary")
    public Result<Map<String, Object>> rcaSummary(
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false, defaultValue = "30") Integer historyDays) {
        return Result.ok(rcaKnowledgeService.knowledgeSummary(metricName, historyDays));
    }

    // ---------- Day 53: 告警趋势预测 (EWMA + 线性回归) ----------
    @Operation(summary = "告警趋势预测（指数加权移动平均 + 线性回归 + 风险预警）")
    @GetMapping("/alerts/predict")
    public Result<Map<String, Object>> alertPredict(
            @RequestParam(required = false, defaultValue = "30") Integer historyDays,
            @RequestParam(required = false, defaultValue = "7") Integer forecastDays,
            @RequestParam(required = false) String severity) {
        return Result.ok(predictionService.predict(historyDays, forecastDays, severity));
    }

    @Operation(summary = "按级别预测（CRITICAL / WARNING / INFO 分别预测）")
    @GetMapping("/alerts/predict/by-severity")
    public Result<Map<String, Object>> alertPredictBySeverity(
            @RequestParam(required = false, defaultValue = "30") Integer historyDays,
            @RequestParam(required = false, defaultValue = "7") Integer forecastDays) {
        return Result.ok(predictionService.predictBySeverity(historyDays, forecastDays));
    }

    // ---------- Day 52: 告警历史高级筛选 ----------
    @Operation(summary = "告警历史高级筛选（severity / 指标名 / 状态 / 时间范围 / 分页）")
    @GetMapping("/alerts/history/advanced")
    public Result<Map<String, Object>> getAlertHistoryAdvanced(
            @RequestParam(required = false) String severity,     // CRITICAL / WARNING / INFO
            @RequestParam(required = false) String metricName,   // 指标名模糊匹配
            @RequestParam(required = false) String status,       // firing / acked / resolved
            @RequestParam(required = false) String startTime,  // ISO: 2026-08-01T00:00:00
            @RequestParam(required = false) String endTime,     // ISO: 2026-08-23T23:59:59
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "1") int page) {

        LocalDateTime start = null;
        LocalDateTime end = null;
        try {
            if (startTime != null && !startTime.isBlank()) {
                start = LocalDateTime.parse(startTime);
            }
            if (endTime != null && !endTime.isBlank()) {
                end = LocalDateTime.parse(endTime);
            }
        } catch (Exception ex) {
            log.warn("时间解析失败 startTime={} endTime={}: {}", startTime, endTime, ex.getMessage());
        }

        long total = alertEventMapper.countAdvanced(severity, metricName, status, start, end);
        int safeLimit = Math.max(1, Math.min(limit, 500));
        int offset = Math.max(0, (page - 1)) * safeLimit;

        List<AlertEvent> events = alertEventMapper.selectAdvanced(
                severity, metricName, status, start, end, safeLimit);

        List<Map<String, Object>> items = events.stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", e.getId());
                    m.put("ruleId", e.getRuleId());
                    m.put("ruleName", e.getRuleName());
                    m.put("severity", e.getSeverity());
                    m.put("metricName", e.getMetricName());
                    m.put("metricValue", e.getMetricValue());
                    m.put("threshold", e.getThreshold());
                    m.put("message", e.getMessage());
                    m.put("status", e.getStatus());
                    m.put("firedAt", e.getFiredAt());
                    m.put("resolvedAt", e.getResolvedAt());
                    m.put("ackedAt", e.getAckedAt());
                    m.put("ackedBy", e.getAckedBy());
                    m.put("duration", e.getDuration());
                    m.put("notes", e.getNotes());
                    m.put("escalated", e.getEscalated());
                    m.put("resolvedBy", e.getResolvedBy());
                    return m;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("limit", safeLimit);
        result.put("items", items);
        return Result.ok(result);
    }

    /**
     * 告警实时推送 (SSE) (Day 27).
     * 前端 EventSource 订阅此端点，新告警触发时实时推送。
     */
    @Operation(summary = "告警实时推送 (SSE)")
    @GetMapping(value = "/alerts/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAlerts() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        alertStreamRegistry.register(emitter);
        emitter.onCompletion(() -> alertStreamRegistry.unregister(emitter));
        emitter.onTimeout(() -> alertStreamRegistry.unregister(emitter));
        emitter.onError(e -> alertStreamRegistry.unregister(emitter));
        // 立即发送一条心跳
        try {
            emitter.send(SseEmitter.event()
                    .name("ping")
                    .data("{\"type\":\"connected\",\"msg\":\"alert stream connected\"}"));
        } catch (Exception ignored) {}
        return emitter;
    }

    /**
     * 启停告警规则
     *
     * @param id 规则 ID
     * @return 启停状态
     */
    @Operation(summary = "启停告警规则")
    @PostMapping("/alerts/rules/{id}/toggle")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> toggleAlertRule(@PathVariable Long id, @RequestParam Boolean enabled) {
        log.info("[monitor] toggle rule id={} enabled={}", id, enabled);
        return Result.ok(enabled);
    }

    @Operation(summary = "转发其他服务的 Prometheus 输出 (V5.10)")
    @GetMapping(value = "/forward-prometheus", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> forwardPrometheus(@RequestParam("service") String service) {
        String base = endpoints.resolve(service);
        if (base == null) {
            return ResponseEntity.badRequest().body("# unknown service: " + service);
        }
        String url = base + "/actuator/prometheus";
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(resp.body());
            }
            return ResponseEntity.status(resp.statusCode())
                    .body("# upstream " + service + " status=" + resp.statusCode());
        } catch (Exception ex) {
            log.warn("forward prometheus to {} failed: {}", service, ex.getMessage());
            return ResponseEntity.status(502)
                    .body("# forward failed: " + service + " - " + ex.getMessage());
        }
    }
}