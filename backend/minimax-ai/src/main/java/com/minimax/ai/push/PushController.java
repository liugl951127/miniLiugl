package com.minimax.ai.push;

import com.minimax.ai.entity.PushMessage;
import com.minimax.ai.entity.PushSubscription;
import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 移动推送 REST API (V3.3.1)
 *
 * <p>API 列表 (统一 /api/v1/ai/push 前缀):
 * <ul>
 *   <li>POST /subscribe             注册订阅 (Web Push / iOS / Android)</li>
 *   <li>POST /unsubscribe           取消订阅</li>
 *   <li>GET  /subscriptions         查用户订阅</li>
 *   <li>GET  /subscriptions/all     查所有 ACTIVE</li>
 *   <li>POST /send/user             发送给指定用户</li>
 *   <li>POST /send/platform         发送给平台 (广播)</li>
 *   <li>POST /send/broadcast        全部广播</li>
 *   <li>GET  /messages              最近消息</li>
 *   <li>GET  /stats                 推送统计</li>
 * </ul>
 */
@Tag(name = "移动推送")
@RestController
@RequestMapping("/api/v1/ai/push")
@RequiredArgsConstructor
public class PushController {

    private final PushService service;

    /**
     * 注册订阅
     */
    @Operation(summary = "注册订阅")
    @PostMapping("/subscribe")
    public Result<PushSubscription> subscribe(@RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") == null ? null : ((Number) body.get("userId")).longValue();
        String platform = (String) body.getOrDefault("platform", "web");
        String endpoint = (String) body.get("endpoint");
        String p256dh = (String) body.get("p256dhKey");
        String auth = (String) body.get("authKey");
        String userAgent = (String) body.getOrDefault("userAgent", "");
        if (endpoint == null) return Result.fail(400, "endpoint 不能为空");
        return Result.ok(service.register(userId, platform, endpoint, p256dh, auth, userAgent));
    }

    /**
     * 取消订阅
     */
    @Operation(summary = "取消订阅")
    @PostMapping("/unsubscribe")
    public Result<Void> unsubscribe(@RequestBody Map<String, String> body) {
        String subscriptionId = body.get("subscriptionId");
        if (subscriptionId == null) return Result.fail(400, "subscriptionId 不能为空");
        boolean ok = service.unsubscribe(subscriptionId);
        return ok ? Result.ok() : Result.fail(404, "订阅不存在");
    }

    /**
     * 查用户订阅
     */
    @Operation(summary = "查用户订阅")
    @GetMapping("/subscriptions")
    public Result<List<PushSubscription>> listByUser(@RequestParam Long userId) {
        return Result.ok(service.listByUser(userId));
    }

    /**
     * 查所有 ACTIVE 订阅
     */
    @Operation(summary = "查所有 ACTIVE 订阅")
    @GetMapping("/subscriptions/all")
    public Result<List<PushSubscription>> listAll() {
        return Result.ok(service.listAllActive());
    }

    /**
     * 发送: 用户
     */
    @Operation(summary = "发送给指定用户")
    @PostMapping("/send/user")
    public Result<PushMessage> sendUser(@RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") == null ? null : ((Number) body.get("userId")).longValue();
        String title = (String) body.get("title");
        String message = (String) body.get("body");
        String clickAction = (String) body.get("clickAction");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        if (userId == null || title == null) return Result.fail(400, "userId/title 必填");
        return Result.ok(service.sendToUser(userId, title, message, clickAction, data));
    }

    /**
     * 发送: 平台
     */
    @Operation(summary = "发送给平台 (广播)")
    @PostMapping("/send/platform")
    public Result<PushMessage> sendPlatform(@RequestBody Map<String, Object> body) {
        String platform = (String) body.get("platform");
        String title = (String) body.get("title");
        String message = (String) body.get("body");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        if (platform == null || title == null) return Result.fail(400, "platform/title 必填");
        return Result.ok(service.sendToPlatform(platform, title, message, null, data));
    }

    /**
     * 全部广播
     */
    @Operation(summary = "全部广播")
    @PostMapping("/send/broadcast")
    public Result<PushMessage> broadcast(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String message = (String) body.get("body");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        if (title == null) return Result.fail(400, "title 必填");
        return Result.ok(service.broadcast(title, message, null, data));
    }

    /**
     * 最近消息
     */
    @Operation(summary = "最近消息")
    @GetMapping("/messages")
    public Result<List<PushMessage>> recentMessages(@RequestParam(defaultValue = "20") int limit) {
        return Result.ok(service.recentMessages(limit));
    }

    /**
     * 推送统计
     */
    @Operation(summary = "推送统计")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(service.stats());
    }
}
