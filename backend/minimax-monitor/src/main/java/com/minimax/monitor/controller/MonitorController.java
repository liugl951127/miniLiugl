package com.minimax.monitor.controller;

import com.minimax.common.result.Result;
import com.minimax.monitor.alert.AlertEngine;
import com.minimax.monitor.alert.AlertNotifierManager;
import com.minimax.monitor.config.AlertStreamRegistry;
import com.minimax.monitor.client.ServiceEndpoints;
import com.minimax.monitor.collector.MetricsCollector;
import com.minimax.monitor.entity.AlertChannel;
import com.minimax.monitor.entity.AlertEvent;
import com.minimax.monitor.mapper.AlertEventMapper;
import com.minimax.monitor.entity.AlertRule;
import com.minimax.monitor.entity.MetricSnapshot;
import com.minimax.monitor.health.HealthDetailService;
import com.minimax.monitor.service.AlertChannelService;
import com.minimax.monitor.service.SnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    // ---------- V5.33 告警通知渠道 CRUD ----------

    @Operation(summary = "列出告警通知渠道 (V5.33)")
    @GetMapping("/alerts/channels")
    public Result<List<AlertChannel>> listChannels() {
        return Result.ok(alertChannelService.list());
    }

    @Operation(summary = "获取告警渠道详情 (V5.33)")
    @GetMapping("/alerts/channels/{id}")
    public Result<AlertChannel> getChannel(@PathVariable Long id) {
        return Result.ok(alertChannelService.getById(id));
    }

    @Operation(summary = "创建告警渠道 (V5.33)")
    @PostMapping("/alerts/channels")
    public Result<AlertChannel> createChannel(@RequestBody AlertChannel ch) {
        return Result.ok(alertChannelService.create(ch));
    }

    @Operation(summary = "更新告警渠道 (V5.33)")
    @PutMapping("/alerts/channels/{id}")
    public Result<AlertChannel> updateChannel(@PathVariable Long id, @RequestBody AlertChannel patch) {
        return Result.ok(alertChannelService.update(id, patch));
    }

    @Operation(summary = "删除告警渠道 (V5.33)")
    @DeleteMapping("/alerts/channels/{id}")
    public Result<Void> deleteChannel(@PathVariable Long id) {
        alertChannelService.delete(id);
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
     * 确认告警 (标记已处理)
     *
     * @param id 告警 ID
     * @return 成功状态
     */
    @Operation(summary = "确认告警")
    @PostMapping("/alerts/{id}/ack")
    public Result<Boolean> acknowledgeAlert(@PathVariable Long id) {
        log.info("[monitor] acknowledge alert id={}", id);
        // Day 28: 真正写库
        AlertEvent e = alertEventMapper.selectById(id);
        if (e == null) {
            return Result.fail("告警事件不存在: " + id);
        }
        e.setStatus("acked");
        e.setAckedAt(java.time.LocalDateTime.now());
        alertEventMapper.updateById(e);
        log.info("[monitor] alert {} acked", id);
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