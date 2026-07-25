package com.minimax.admin.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 通用跨服务 HTTP 客户端 (Java 11+ HttpClient, 无需 Feign)。
 *
 * <p>V3.5.25 修复:</p>
 * <ul>
 *   <li>URL encoding: 路径段 + query string 都 encode, 防特殊字符丢失</li>
 *   <li>JWT 透传: 支持从 HttpServletRequest 透传 Authorization header, 解决 401</li>
 *   <li>serviceToken 备用: 内部服务间调用 (admin → auth)</li>
 * </ul>
 *
 * <p>用途: admin 服务调 auth/chat/model/rag/pipeline/ai/agent/monitor 13 个服务</p>
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
        return execute(baseUrl, path, null, "GET", null);
    }

    /**
     * GET 请求 - 带 query 参数
     *
     * <p>V3.5.25 新增: 自动 URL encode query 参数, 防 & = 等特殊字符丢失</p>
     */
    public String getWithQuery(String baseUrl, String path, Map<String, String> queryParams, String authHeader) {
        StringBuilder sb = new StringBuilder(path);
        if (queryParams != null && !queryParams.isEmpty()) {
            sb.append('?');
            boolean first = true;
            for (Map.Entry<String, String> e : queryParams.entrySet()) {
                if (!first) sb.append('&');
                sb.append(encode(e.getKey())).append('=').append(encode(e.getValue()));
                first = false;
            }
        }
        return execute(baseUrl, sb.toString(), null, "GET", authHeader);
    }

    /**
     * GET 请求 - 带 JWT 透传
     */
    public String get(String baseUrl, String path, String authHeader) {
        return execute(baseUrl, path, null, "GET", authHeader);
    }

    /**
     * POST 请求 - 带 JSON body
     */
    public String post(String baseUrl, String path, Object body) {
        return execute(baseUrl, path, body, "POST", null);
    }

    /**
     * POST 请求 - 带 JWT 透传
     */
    public String post(String baseUrl, String path, Object body, String authHeader) {
        return execute(baseUrl, path, body, "POST", authHeader);
    }

    /**
     * PUT 请求
     */
    public String put(String baseUrl, String path, Object body) {
        return execute(baseUrl, path, body, "PUT", null);
    }

    /**
     * PUT 请求 - 带 JWT 透传
     */
    public String put(String baseUrl, String path, Object body, String authHeader) {
        return execute(baseUrl, path, body, "PUT", authHeader);
    }

    /**
     * DELETE 请求
     */
    public String delete(String baseUrl, String path) {
        return execute(baseUrl, path, null, "DELETE", null);
    }

    /**
     * 核心执行方法 (V3.5.25 统一处理 URL encoding + JWT 透传)
     */
    private String execute(String baseUrl, String path, Object body, String method, String authHeader) {
        try {
            String url = stripSlash(baseUrl) + path;
            // V3.5.25: 用 URI 解析 (自动 path-segment encoding)
            URI uri = URI.create(url);
            HttpRequest.Builder hb = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(timeout))
                    .header("Accept", "application/json");

            // body 序列化
            String bodyStr = null;
            if (body != null) {
                bodyStr = json.writeValueAsString(body);
                hb.header("Content-Type", "application/json");
            }

            // method + body
            switch (method) {
                case "GET"    -> hb.GET();
                case "DELETE" -> hb.DELETE();
                case "POST"   -> hb.POST(HttpRequest.BodyPublishers.ofString(bodyStr != null ? bodyStr : "{}"));
                case "PUT"    -> hb.PUT(HttpRequest.BodyPublishers.ofString(bodyStr != null ? bodyStr : "{}"));
            }

            // V3.5.25: 鉴权优先级: 1) 透传 JWT, 2) serviceToken, 3) 无
            if (authHeader != null && !authHeader.isBlank()) {
                hb.header("Authorization", authHeader);
            } else if (serviceToken != null && !serviceToken.isBlank()) {
                hb.header("Authorization", "Bearer " + serviceToken);
            }

            HttpResponse<String> resp = client.send(hb.build(), HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();
            // 4xx 警告 (除 404 之外)
            if (code >= 400 && code != 404) {
                log.warn("service {} {}{} -> {} {}", method, baseUrl, path, code, truncate(resp.body(), 200));
            }
            return resp.body();
        } catch (Exception e) {
            log.warn("service {} fail {}{}: {}", method, baseUrl, path, e.getMessage());
            return null;
        }
    }

    /**
     * URL 编码 (V3.5.25+)
     */
    private String encode(String s) {
        if (s == null) return "";
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
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
            return false;
        }
    }

    /**
     * 构造错误响应
     */
    public Map<String, Object> errorResp(String msg) {
        Map<String, Object> r = new HashMap<>();
        r.put("code", 1500);
        r.put("message", msg);
        return r;
    }

    private String stripSlash(String s) { return s == null ? "" : (s.endsWith("/") ? s.substring(0, s.length()-1) : s); }
    private String truncate(String s, int n) { return s == null ? null : (s.length() > n ? s.substring(0, n) : s); }
}
