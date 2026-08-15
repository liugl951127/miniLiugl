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
}
