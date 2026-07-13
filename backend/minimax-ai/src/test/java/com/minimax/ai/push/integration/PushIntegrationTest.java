package com.minimax.ai.push.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 推送真实集成 (V3.5.1) 单元测试
 */
class PushIntegrationTest {

    /**
     * 测试 1: VAPID 密钥对生成
     */
    @Test
    @DisplayName("1. VAPID 密钥对生成 (P-256)")
    void testVapidGenerate() {
        VapidUtil.VapidKeyPair kp = VapidUtil.generateKeyPair();
        assertNotNull(kp.publicKeyBase64Url());
        assertNotNull(kp.privateKeyBase64Url());
        // 公钥 uncompressed 65 字节, base64 约 88 字符
        assertEquals(87, kp.publicKeyBase64Url().length(), 1);
        // 私钥 32 字节, base64 约 43 字符
        assertEquals(43, kp.privateKeyBase64Url().length(), 1);
        // 2 次生成应不同
        VapidUtil.VapidKeyPair kp2 = VapidUtil.generateKeyPair();
        assertNotEquals(kp.publicKeyBase64Url(), kp2.publicKeyBase64Url());
    }

    /**
     * 测试 2: VAPID JWT 生成
     */
    @Test
    @DisplayName("2. VAPID JWT 格式 (header.payload.signature)")
    void testVapidJwt() {
        VapidUtil.VapidKeyPair kp = VapidUtil.generateKeyPair();
        String jwt = VapidUtil.createJwt("https://fcm.googleapis.com",
                "mailto:admin@minimax.ai",
                System.currentTimeMillis() / 1000 + 3600,
                kp);
        String[] parts = jwt.split("\\.");
        assertEquals(3, parts.length, "JWT 应有 3 段");
        // 3 段都非空
        for (String p : parts) assertFalse(p.isEmpty());
    }

    /**
     * 测试 3: Web Push 推送 (沙箱)
     */
    @Test
    @DisplayName("3. Web Push 推送 (沙箱模式)")
    void testWebPushSandbox() {
        WebPushProvider p = new WebPushProvider("mailto:admin@minimax.ai", true);
        PushRequest req = PushRequest.builder()
                .target("https://fcm.googleapis.com/wp/abc123")
                .title("测试")
                .body("Hello Web Push")
                .priority(PushRequest.Priority.HIGH)
                .ttlSeconds(60)
                .build();
        PushResult r = p.push(req);
        assertTrue(r.isSuccess());
        assertEquals(PushResult.Platform.WEB_PUSH, r.getPlatform());
        assertNotNull(r.getMessageId());
    }

    /**
     * 测试 4: Web Push 拒绝非法 endpoint
     */
    @Test
    @DisplayName("4. Web Push 拒绝非 https")
    void testWebPushInvalidEndpoint() {
        WebPushProvider p = new WebPushProvider("mailto:admin@minimax.ai", true);
        PushRequest req = PushRequest.builder()
                .target("http://insecure.com/abc")
                .title("测试")
                .build();
        PushResult r = p.push(req);
        assertFalse(r.isSuccess());
        assertEquals("INVALID_TARGET", r.getErrorCode());
    }

    /**
     * 测试 5: APNs 推送 (沙箱)
     */
    @Test
    @DisplayName("5. APNs 推送 (沙箱模式)")
    void testApnsSandbox() {
        ApnsProvider p = new ApnsProvider("K1234567", "T12345", "com.minimax.app", true);
        // 64 位 hex 模拟 device token
        String deviceToken = "abcdef0123456789".repeat(4);  // 64 chars
        PushRequest req = PushRequest.builder()
                .target(deviceToken)
                .title("iOS 推送")
                .body("Hello APNs")
                .priority(PushRequest.Priority.HIGH)
                .build();
        PushResult r = p.push(req);
        assertTrue(r.isSuccess());
        assertEquals(PushResult.Platform.APNS, r.getPlatform());
    }

