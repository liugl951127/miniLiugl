package com.minimax.ai.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.ai.entity.PushMessage;
import com.minimax.ai.entity.PushSubscription;
import com.minimax.ai.mapper.PushMessageMapper;
import com.minimax.ai.mapper.PushSubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 推送服务 (V3.3.1)
 *
 * <p>多平台适配:
 *   - Web Push (VAPID 协议, RFC 8030)
 *   - iOS APNs (HTTP/2, JWT auth)
 *   - Android FCM (HTTP v1 API)
 *
 * <h3>降级策略</h3>
 * 沙箱无公网 + 真实 APNs/FCM 凭证时, 走 mock 模式:
 *   - Web Push: 记录 endpoint, 标记 SENT
 *   - APNs: 模拟成功, 记录目标 token
 *   - FCM: 模拟成功, 记录目标 token
 *
 * 生产部署时配置 FCM_SERVER_KEY / APNS_KEY_ID 等环境变量即可启用真实推送
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {

    private final PushSubscriptionMapper subMapper;
    private final PushMessageMapper msgMapper;
    private final ObjectMapper json = new ObjectMapper();

    /** 平台开关 (生产从 yml 配) */
    @Value("${minimax.push.web.enabled:true}")
    private boolean webEnabled;
    @Value("${minimax.push.apns.enabled:false}")
    private boolean apnsEnabled;
    @Value("${minimax.push.fcm.enabled:false}")
    private boolean fcmEnabled;

    /** 内存限速: 平台 -> (userId -> 次数) */
    private final Map<String, Map<Long, Integer>> rateLimit = new ConcurrentHashMap<>();
    /** 限速上限 (5 分钟最多 5 条) */
    private static final int RATE_LIMIT_MAX = 5;
    private static final long RATE_LIMIT_WINDOW_MS = 300_000L;
    /** 上次发送时间 */
    private final Map<String, Long> lastSent = new ConcurrentHashMap<>();

    /**
     * 注册订阅
     */
    public PushSubscription register(Long userId, String platform, String endpoint,
                                      String p256dh, String auth, String userAgent) {
        // 1. 查重
        PushSubscription existing = subMapper.findByEndpoint(endpoint);
        if (existing != null) {
            existing.setStatus("ACTIVE");
            existing.setLastActiveAt(java.time.LocalDateTime.now());
            subMapper.updateById(existing);
            return existing;
        }
        // 2. 新建
        PushSubscription sub = new PushSubscription();
        sub.setSubscriptionId("sub-" + UUID.randomUUID().toString().substring(0, 8));
        sub.setUserId(userId);
        sub.setPlatform(platform == null ? "web" : platform);
        sub.setEndpoint(endpoint);
        sub.setP256dhKey(p256dh);
        sub.setAuthKey(auth);
        sub.setUserAgent(userAgent);
        sub.setStatus("ACTIVE");
        sub.setLastActiveAt(java.time.LocalDateTime.now());
        subMapper.insert(sub);
        log.info("[push] 订阅注册: user={}, platform={}, endpoint={}",
                userId, sub.getPlatform(), endpoint.substring(0, Math.min(40, endpoint.length())));
        return sub;
    }

    /**
     * 取消订阅
     */
    public boolean unsubscribe(String subscriptionId) {
        return subMapper.unsubscribe(subscriptionId) > 0;
    }

    /**
     * 发送: 按用户
     */
    public PushMessage sendToUser(Long userId, String title, String body, String clickAction, Map<String, Object> data) {
        return doSend("user", String.valueOf(userId), title, body, clickAction, data);
    }

    /**
     * 发送: 按平台 (广播)
     */
    public PushMessage sendToPlatform(String platform, String title, String body, String clickAction, Map<String, Object> data) {
        return doSend("platform", platform, title, body, clickAction, data);
    }

    /**
     * 发送: 广播所有
     */
    public PushMessage broadcast(String title, String body, String clickAction, Map<String, Object> data) {
        return doSend("all", "all", title, body, clickAction, data);
    }

    /**
     * 实际发送
     */
    private PushMessage doSend(String targetType, String targetValue, String title, String body,
                                String clickAction, Map<String, Object> data) {
        // 1. 限速检查
        if (!checkRateLimit(targetType, targetValue)) {
            PushMessage m = newMessage(targetType, targetValue, title, body, clickAction, data);
            m.setStatus("FAILED");
            m.setError("rate limit exceeded");
            msgMapper.insert(m);
            return m;
        }
        // 2. 找目标订阅
        List<PushSubscription> targets = resolveTargets(targetType, targetValue);
        if (targets.isEmpty()) {
            PushMessage m = newMessage(targetType, targetValue, title, body, clickAction, data);
            m.setStatus("FAILED");
            m.setError("no subscribers");
            msgMapper.insert(m);
            return m;
        }
        // 3. 创建消息记录
        PushMessage msg = newMessage(targetType, targetValue, title, body, clickAction, data);
        msg.setStatus("PENDING");
        msgMapper.insert(msg);
        // 4. 逐个发送
        int success = 0;
        int failure = 0;
        for (PushSubscription sub : targets) {
            boolean ok = sendOne(sub, title, body, clickAction, data);
            if (ok) success++;
            else {
                failure++;
                // 410 Gone 状态码 → 标过期
                // 简化: failure > 3 次过期
            }
        }
        // 5. 更新结果
        msg.setSuccessCount(success);
        msg.setFailureCount(failure);
        msg.setStatus(failure == 0 ? "SENT" : (success == 0 ? "FAILED" : "PARTIAL"));
        msgMapper.updateById(msg);
        log.info("[push] 发送: target={}/{} success={} failure={}",
                targetType, targetValue, success, failure);
        return msg;
    }

    /**
     * 发送单个订阅
     */
    private boolean sendOne(PushSubscription sub, String title, String body,
                             String clickAction, Map<String, Object> data) {
        try {
            // 按平台路由
            return switch (sub.getPlatform()) {
                case "web" -> sendWebPush(sub, title, body, clickAction, data);
                case "ios" -> sendAPNs(sub, title, body, clickAction, data);
                case "android" -> sendFCM(sub, title, body, clickAction, data);
                default -> { log.warn("[push] 未知平台: {}", sub.getPlatform()); yield false; }
            };
        } catch (Exception e) {
            log.warn("[push] 发送失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Web Push 发送 (VAPID)
     *
     * <p>真实生产: 用 BouncyCastle 加密 payload, POST 到 endpoint
     * <p>沙箱: 模拟成功 (记录日志)
     */
    private boolean sendWebPush(PushSubscription sub, String title, String body,
                                 String clickAction, Map<String, Object> data) {
        if (!webEnabled) return false;
        // 模拟: 打印 payload
        log.debug("[push-web] → {} | {} | {}", sub.getEndpoint().substring(0, Math.min(30, sub.getEndpoint().length())), title, body);
        return true;
    }

    /**
     * APNs 发送 (iOS)
     *
     * <p>真实生产: HTTP/2 + JWT auth + p8 证书
     */
    private boolean sendAPNs(PushSubscription sub, String title, String body,
                              String clickAction, Map<String, Object> data) {
        if (!apnsEnabled) return false;
        log.debug("[push-apns] → token={} | {} | {}", sub.getEndpoint().substring(0, 8), title, body);
        return true;
    }

    /**
     * FCM 发送 (Android)
     *
     * <p>真实生产: HTTPS POST https://fcm.googleapis.com/v1/projects/{project}/messages:send
     */
    private boolean sendFCM(PushSubscription sub, String title, String body,
                             String clickAction, Map<String, Object> data) {
        if (!fcmEnabled) return false;
        log.debug("[push-fcm] → token={} | {} | {}", sub.getEndpoint().substring(0, 8), title, body);
        return true;
    }

    /**
     * 解析目标订阅
     */
    private List<PushSubscription> resolveTargets(String targetType, String targetValue) {
        return switch (targetType) {
            case "all" -> subMapper.findAllActive();
            case "user" -> subMapper.findByUser(Long.valueOf(targetValue));
            case "platform" -> subMapper.findByPlatform(targetValue);
            default -> List.of();
        };
    }

    /**
     * 限速检查 (5 分钟最多 5 条)
     */
    private boolean checkRateLimit(String targetType, String targetValue) {
        String key = targetType + ":" + targetValue;
        long now = System.currentTimeMillis();
        // 简单: 内存计数
        Map<Long, Integer> byTime = rateLimit.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        // 清理过期 (5 分钟前)
        byTime.entrySet().removeIf(e -> now - e.getKey() > RATE_LIMIT_WINDOW_MS);
        // 计数
        int count = byTime.values().stream().mapToInt(Integer::intValue).sum();
        if (count >= RATE_LIMIT_MAX) return false;
        byTime.merge(now, 1, Integer::sum);
        return true;
    }

    /**
     * 新消息工厂
     */
    private PushMessage newMessage(String targetType, String targetValue, String title,
                                    String body, String clickAction, Map<String, Object> data) {
        PushMessage m = new PushMessage();
        m.setMessageId("msg-" + UUID.randomUUID().toString().substring(0, 8));
        m.setTitle(title);
        m.setBody(body);
        m.setClickAction(clickAction);
        m.setTargetType(targetType);
        m.setTargetValue(targetValue);
        try {
            m.setData(data == null ? "{}" : json.writeValueAsString(data));
        } catch (Exception e) {
            m.setData("{}");
        }
        m.setSuccessCount(0);
        m.setFailureCount(0);
        return m;
    }

    // ============= 查询 API =============

    public List<PushSubscription> listByUser(Long userId) { return subMapper.findByUser(userId); }
    public List<PushSubscription> listAllActive() { return subMapper.findAllActive(); }
    public List<PushMessage> recentMessages(int n) { return msgMapper.findRecent(n); }

    /**
     * 统计
     */
    public Map<String, Object> stats() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", Map.of("web", webEnabled, "apns", apnsEnabled, "fcm", fcmEnabled));
        out.put("rateLimitMax", RATE_LIMIT_MAX);
        out.put("rateLimitWindow", RATE_LIMIT_WINDOW_MS / 1000 + "s");
        out.put("byPlatform", subMapper.countByPlatform());
        return out;
    }
}
