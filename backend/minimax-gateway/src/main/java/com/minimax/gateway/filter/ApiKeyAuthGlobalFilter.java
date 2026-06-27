package com.minimax.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * API Key 鉴权过滤器 (V5.33 Day 19).
 *
 * 优先级: Order = -200 (比 JwtAuthGlobalFilter -100 更早执行)
 *
 * 拦截逻辑:
 *   1. 检查 Authorization 是否为 "Bearer mmx_xxxx" (API Key 格式)
 *   2. 从 Redis 缓存查询 key → userId
 *   3. 缓存未命中 → WebClient 调用 auth 服务 /internal/apikey/validate
 *   4. 验证成功: 注入 X-User-Id 头, 放行
 *   5. 验证失败: 401
 *
 * 缓存策略:
 *   - Key: "apikey:" + SHA-256(rawKey)
 *   - Value: userId
 *   - TTL: 5 分钟 (快速失效)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String API_KEY_PREFIX = "mmx_";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final String CACHE_KEY_PREFIX = "apikey:";

    private final WebClient webClient;
    private final StringRedisTemplate redisTemplate;
    private String authServiceUrl;

    /** 测试用构造函数 */
    public ApiKeyAuthGlobalFilter(WebClient webClient, StringRedisTemplate redisTemplate, String authServiceUrl) {
        this.webClient = webClient;
        this.redisTemplate = redisTemplate;
        this.authServiceUrl = authServiceUrl;
    }

    @org.springframework.beans.factory.annotation.Value("${minimax.auth.service-url:http://minimax-auth:8081}")
    public void setAuthServiceUrl(String url) {
        this.authServiceUrl = url;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 1. 只拦截 API Key 格式的 Bearer Token
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer " + API_KEY_PREFIX)) {
            // 非 API Key 格式, 放行 (让 JwtAuthGlobalFilter 处理)
            return chain.filter(exchange);
        }

        String rawKey = authHeader.substring("Bearer ".length());

        // 2. Redis 缓存查询
        String cacheKey = CACHE_KEY_PREFIX + sha256(rawKey);
        String cachedUserId = redisTemplate.opsForValue().get(cacheKey);

        if (cachedUserId != null) {
            log.debug("API Key 缓存命中: path={}", path);
            return injectAndProceed(exchange, chain, cachedUserId);
        }

        // 3. 调用 auth 服务验证
        return validateViaAuthService(rawKey)
                .flatMap(userId -> {
                    if (userId == null) {
                        return unauthorized(exchange, "invalid_api_key");
                    }
                    // 4. 缓存结果
                    redisTemplate.opsForValue().set(cacheKey, String.valueOf(userId), CACHE_TTL);
                    log.debug("API Key 验证成功: userId={} path={}", userId, path);
                    return injectAndProceed(exchange, chain, String.valueOf(userId));
                })
                .onErrorResume(e -> {
                    log.error("API Key 验证请求失败: {}", e.getMessage());
                    // 验证服务不可用时, 拒绝访问 (安全优先)
                    return unauthorized(exchange, "auth_service_unavailable");
                });
    }

    /**
     * 调用 auth 服务内部接口验证 API Key.
     * @return userId 或 null (无效)
     */
    private Mono<Long> validateViaAuthService(String rawKey) {
        return webClient.post()
                .uri(authServiceUrl + "/internal/apikey/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("rawKey", rawKey))
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    if (response.containsKey("userId")) {
                        Object uid = response.get("userId");
                        if (uid instanceof Number) {
                            return Mono.just(((Number) uid).longValue());
                        }
                    }
                    return Mono.empty();
                });
    }

    /** 注入 X-User-Id 头并放行 */
    private Mono<Void> injectAndProceed(ServerWebExchange exchange, GatewayFilterChain chain, String userId) {
        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.header("X-User-Id", userId)
                        .header("X-User-Source", "apikey"))
                .build();
        return chain.filter(mutated);
    }

    /** 构造 401 响应 */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private String sha256(String input) {
        try {
            var d = java.security.MessageDigest.getInstance("SHA-256");
            byte[] h = d.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /** 比 JwtAuthGlobalFilter (-100) 更早执行 */
    @Override
    public int getOrder() {
        return -200;
    }
}
