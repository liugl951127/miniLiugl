package com.minimax.monitor.alert;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.minimax.monitor.entity.AlertEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import jakarta.annotation.Resource;

/**
 * 钉钉 WebHook 告警通知器 (V5.33 Day 18).
 *
 * 配置示例 (alert_channel.config JSON):
 *   {
 *     "webhook": "https://oapi.dingtalk.com/robot/send?access_token=xxx",
 *     "secret": "SEC..."          // 签名密钥，可选
 *   }
 *
 * 签名算法: HMAC-SHA256
 *   sign = Base64(HMAC-SHA256(secret, timestamp + "\n" + secret))
 */
@Slf4j
@Component
public class DingTalkAlertNotifier implements AlertNotifier {

    @Resource
    private HttpClient httpClient;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String COLOR_OK     = "info".equals("info") ? "info" : "warning".equals("info") ? "#FFA500" : "#FF0000";
    private static final String COLOR_FATAL  = "#FF0000";
    private static final String COLOR_WARN   = "#FFA500";
    private static final String COLOR_INFO   = "#1890FF";

    @Override
    public boolean send(AlertEvent event, String channelConfig) {
        try {
            JSONObject cfg = new JSONObject(channelConfig);
            String webhook = cfg.getStr("webhook");
            String secret  = cfg.getStr("secret");

            if (webhook == null || webhook.isBlank()) {
                log.warn("dingtalk webhook not configured");
                return false;
            }

            String url = buildSignedUrl(webhook, secret);
            String body = buildBody(event);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            boolean ok = resp.statusCode() == 200;
            if (ok) {
                log.info("dingtalk alert sent for rule {}", event.getRuleName());
            } else {
                log.warn("dingtalk alert failed: {} - {}", resp.statusCode(), resp.body());
            }
            return ok;
        } catch (Exception e) {
            log.warn("dingtalk alert exception: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String channelType() {
        return "DINGTALK";
    }

    // ---- private helpers ----

    private String buildSignedUrl(String webhook, String secret) {
        if (secret == null || secret.isBlank()) return webhook;
        try {
            long timestamp = System.currentTimeMillis();
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signBytes = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String sign = Base64.getEncoder().encodeToString(signBytes);
            return webhook + "&timestamp=" + timestamp + "&sign=" + java.net.URLEncoder.encode(sign, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return webhook;
        }
    }

    private String buildBody(AlertEvent e) {
        String color = switch (e.getSeverity() == null ? "" : e.getSeverity().toLowerCase()) {
            case "critical" -> COLOR_FATAL;
            case "warning"  -> COLOR_WARN;
            default         -> COLOR_INFO;
        };

        JSONObject at = new JSONObject();
        at.set("isAtAll", false);

        JSONObject body = new JSONObject();
        body.set("msgtype", "markdown");

        JSONObject markdown = new JSONObject();
        markdown.set("title", String.format("【%s】%s", e.getSeverity().toUpperCase(), e.getRuleName()));
        String content = String.format(
                "### 【%s】告警: %s\n\n" +
                "> 指标: `%s`  当前值: **%s**  阈值: %s\n\n" +
                "> 触发时间: %s\n\n" +
                "> 消息: %s",
                e.getSeverity().toUpperCase(),
                e.getRuleName(),
                e.getMetricName(),
                e.getMetricValue(),
                e.getThreshold(),
                e.getFiredAt() != null ? e.getFiredAt().format(FMT) : "N/A",
                e.getMessage() != null ? e.getMessage() : "无"
        );
        markdown.set("content", content);
        body.set("markdown", markdown);
        body.set("at", at);

        return JSONUtil.toJsonStr(body);
    }
}
