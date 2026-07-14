package com.minimax.gateway.filter;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HexFormat;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * API Key 限流 Key 解析器 (V5.33 Day 19).
 *
 * 优先级:
 *   1) X-User-Id 头 (已由 ApiKeyAuthGlobalFilter / JwtAuthGlobalFilter 注入)
 *   2) Authorization: Bearer mmx_xxxx (从请求中提取, SHA-256 摘要)
 *   3) X-Forwarded-For / 真实 IP (兜底)
 *
 * 限流维度:
 *   - 登录用户: 按 userId
 *   - API Key 用户: 按 key 的 SHA-256 哈希 (不暴露真实 key)
 *   - 匿名: 按 IP
 *
 * @since V5.33 Day 19
 */
@Component("apiKeyRateLimitResolver")
@org.springframework.context.annotation.Primary
public class ApiKeyRateLimitResolver implements KeyResolver {

    private static final String API_KEY_PREFIX = "mmx_";

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        // 1. 优先用已注入的 X-User-Id (API Key 用户或 JWT 用户)
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.isBlank()) {
            return Mono.just("user:" + userId);
        }

        // 2. 尝试从 Authorization 头提取 API Key 并哈希
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer " + API_KEY_PREFIX)) {
            String rawKey = authHeader.substring("Bearer ".length());
            return Mono.just("apikey:" + sha256(rawKey));
        }

        // 3. 兜底: 按 IP
        String ip = getClientIp(exchange);
        return Mono.just("ip:" + ip);
    }

    private String getClientIp(ServerWebExchange exchange) {
        // 依次尝试: X-Forwarded-For → X-Real-IP → remoteAddress
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    private String sha256(String input) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            byte[] h = d.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (Exception e) {
            return input; // fallback to raw key (shouldn't happen)
        }
    }
}