    /**
     * 测试 6: APNs 拒绝短 token
     */
    @Test
    @DisplayName("6. APNs 拒绝过短 token")
    void testApnsShortToken() {
        ApnsProvider p = new ApnsProvider("K1234567", "T12345", "com.minimax.app", true);
        PushRequest req = PushRequest.builder().target("short").build();
        PushResult r = p.push(req);
        assertFalse(r.isSuccess());
        assertEquals("INVALID_TOKEN", r.getErrorCode());
    }

    /**
     * 测试 7: FCM 推送 (沙箱)
     */
    @Test
    @DisplayName("7. FCM 推送 (沙箱模式)")
    void testFcmSandbox() {
        FcmProvider p = new FcmProvider("minimax-prod", null, true);
        PushRequest req = PushRequest.builder()
                .target("fcm-token-12345")
                .title("Android 推送")
                .body("Hello FCM")
                .priority(PushRequest.Priority.NORMAL)
                .data(Map.of("orderId", "12345"))
                .build();
        PushResult r = p.push(req);
        assertTrue(r.isSuccess());
        assertEquals(PushResult.Platform.FCM, r.getPlatform());
    }

    /**
     * 测试 8: 平台自动检测
     */
    @Test
    @DisplayName("8. 平台自动检测 (https / 64hex / 默认)")
    void testPlatformDetection() {
        PushIntegrationService svc = new PushIntegrationService(true);
        assertEquals(PushResult.Platform.WEB_PUSH,
                svc.detectPlatform("https://fcm.googleapis.com/wp/abc"));
        assertEquals(PushResult.Platform.APNS,
                svc.detectPlatform("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));
        assertEquals(PushResult.Platform.FCM, svc.detectPlatform("fcm-token-xyz"));
        assertEquals(PushResult.Platform.FCM, svc.detectPlatform(null));
    }

    /**
     * 测试 9: 自动推送分发
     */
    @Test
    @DisplayName("9. 自动推送分发 (按 target 类型)")
    void testAutoDispatch() {
        PushIntegrationService svc = new PushIntegrationService(true);
        PushRequest web = PushRequest.builder()
                .target("https://fcm.googleapis.com/wp/abc")
                .title("测试").build();
        PushResult r1 = svc.pushAuto(web);
        assertEquals(PushResult.Platform.WEB_PUSH, r1.getPlatform());
        assertTrue(r1.isSuccess());

        PushRequest apns = PushRequest.builder()
                .target("a".repeat(64))
                .title("测试").build();
        PushResult r2 = svc.pushAuto(apns);
        assertEquals(PushResult.Platform.APNS, r2.getPlatform());
    }

    /**
     * 测试 10: 统计
     */
    @Test
    @DisplayName("10. 推送统计 (总数/成功/失败)")
    void testStats() {
        PushIntegrationService svc = new PushIntegrationService(true);
        svc.pushAuto(PushRequest.builder().target("https://test.com").title("x").build());
        svc.pushAuto(PushRequest.builder().target("a".repeat(64)).title("x").build());
        svc.pushAuto(PushRequest.builder().target("fcm-token").title("x").build());
        Map<String, Object> stat = svc.getStats().snapshot();
        assertEquals(3L, stat.get("total"));
        assertEquals(3L, stat.get("success"));
        assertEquals(0L, stat.get("failed"));
        assertNotNull(stat.get("byPlatform"));
    }

    /**
     * 测试 11: 健康检查
     */
    @Test
    @DisplayName("11. 健康检查 (3 Provider)")
    void testHealthCheck() {
        PushIntegrationService svc = new PushIntegrationService(true);
        Map<PushResult.Platform, Boolean> h = svc.healthCheck();
        assertEquals(3, h.size());
        assertTrue(h.get(PushResult.Platform.WEB_PUSH));
        assertTrue(h.get(PushResult.Platform.APNS));
        assertTrue(h.get(PushResult.Platform.FCM));
    }

    /**
     * 测试 12: Stats 多种状态
     */
    @Test
    @DisplayName("12. Stats 重置")
    void testStatsReset() {
        PushIntegrationService svc = new PushIntegrationService(true);
        svc.pushAuto(PushRequest.builder().target("https://x.com").title("t").build());
        svc.getStats().reset();
        Map<String, Object> stat = svc.getStats().snapshot();
        assertEquals(0L, stat.get("total"));
    }
}
