package com.minimax.monitor.controller;

import com.minimax.common.result.Result;
import com.minimax.monitor.alert.AlertEngine;
import com.minimax.monitor.collector.MetricsCollector;
import com.minimax.monitor.entity.AlertEvent;
import com.minimax.monitor.entity.AlertRule;
import com.minimax.monitor.entity.MetricSnapshot;
import com.minimax.monitor.health.HealthDetailService;
import com.minimax.monitor.service.SnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final HealthDetailService health;
    private final MetricsCollector collector;
    private final SnapshotService snapshotService;
    private final AlertEngine alert;

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

    // ---------- V5.9 告警规则 CRUD ----------

    @Operation(summary = "全部告警规则 (含禁用, V5.9)")
    @GetMapping("/alerts/rules/all")
    public Result<List<AlertRule>> allRules() {
        return Result.ok(alert.allRules());
    }

    @Operation(summary = "创建告警规则 (V5.9)")
    @PostMapping("/alerts/rules")
    public Result<AlertRule> createRule(@RequestBody AlertRule rule) {
        return Result.ok(alert.createRule(rule));
    }

    @Operation(summary = "更新告警规则 (V5.9)")
    @PutMapping("/alerts/rules/{id}")
    public Result<AlertRule> updateRule(@PathVariable("id") Long id, @RequestBody AlertRule patch) {
        return Result.ok(alert.updateRule(id, patch));
    }

    @Operation(summary = "删除告警规则 (V5.9)")
    @DeleteMapping("/alerts/rules/{id}")
    public Result<Void> deleteRule(@PathVariable("id") Long id) {
        alert.deleteRule(id);
        return Result.ok();
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
                "GET /actuator/prometheus"
        ));
        return Result.ok(r);
    }
}