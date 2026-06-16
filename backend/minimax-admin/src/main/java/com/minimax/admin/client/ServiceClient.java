package com.minimax.admin.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 通用跨服务 HTTP 客户端 (Java 11+ HttpClient, 无需 Feign)。
 *
 * 用途: admin 服务调 auth/chat/model/memory/rag/function 6 个服务
 * 鉴权: 用 service-to-service token (admin 用自己的 JWT, 或 service account)
 */
@Slf4j
@Component
public class ServiceClient {

    @Value("${minimax.admin.service-token:}")
    private String serviceToken;

    @Value("${minimax.admin.timeout-seconds:10}")
    private int timeout;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper json = new ObjectMapper();

    /**
     * GET 请求 - 返回 raw body (JSON 字符串) 或 null
     */
    public String get(String baseUrl, String path) {
        try {
            HttpRequest.Builder hb = HttpRequest.newBuilder()
                    .uri(URI.create(stripSlash(baseUrl) + path))
                    .timeout(Duration.ofSeconds(timeout))
                    .header("Accept", "application/json")
                    .GET();
            if (serviceToken != null && !serviceToken.isBlank()) {
                hb.header("Authorization", "Bearer " + serviceToken);
            }
            HttpResponse<String> resp = client.send(hb.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                log.warn("service GET {}{} -> {} {}", baseUrl, path, resp.statusCode(), truncate(resp.body(), 200));
                return null;
            }
            return resp.body();
        } catch (Exception e) {
            log.warn("service GET fail {}{}: {}", baseUrl, path, e.getMessage());
            return null;
        }
    }

    /**
     * POST 请求 - 带 JSON body
     */
    public String post(String baseUrl, String path, Object body) {
        try {
            String bodyStr = body == null ? "{}" : json.writeValueAsString(body);
            HttpRequest.Builder hb = HttpRequest.newBuilder()
                    .uri(URI.create(stripSlash(baseUrl) + path))
                    .timeout(Duration.ofSeconds(timeout))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyStr));
            if (serviceToken != null && !serviceToken.isBlank()) {
                hb.header("Authorization", "Bearer " + serviceToken);
            }
            HttpResponse<String> resp = client.send(hb.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                log.warn("service POST {}{} -> {} {}", baseUrl, path, resp.statusCode(), truncate(resp.body(), 200));
                return null;
            }
            return resp.body();
        } catch (Exception e) {
            log.warn("service POST fail {}{}: {}", baseUrl, path, e.getMessage());
            return null;
        }
    }

    /**
     * PUT 请求
     */
    public String put(String baseUrl, String path, Object body) {
        try {
            String bodyStr = body == null ? "{}" : json.writeValueAsString(body);
            HttpRequest.Builder hb = HttpRequest.newBuilder()
                    .uri(URI.create(stripSlash(baseUrl) + path))
                    .timeout(Duration.ofSeconds(timeout))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(bodyStr));
            if (serviceToken != null && !serviceToken.isBlank()) {
                hb.header("Authorization", "Bearer " + serviceToken);
            }
            HttpResponse<String> resp = client.send(hb.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                log.warn("service PUT {}{} -> {} {}", baseUrl, path, resp.statusCode(), truncate(resp.body(), 200));
                return null;
            }
            return resp.body();
        } catch (Exception e) {
            log.warn("service PUT fail {}{}: {}", baseUrl, path, e.getMessage());
            return null;
        }
    }

    /**
     * DELETE 请求
     */
    public String delete(String baseUrl, String path) {
        try {
            HttpRequest.Builder hb = HttpRequest.newBuilder()
                    .uri(URI.create(stripSlash(baseUrl) + path))
                    .timeout(Duration.ofSeconds(timeout))
                    .header("Accept", "application/json")
                    .DELETE();
            if (serviceToken != null && !serviceToken.isBlank()) {
                hb.header("Authorization", "Bearer " + serviceToken);
            }
            HttpResponse<String> resp = client.send(hb.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400 && resp.statusCode() != 404) {
                log.warn("service DELETE {}{} -> {}", baseUrl, path, resp.statusCode());
                return null;
            }
            return resp.body();
        } catch (Exception e) {
            log.warn("service DELETE fail {}{}: {}", baseUrl, path, e.getMessage());
            return null;
        }
    }

    /**
     * 简单 health probe - 返回 true 表示服务可达
     */
    public boolean isReachable(String baseUrl) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(stripSlash(baseUrl) + "/actuator/health"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() < 500;
        } catch (Exception e) {
            // 也试 /api/v1/auth/health (auth 服务没有 actuator)
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(stripSlash(baseUrl) + "/api/v1/auth/health"))
                        .timeout(Duration.ofSeconds(2))
                        .GET()
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                return resp.statusCode() < 500;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    /** 构造错误响应 */
    public Map<String, Object> errorResp(String msg) {
        Map<String, Object> r = new HashMap<>();
        r.put("code", 1500);
        r.put("message", msg);
        return r;
    }

    private String stripSlash(String s) { return s == null ? "" : (s.endsWith("/") ? s.substring(0, s.length()-1) : s); }
    private String truncate(String s, int n) { return s == null ? null : (s.length() > n ? s.substring(0, n) : s); }
}
