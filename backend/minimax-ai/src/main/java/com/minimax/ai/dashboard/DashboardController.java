package com.minimax.ai.dashboard;

import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 数据看板 REST API + SSE 实时 (V3.2.1)
 *
 * <p>API 列表 (统一 /api/v1/ai/dashboard 前缀):
 * <ul>
 *   <li>GET  /metrics/all          全量指标</li>
 *   <li>GET  /metrics/{name}       单个指标 (带 5s 缓存)</li>
 *   <li>POST /metrics              设置指标 (业务注入)</li>
 *   <li>POST /tools/track          累加工具调用</li>
 *   <li>GET  /tools/top            工具 Top N</li>
 *   <li>GET  /trend/{name}         趋势 (hours 参数)</li>
 *   <li>GET  /cache/stats          缓存统计</li>
 *   <li>POST /cache/clear          清缓存</li>
 *   <li>GET  /stream               SSE 实时流 (5s 推一次)</li>
 *   <li>GET  /health               健康检查</li>
 * </ul>
 */
@Tag(name = "数据看板")
@RestController
@RequestMapping("/api/v1/ai/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    /**
     * 全量指标
     */
    @Operation(summary = "全量指标")
    @GetMapping("/metrics/all")
    public Result<Map<String, Object>> all() {
        return Result.ok(service.getAll());
    }

    /**
     * 单个指标
     */
    @Operation(summary = "单个指标 (5s 缓存)")
    @GetMapping("/metrics/{name}")
    public Result<Double> get(@PathVariable String name) {
        return Result.ok(service.getMetric(name));
    }

    /**
     * 设置指标 (业务注入)
     */
    @Operation(summary = "设置指标")
    @PostMapping("/metrics")
    public Result<Void> set(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Object value = body.get("value");
        if (name == null || value == null) return Result.fail(400, "name/value 不能为空");
        long n = ((Number) value).longValue();
        switch (name) {
            case "user.total" -> service.setUserTotal(n);
            case "user.active" -> service.setUserActive(n);
            case "api.requests" -> service.setApiRequests(n);
            case "api.errors" -> service.setApiErrors(n);
            case "cache.hit" -> service.setCacheHit(n);
            case "cache.miss" -> service.setCacheMiss(n);
            case "training.running" -> service.setTrainingRunning(n);
            case "training.completed" -> service.setTrainingCompleted(n);
            default -> { return Result.fail(400, "未知指标: " + name); }
        }
        return Result.ok();
    }

    /**
     * 累加工具调用
     */
    @Operation(summary = "累加工具调用")
    @PostMapping("/tools/track")
    public Result<Void> trackTool(@RequestBody Map<String, String> body) {
        String toolCode = body.get("toolCode");
        if (toolCode == null) return Result.fail(400, "toolCode 不能为空");
        service.incrementToolUsage(toolCode);
        return Result.ok();
    }

    /**
     * 工具 Top N
     */
    @Operation(summary = "工具使用 Top N")
    @GetMapping("/tools/top")
    public Result<List<Map<String, Object>>> toolsTop(@RequestParam(defaultValue = "10") int n) {
        return Result.ok(service.getToolUsageTop(n));
    }

    /**
     * 趋势查询
     */
    @Operation(summary = "趋势 (近 N 小时)")
    @GetMapping("/trend/{name}")
    public Result<List<Map<String, Object>>> trend(@PathVariable String name,
                                                    @RequestParam(defaultValue = "24") int hours) {
        return Result.ok(service.getTrend(name, hours));
    }

    /**
     * 缓存统计
     */
    @Operation(summary = "缓存统计")
    @GetMapping("/cache/stats")
    public Result<Map<String, Object>> cacheStats() {
        return Result.ok(service.getCacheStats());
    }

    /**
     * 清缓存
     */
    @Operation(summary = "清缓存")
    @PostMapping("/cache/clear")
    public Result<Void> clearCache() {
        service.clearCache();
        return Result.ok();
    }

    /**
     * 健康检查
     */
    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.ok(service.health());
    }

    /**
     * SSE 实时流 (5s 推一次)
     */
    @GetMapping(value = "/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        // 1. 长连接 (1 小时)
        SseEmitter emitter = new SseEmitter(3_600_000L);
        // 2. 推 1 条立即
        try {
            emitter.send(SseEmitter.event().name("snapshot").data(service.getAll()));
        } catch (Exception e) {
            emitter.completeWithError(e);
            return emitter;
        }
        // 3. 定时推
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() -> {
                    try {
                        emitter.send(SseEmitter.event().name("snapshot").data(service.getAll()));
                    } catch (Exception ex) {
                        emitter.complete();
                    }
                }, 5, 5, java.util.concurrent.TimeUnit.SECONDS);
        return emitter;
    }
}
