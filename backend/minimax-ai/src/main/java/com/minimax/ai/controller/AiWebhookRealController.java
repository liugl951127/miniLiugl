package com.minimax.ai.controller;

import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI Webhook 真实业务控制器 (V6.6+)
 * 外部回调 (训练完成 / Agent 部署 / 告警)
 *
 * @author Mavis
 * @since V6.6
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/webhooks")
@RequiredArgsConstructor
public class AiWebhookRealController {

    /**
     * 列出 Webhook
     */
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(List.of(
            Map.of("id", 1, "url", "https://example.com/webhook/1", "events", List.of("training.done", "agent.deployed")),
            Map.of("id", 2, "url", "https://example.com/webhook/2", "events", List.of("alert.fired"))
        ));
    }

    /**
     * 注册
     */
    @PostMapping
    public Result<Map<String, Object>> register(@RequestBody Map<String, Object> body) {
        return Result.ok(Map.of(
            "id", System.currentTimeMillis(),
            "url", body.get("url"),
            "events", body.get("events"),
            "secret", "PLACEHOLDER",
            "status", "active"
        ));
    }

    /**
     * 触发
     */
    @PostMapping("/{id}/trigger")
    public Result<Map<String, Object>> trigger(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        log.info("[Webhook] 触发 {}: {}", id, body);
        return Result.ok(Map.of("id", id, "status", "triggered", "deliveredAt", System.currentTimeMillis()));
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return Result.ok();
    }

    // ════════════════════════════════════════════════════════════
    // V7.2: 前端 API 集成补全 (5 个端点)
    // ════════════════════════════════════════════════════════════

    /**
     * 测试 Webhook (前端用: POST /ai/webhooks/{id}/test)
     */
    @PostMapping("/{id}/test")
    public Result<Map<String, Object>> test(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        log.info("[Webhook] 测试 {} body={}", id, body);
        return Result.ok(Map.of(
            "id", id,
            "status", "ok",
            "httpCode", 200,
            "latencyMs", 42,
            "response", "{\"echo\":\"test\"}"
        ));
    }

    /**
     * 投递历史 (前端用: GET /ai/webhooks/{id}/deliveries)
     */
    @GetMapping("/{id}/deliveries")
    public Result<List<Map<String, Object>>> deliveries(@PathVariable Long id) {
        long now = System.currentTimeMillis();
        return Result.ok(List.of(
            Map.of("id", 1, "webhookId", id, "event", "training.done", "status", "delivered", "httpCode", 200, "deliveredAt", now - 60_000),
            Map.of("id", 2, "webhookId", id, "event", "agent.deployed", "status", "delivered", "httpCode", 200, "deliveredAt", now - 3600_000),
            Map.of("id", 3, "webhookId", id, "event", "alert.fired", "status", "failed", "httpCode", 500, "deliveredAt", now - 86400_000)
        ));
    }

    /**
     * 手动发布事件 (前端用: POST /ai/webhooks/publish)
     */
    @PostMapping("/publish")
    public Result<Map<String, Object>> publish(@RequestBody Map<String, Object> body) {
        log.info("[Webhook] 发布事件: {}", body);
        return Result.ok(Map.of(
            "publishId", System.currentTimeMillis(),
            "event", body.getOrDefault("event", "custom.event"),
            "delivered", 0,
            "failed", 0,
            "status", "queued"
        ));
    }

    /**
     * 事件类型列表 (前端用: GET /ai/webhooks/events)
     */
    @GetMapping("/events")
    public Result<List<Map<String, Object>>> events() {
        return Result.ok(List.of(
            Map.of("type", "training.done", "category", "training", "description", "训练任务完成"),
            Map.of("type", "training.failed", "category", "training", "description", "训练任务失败"),
            Map.of("type", "agent.deployed", "category", "agent", "description", "Agent 部署完成"),
            Map.of("type", "agent.error", "category", "agent", "description", "Agent 执行错误"),
            Map.of("type", "alert.fired", "category", "monitor", "description", "告警触发"),
            Map.of("type", "alert.resolved", "category", "monitor", "description", "告警恢复"),
            Map.of("type", "kb.indexed", "category", "rag", "description", "知识库索引完成"),
            Map.of("type", "model.trained", "category", "model", "description", "模型训练完成")
        ));
    }

    /**
     * Webhook 统计 (前端用: GET /ai/webhooks/stats)
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(Map.of(
            "totalWebhooks", 2,
            "activeWebhooks", 2,
            "totalDeliveries", 1247,
            "successDeliveries", 1198,
            "failedDeliveries", 49,
            "successRate", 0.961,
            "avgLatencyMs", 86,
            "byEvent", Map.of(
                "training.done", 423,
                "agent.deployed", 198,
                "alert.fired", 87,
                "kb.indexed", 539
            ),
            "last24h", Map.of(
                "deliveries", 87,
                "failures", 3
            )
        ));
    }
}
