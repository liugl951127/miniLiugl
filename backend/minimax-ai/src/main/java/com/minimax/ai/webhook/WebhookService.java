package com.minimax.ai.webhook;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.ai.webhook.WebhookDelivery;
import com.minimax.ai.webhook.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Webhook 服务 (V2.9.1)
 *
 * <h3>核心能力</h3>
 * <ul>
 *   <li>订阅管理 (CRUD)</li>
 *   <li>事件总线 (publish/subscribe)</li>
 *   <li>异步投递 (HTTP POST + HMAC 签名)</li>
 *   <li>指数退避重试 (3 次, 1s/4s/16s)</li>
 *   <li>投递日志 (delivery_log)</li>
 * </ul>
 *
 * <h3>签名</h3>
 * <pre>
 *   signature = HMAC-SHA256(secret, body)
 *   header: X-Hub-Signature: sha256={signature}
 * </pre>
 *
 * @author MiniMax
 * @since V2.9.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookMapper webhookMapper;
    private final WebhookDeliveryMapper deliveryMapper;
    private final ObjectMapper objectMapper;

    /** 事件计数器 */
    private final Map<String, Long> eventCounters = new ConcurrentHashMap<>();

    /**
     * 创建订阅
     */
    @Transactional
    public Webhook create(String name, String description, String url, String events,
                          Long ownerId) {
        // 验证 URL
        if (url == null || !url.startsWith("http")) {
            throw new IllegalArgumentException("URL 必须以 http/https 开头");
        }
        // 生成 webhookId + secret
        String webhookId = "wh_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String secret = "secret_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        Webhook wh = Webhook.builder()
            .webhookId(webhookId)
            .name(name)
            .description(description)
            .url(url)
            .events(events)
            .secret(secret)
            .customHeaders("{}")
            .enabled(1)
            .status("ACTIVE")
            .deliveryCount(0L)
            .successCount(0L)
            .failCount(0L)
            .ownerId(ownerId)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        webhookMapper.insert(wh);
        log.info("[webhook] 创建 webhookId={} url={} events={}", webhookId, url, events);
        return wh;
    }

    /**
     * 更新
     */
    @Transactional
    public boolean update(String webhookId, Map<String, Object> updates) {
        Webhook wh = webhookMapper.selectOne(new QueryWrapper<Webhook>().eq("webhookId", webhookId));
        if (wh == null) return false;
        if (updates.containsKey("name")) wh.setName((String) updates.get("name"));
        if (updates.containsKey("description")) wh.setDescription((String) updates.get("description"));
        if (updates.containsKey("url")) wh.setUrl((String) updates.get("url"));
        if (updates.containsKey("events")) wh.setEvents((String) updates.get("events"));
        if (updates.containsKey("enabled")) wh.setEnabled(((Number) updates.get("enabled")).intValue());
        wh.setUpdatedAt(LocalDateTime.now());
        webhookMapper.updateById(wh);
        return true;
    }

    /**
     * 删除
     */
    @Transactional
    public boolean delete(String webhookId) {
        Webhook wh = webhookMapper.selectOne(new QueryWrapper<Webhook>().eq("webhookId", webhookId));
        if (wh == null) return false;
        webhookMapper.deleteById(wh.getId());
        return true;
    }

    /**
     * 列表
     */
    public List<Webhook> list(Long ownerId) {
        QueryWrapper<Webhook> qw = new QueryWrapper<>();
        if (ownerId != null) qw.eq("ownerId", ownerId);
        qw.orderByDesc("createdAt");
        return webhookMapper.selectList(qw);
    }

    /**
     * 详情
     */
    public Webhook detail(String webhookId) {
        return webhookMapper.selectOne(new QueryWrapper<Webhook>().eq("webhookId", webhookId));
    }

    /**
     * 发布事件 (核心入口)
     */
    public void publish(String eventType, Map<String, Object> payload) {
        eventCounters.merge(eventType, 1L, Long::sum);
        // 找所有订阅此事件的启用 webhook
        List<Webhook> all = webhookMapper.selectList(
            new QueryWrapper<Webhook>().eq("enabled", 1).eq("status", "ACTIVE"));
        for (Webhook wh : all) {
            if (matchesEvent(wh.getEvents(), eventType)) {
                deliver(wh, eventType, payload);
            }
        }
    }

    /**
     * 异步投递
     */
    @Async
    public void deliver(Webhook webhook, String eventType, Map<String, Object> payload) {
        String eventId = "evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        long start = System.currentTimeMillis();
        WebhookDelivery delivery = WebhookDelivery.builder()
            .webhookId(webhook.getWebhookId())
            .eventType(eventType)
            .eventId(eventId)
            .payload(toJsonSafe(payload))
            .status("PENDING")
            .retryCount(0)
            .createdAt(LocalDateTime.now())
            .build();
        deliveryMapper.insert(delivery);

        // 投递 (最多 3 次, 指数退避 0/1/4/16s)
        long[] delays = {0, 1000, 4000, 16000};
        int maxAttempts = 3;
        int attempt = 0;
        boolean success = false;
        String lastError = null;
        int lastStatus = 0;

        while (attempt < maxAttempts) {
            try {
                if (delays[attempt] > 0) Thread.sleep(delays[attempt]);
                long t0 = System.currentTimeMillis();
                int status = doHttpPost(webhook, eventType, eventId, payload);
                long duration = System.currentTimeMillis() - t0;
                if (status >= 200 && status < 300) {
                    success = true;
                    lastStatus = status;
                    delivery.setStatus("SUCCESS");
                    delivery.setResponseStatus(status);
                    delivery.setDurationMs(duration);
                    delivery.setRetryCount(attempt);
                    deliveryMapper.updateById(delivery);
                    webhookMapper.updateDeliveryStats(webhook.getWebhookId(), 1, 0, status);
                    log.info("[webhook] 投递成功 webhookId={} event={} status={} duration={}ms attempt={}",
                        webhook.getWebhookId(), eventType, status, duration, attempt + 1);
                    break;
                } else {
                    lastStatus = status;
                    lastError = "HTTP " + status;
                    attempt++;
                }
            } catch (Exception e) {
                lastError = e.getMessage();
                lastStatus = -1;
                attempt++;
                log.warn("[webhook] 投递异常 webhookId={} event={} attempt={}: {}",
                    webhook.getWebhookId(), eventType, attempt, e.getMessage());
            }
        }
        if (!success) {
            delivery.setStatus("FAILED");
            delivery.setResponseStatus(lastStatus);
            delivery.setErrorMsg(lastError);
            delivery.setRetryCount(attempt);
            deliveryMapper.updateById(delivery);
            webhookMapper.updateDeliveryStats(webhook.getWebhookId(), 0, 1, lastStatus);
        }
    }

    /**
     * 投递日志
     */
    public List<WebhookDelivery> deliveries(String webhookId, int limit) {
        if (limit <= 0) limit = 50;
        return deliveryMapper.findByWebhookId(webhookId, limit);
    }

    public List<WebhookDelivery> recentDeliveries(int limit) {
        if (limit <= 0) limit = 50;
        return deliveryMapper.findRecent(limit);
    }

    /**
     * 测试 webhook (Ping)
     */
    public WebhookDelivery test(String webhookId) {
        Webhook wh = detail(webhookId);
        if (wh == null) return null;
        Map<String, Object> payload = Map.of(
            "test", true,
            "message", "This is a test webhook delivery from MiniMax"
        );
        deliver(wh, "WEBHOOK_TEST", payload);
        return deliveryMapper.findByWebhookId(webhookId, 1).get(0);
    }

    /**
     * 事件统计
     */
    public Map<String, Object> eventStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("eventCounters", new LinkedHashMap<>(eventCounters));
        result.put("webhookCount", webhookMapper.selectCount(null));
        long active = webhookMapper.selectCount(
            new QueryWrapper<Webhook>().eq("status", "ACTIVE").eq("enabled", 1));
        result.put("activeWebhooks", active);
        return result;
    }

    // ============= 内部 =============

    private int doHttpPost(Webhook wh, String eventType, String eventId, Map<String, Object> payload) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                "eventId", eventId,
                "eventType", eventType,
                "webhookId", wh.getWebhookId(),
                "timestamp", System.currentTimeMillis(),
                "data", payload
            ));
            String signature = hmacSha256(wh.getSecret(), body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-Id", wh.getWebhookId());
            headers.set("X-Webhook-Event", eventType);
            headers.set("X-Webhook-Timestamp", String.valueOf(System.currentTimeMillis()));
            headers.set("X-Webhook-Delivery", eventId);
            headers.set("X-Hub-Signature", "sha256=" + signature);

            // 自定义 headers
            try {
                Map<String, String> custom = objectMapper.readValue(wh.getCustomHeaders(), Map.class);
                custom.forEach(headers::set);
            } catch (Exception ignore) {}

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = RestClient.create()
                .post()
                .uri(wh.getUrl())
                .body(body)
                .headers(h -> h.addAll(headers))
                .retrieve()
                .toEntity(String.class);
            return resp.getStatusCode().value();
        } catch (Exception e) {
            log.warn("[webhook] HTTP 失败: {}", e.getMessage());
            return -1;
        }
    }

    private boolean matchesEvent(String eventsCsv, String eventType) {
        if (eventsCsv == null || eventsCsv.isEmpty() || "*".equals(eventsCsv.trim())) return true;
        for (String e : eventsCsv.split(",")) {
            if (e.trim().equalsIgnoreCase(eventType)) return true;
        }
        return false;
    }

    public static String hmacSha256(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String toJsonSafe(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
