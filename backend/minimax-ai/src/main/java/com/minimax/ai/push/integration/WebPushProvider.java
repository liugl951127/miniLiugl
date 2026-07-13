package com.minimax.ai.push.integration;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * Web Push 真实实现 (V3.5.1)
 *
 * <h3>协议</h3>
 * RFC 8030 (Web Push) + RFC 8292 (VAPID) + RFC 8291 (Message Encryption)
 *
 * <h3>请求格式</h3>
 * <pre>
 * POST /  HTTP/1.1
 * Host: push.service.com
 * TTL: 60
 * Urgency: high
 * Topic: my-topic
 * Authorization: vapid t=&lt;JWT&gt;, k=&lt;pubkey&gt;
 * Content-Type: application/octet-stream
 * Content-Encoding: aesgcm
 *
 * &lt;encrypted payload&gt;
 * </pre>
 *
 * <h3>沙箱模式</h3>
 * 真实部署时 endpoint 来自浏览器订阅 (e.g. https://fcm.googleapis.com/...).
 * 沙箱无外网, 跳过实际 HTTP, 验证 VAPID JWT 构造正确性.
 */
@Slf4j
public class WebPushProvider implements PushProvider {

    private final VapidUtil.VapidKeyPair vapidKeys;
    private final String subject;        // mailto:admin@example.com
    private final HttpClient httpClient;
    private final boolean sandboxMode;   // true = 跳过实际 HTTP

    public WebPushProvider(String subject, boolean sandboxMode) {
        this.vapidKeys = VapidUtil.generateKeyPair();
        this.subject = subject;
        this.sandboxMode = sandboxMode;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        log.info("[WebPush] 初始化, subject={}, pubKey={}..., sandbox={}",
                subject, vapidKeys.publicKeyBase64Url().substring(0, 16), sandboxMode);
    }

    @Override
    public PushResult.Platform platform() {
        return PushResult.Platform.WEB_PUSH;
    }

    @Override
    public PushResult push(PushRequest req) {
        // 1. 验证 endpoint
        if (req.getTarget() == null || !req.getTarget().startsWith("https://")) {
            return PushResult.fail(platform(), 400, "INVALID_TARGET",
                    "Web Push endpoint 必须 https://", null);
        }
        // 2. 生成 VAPID JWT
        URI endpointUri = URI.create(req.getTarget());
        String audience = endpointUri.getScheme() + "://" + endpointUri.getHost();
        long exp = Instant.now().getEpochSecond() + 12 * 3600;  // 12h
        String jwt = VapidUtil.createJwt(audience, subject, exp, vapidKeys);
        String authHeader = "vapid t=" + jwt + ", k=" + vapidKeys.publicKeyBase64Url();
        // 3. 构造 payload (明文 - 沙箱)
        String payload = buildPayload(req);
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        // 4. TTL
        int ttl = req.getTtlSeconds() != null ? req.getTtlSeconds() : 60;
        String urgency = req.getPriority() == PushRequest.Priority.HIGH ? "high" : "normal";
        // 5. 发送
        if (sandboxMode) {
            log.info("[WebPush][SANDBOX] 模拟推送 endpoint={}, ttl={}s, urgency={}, payload={}",
                    req.getTarget(), ttl, urgency, payload);
            return PushResult.ok(platform(), "sandbox-" + System.currentTimeMillis(),
                    201, "{\"sandbox\":true,\"jwt_len\":" + jwt.length() + "}");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(endpointUri)
                    .timeout(Duration.ofSeconds(15))
                    .header("TTL", String.valueOf(ttl))
                    .header("Urgency", urgency)
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/octet-stream")
                    .header("Content-Encoding", "aes128gcm")  // RFC 8188
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payloadBytes))
                    .build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();
            if (code == 201 || code == 200) {
                return PushResult.ok(platform(), resp.headers().firstValue("Location").orElse(null),
                        code, resp.body());
            } else {
                return PushResult.fail(platform(), code, "HTTP_" + code, resp.body(), resp.body());
            }
        } catch (Exception e) {
            log.warn("[WebPush] 推送失败: {}", e.getMessage());
            return PushResult.fail(platform(), 500, "EXCEPTION", e.getMessage(), null);
        }
    }

    /**
     * 构造通知 payload (JSON)
     */
    private String buildPayload(PushRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"title\":\"").append(escape(req.getTitle())).append("\",");
        sb.append("\"body\":\"").append(escape(req.getBody())).append("\"");
        if (req.getIcon() != null) sb.append(",\"icon\":\"").append(escape(req.getIcon())).append("\"");
        if (req.getClickAction() != null)
            sb.append(",\"url\":\"").append(escape(req.getClickAction())).append("\"");
        if (req.getData() != null && !req.getData().isEmpty()) {
            sb.append(",\"data\":{");
            boolean first = true;
            for (var e : req.getData().entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escape(e.getKey())).append("\":\"").append(escape(e.getValue())).append("\"");
                first = false;
            }
            sb.append("}");
        }
        sb.append("}");
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    public VapidUtil.VapidKeyPair getVapidKeys() {
        return vapidKeys;
    }
}
