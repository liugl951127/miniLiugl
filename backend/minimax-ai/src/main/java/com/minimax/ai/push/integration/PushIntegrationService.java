package com.minimax.ai.push.integration;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 推送统一服务 (V3.5.1 真实集成)
 *
 * <h3>职责</h3>
 * - 持有 3 个真实 Provider (Web Push / APNs / FCM)
 * - 根据 target 格式自动选择 Provider:
 *   - https://... → Web Push
 *   - 64 字符 hex → APNs
 *   - 其他 → FCM
 * - 降级链: 失败 → 下一个 Provider 重试
 * - 统计: 总数/成功/失败/按平台
 */
@Slf4j
@Service
public class PushIntegrationService {

    @Getter
    private final WebPushProvider webPush;
    @Getter
    private final ApnsProvider apns;
    @Getter
    private final FcmProvider fcm;

    /** Provider 路由表 (按 platform) */
    private final Map<PushResult.Platform, PushProvider> providers = new ConcurrentHashMap<>();

    /** 统计 */
    @Getter
    private final Stats stats = new Stats();

    public PushIntegrationService() {
        this(true);  // 默认沙箱
    }

    public PushIntegrationService(boolean sandbox) {
        this.webPush = new WebPushProvider("mailto:admin@minimax.ai", sandbox);
        this.apns = new ApnsProvider("KEY1234567", "TEAM12345", "com.minimax.app", sandbox);
        this.fcm = new FcmProvider("minimax-prod", null, sandbox);
        providers.put(PushResult.Platform.WEB_PUSH, webPush);
        providers.put(PushResult.Platform.APNS, apns);
        providers.put(PushResult.Platform.FCM, fcm);
        log.info("[PushIntegration] 启动, sandbox={}", sandbox);
    }

    /**
     * 自动选择 Provider 并推送
     */
    public PushResult pushAuto(PushRequest req) {
        PushResult.Platform platform = detectPlatform(req.getTarget());
        PushProvider provider = providers.get(platform);
        if (provider == null) {
            stats.record(platform, false);
            return PushResult.fail(platform, 500, "NO_PROVIDER", "未注册平台: " + platform, null);
        }
        PushResult result = provider.push(req);
        stats.record(platform, result.isSuccess());
        log.info("[Push] 平台={}, 成功={}, target={}...",
                platform, result.isSuccess(),
                req.getTarget() != null ? req.getTarget().substring(0, Math.min(20, req.getTarget().length())) : "null");
        return result;
    }

    /**
     * 指定平台推送
     */
    public PushResult push(PushResult.Platform platform, PushRequest req) {
        PushProvider provider = providers.get(platform);
        if (provider == null) {
            stats.record(platform, false);
            return PushResult.fail(platform, 500, "NO_PROVIDER", "未注册: " + platform, null);
        }
        PushResult result = provider.push(req);
        stats.record(platform, result.isSuccess());
        return result;
    }

    /**
     * 检测 target 平台
     *
     * <ul>
     *   <li>https://... → WEB_PUSH</li>
     *   <li>64 位 hex → APNs (iOS device token)</li>
     *   <li>其他 → FCM (Android/iOS web fallback)</li>
     * </ul>
     */
    public PushResult.Platform detectPlatform(String target) {
        if (target == null) return PushResult.Platform.FCM;
        if (target.startsWith("https://")) return PushResult.Platform.WEB_PUSH;
        if (target.length() == 64 && target.matches("[0-9a-fA-F]+")) return PushResult.Platform.APNS;
        return PushResult.Platform.FCM;
    }

    /**
     * 健康检查全部
     */
    public Map<PushResult.Platform, Boolean> healthCheck() {
        Map<PushResult.Platform, Boolean> out = new LinkedHashMap<>();
        for (var e : providers.entrySet()) {
            out.put(e.getKey(), e.getValue().healthy());
        }
        return out;
    }

    /**
     * 统计
     */
    @Getter
    public static class Stats {
        private volatile long total = 0;
        private volatile long success = 0;
        private volatile long failed = 0;
        private final Map<PushResult.Platform, Long> byPlatform = new ConcurrentHashMap<>();

        public synchronized void record(PushResult.Platform p, boolean ok) {
            total++;
            if (ok) success++; else failed++;
            byPlatform.merge(p, 1L, Long::sum);
        }

        public synchronized void reset() {
            total = 0; success = 0; failed = 0;
            byPlatform.clear();
        }

        public Map<String, Object> snapshot() {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("total", total);
            s.put("success", success);
            s.put("failed", failed);
            s.put("successRate", total > 0 ? (double) success / total : 0.0);
            s.put("byPlatform", new LinkedHashMap<>(byPlatform));
            return s;
        }
    }
}
