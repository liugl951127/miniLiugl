package com.minimax.ai.webhook;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService service;

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateRequest req) {
        try {
            Webhook wh = service.create(req.getName(), req.getDescription(),
                req.getUrl(), req.getEvents(), req.getOwnerId());
            return ResponseEntity.ok(Map.of("code", 0, "data", wh, "message", "创建成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(@RequestParam(required = false) Long ownerId) {
        return ResponseEntity.ok(Map.of("code", 0, "data", service.list(ownerId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable String id) {
        Webhook wh = service.detail(id);
        if (wh == null) return ResponseEntity.ok(Map.of("code", 404, "message", "不存在"));
        return ResponseEntity.ok(Map.of("code", 0, "data", wh));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String id, @RequestBody Map<String, Object> body) {
        boolean ok = service.update(id, body);
        return ResponseEntity.ok(Map.of("code", ok ? 0 : 404, "message", ok ? "已更新" : "不存在"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String id) {
        boolean ok = service.delete(id);
        return ResponseEntity.ok(Map.of("code", ok ? 0 : 404));
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> test(@PathVariable String id) {
        WebhookDelivery delivery = service.test(id);
        if (delivery == null) return ResponseEntity.ok(Map.of("code", 404, "message", "不存在"));
        return ResponseEntity.ok(Map.of("code", 0, "data", delivery));
    }

    @GetMapping("/{id}/deliveries")
    public ResponseEntity<Map<String, Object>> deliveries(
            @PathVariable String id,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(Map.of("code", 0, "data", service.deliveries(id, limit)));
    }

    @GetMapping("/deliveries")
    public ResponseEntity<Map<String, Object>> recentDeliveries(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(Map.of("code", 0, "data", service.recentDeliveries(limit)));
    }

    @GetMapping("/events")
    public ResponseEntity<Map<String, Object>> eventTypes() {
        return ResponseEntity.ok(Map.of("code", 0, "data", List.of(
            "USER_LOGIN", "USER_REGISTER",
            "MODEL_TRAINED", "AGENT_PUBLISHED",
            "COLLAB_MESSAGE", "AUDIT_FAILED", "ALERT_TRIGGERED",
            "WEBHOOK_TEST"
        )));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(Map.of("code", 0, "data", service.eventStats()));
    }

    // 事件发布 (供其他模块调用, 简化)
    @PostMapping("/publish")
    public ResponseEntity<Map<String, Object>> publish(@RequestBody Map<String, Object> body) {
        String eventType = (String) body.get("eventType");
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) body.getOrDefault("payload", new java.util.HashMap<>());
        service.publish(eventType, payload);
        return ResponseEntity.ok(Map.of("code", 0, "message", "已发布"));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String name;
        private String description;
        private String url;
        private String events;
        private Long ownerId;
    }
}
