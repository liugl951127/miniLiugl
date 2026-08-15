package com.minimax.ai.push.integration;

import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 推送真实集成 REST API (V3.5.1)
 */
@Tag(name = "推送真实集成 (V3.5.1)")
@RestController
@RequestMapping("/api/v1/ai/push/integration")
@RequiredArgsConstructor
public class PushIntegrationController {

    private final PushIntegrationService service;

    /**
     * 自动检测平台推送
     */
    @Operation(summary = "自动检测平台推送 (Web Push / APNs / FCM)")
    @PostMapping("/auto")
    public Result<PushResult> pushAuto(@RequestBody Map<String, Object> body) {
        PushRequest req = buildRequest(body);
        return Result.ok(service.pushAuto(req));
    }

    /**
     * Web Push
     */
    @Operation(summary = "Web Push (VAPID 签名)")
    @PostMapping("/web")
    public Result<PushResult> pushWeb(@RequestBody Map<String, Object> body) {
        PushRequest req = buildRequest(body);
        return Result.ok(service.push(PushResult.Platform.WEB_PUSH, req));
    }

    /**
     * APNs
     */
    @Operation(summary = "APNs (HTTP/2 + JWT ES256)")
    @PostMapping("/apns")
    public Result<PushResult> pushApns(@RequestBody Map<String, Object> body) {
        PushRequest req = buildRequest(body);
        return Result.ok(service.push(PushResult.Platform.APNS, req));
    }

    /**
     * FCM
     */
    @Operation(summary = "FCM (HTTP v1 + OAuth2)")
    @PostMapping("/fcm")
    public Result<PushResult> pushFcm(@RequestBody Map<String, Object> body) {
        PushRequest req = buildRequest(body);
        return Result.ok(service.push(PushResult.Platform.FCM, req));
    }

    /**
     * 平台检测
     */
    @Operation(summary = "检测 target 所属平台")
    @GetMapping("/detect")
    public Result<PushResult.Platform> detect(@RequestParam String target) {
        return Result.ok(service.detectPlatform(target));
    }

    /**
     * 健康检查
     */
    @Operation(summary = "3 Provider 健康检查")
    @GetMapping("/health")
    public Result<Map<PushResult.Platform, Boolean>> health() {
        return Result.ok(service.healthCheck());
    }

    /**
     * 统计
     */
    @Operation(summary = "推送统计 (总数/成功/失败/按平台)")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(service.getStats().snapshot());
    }

    /**
     * VAPID 公钥 (供前端订阅)
     */
    @Operation(summary = "VAPID 公钥 (前端 subscribe 用)")
    @GetMapping("/vapid-public-key")
    public Result<String> vapidPublicKey() {
        return Result.ok(service.getWebPush().getVapidKeys().publicKeyBase64Url());
    }

    private PushRequest buildRequest(Map<String, Object> body) {
        PushRequest.PushRequestBuilder b = PushRequest.builder()
                .target((String) body.get("target"))
                .title((String) body.get("title"))
                .body((String) body.get("body"));
        if (body.get("icon") != null) b.icon((String) body.get("icon"));
        if (body.get("clickAction") != null) b.clickAction((String) body.get("clickAction"));
        if (body.get("data") != null) {
            @SuppressWarnings("unchecked")
            Map<String, String> data = (Map<String, String>) body.get("data");
            b.data(data);
        }
        if (body.get("priority") != null) {
            b.priority(PushRequest.Priority.valueOf((String) body.get("priority")));
        }
        if (body.get("ttlSeconds") != null) {
            b.ttlSeconds(((Number) body.get("ttlSeconds")).intValue());
        }
        if (body.get("topic") != null) b.topic((String) body.get("topic"));
        return b.build();
    }
}
