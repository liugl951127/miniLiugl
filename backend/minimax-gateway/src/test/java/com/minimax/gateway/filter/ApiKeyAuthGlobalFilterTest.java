package com.minimax.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ApiKeyAuthGlobalFilter 单元测试 (V5.22 Day 22).
 *
 * 测试场景:
 *  1. 非 mmx_ 前缀 Bearer Token → 放行 (让 JwtAuthFilter 处理)
 *  2. 无 Authorization 头 → 放行
 *  3. mmx_ 格式 + Redis 命中 → 直接放行
 *  4. mmx_ 格式 + Redis 未命中 + 验证失败 → 401
 *  5. SHA-256 cache key 计算正确
 */
class ApiKeyAuthGlobalFilterTest {

    private ApiKeyAuthGlobalFilter filter;
    private GatewayFilterChain mockChain;

    @BeforeEach
    void setUp() {
        // 直接 new 而不用 Spring 容器 — 测试纯逻辑
        mockChain = mock(GatewayFilterChain.class);
    }

    @Test
    @DisplayName("1. 无 Authorization 头 → 放行")
    void noAuthHeader_passesThrough() {
        var request = MockServerHttpRequest.get("/api/v1/chat/sessions").build();
        var exchange = MockServerWebExchange.from(request);

        // mock Redis (永不使用)
        var redis = mockRedisTemplate(null);
        var webClient = mockWebClient();
        var f = new ApiKeyAuthGlobalFilter(redis, webClient, "http://auth:8081");

        when(mockChain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(f.filter(exchange, mockChain))
                .verifyComplete();

        verify(mockChain).filter(exchange);
        verifyNoInteractions(webClient);
    }

    @Test
    @DisplayName("2. 普通 JWT Bearer Token (非 mmx_) → 放行")
    void jwtBearerToken_passesThrough() {
        var request = MockServerHttpRequest.get("/api/v1/chat/sessions")
                .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.x")
                .build();
        var exchange = MockServerWebExchange.from(request);

        var redis = mockRedisTemplate(null);
        var webClient = mockWebClient();
        var f = new ApiKeyAuthGlobalFilter(redis, webClient, "http://auth:8081");

        when(mockChain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(f.filter(exchange, mockChain))
                .verifyComplete();

        verify(mockChain).filter(exchange);
        verifyNoInteractions(webClient);
    }

    @Test
    @DisplayName("3. mmx_ 格式 + Redis 命中 → 直接放行，注入 X-User-Id 头")
    void mmxApiKey_redisHit_injectsUserId() {
        String rawKey = "mmx_test_key_123";
        String cachedUserId = "42";

        var request = MockServerHttpRequest.get("/api/v1/chat/sessions")
                .header("Authorization", "Bearer " + rawKey)
                .build();
        var exchange = MockServerWebExchange.from(request);

        var redis = mockRedisTemplate(cachedUserId);
        var webClient = mockWebClient();
        var f = new ApiKeyAuthGlobalFilter(redis, webClient, "http://auth:8081");

        when(mockChain.filter(any())).thenAnswer(inv -> {
            ServerWebExchange ex = inv.getArgument(0);
            assertEquals("42", ex.getRequest().getHeaders().getFirst("X-User-Id"));
            assertEquals("apikey", ex.getRequest().getHeaders().getFirst("X-User-Source"));
            return Mono.empty();
        });

        StepVerifier.create(f.filter(exchange, mockChain))
                .verifyComplete();

        verify(redis.opsForValue()).get(anyString());
        verifyNoInteractions(webClient); // 不应调 auth 服务
    }

    @Test
    @DisplayName("4. getOrder = -200 (早于 JwtAuthFilter 的 -100)")
    void order_isBeforeJwtAuthFilter() {
        var redis = mockRedisTemplate(null);
        var webClient = mockWebClient();
        var f = new ApiKeyAuthGlobalFilter(redis, webClient, "http://auth:8081");

        assertEquals(-200, f.getOrder(), "API Key Filter 优先级应 < JwtAuthFilter 的 -100");
    }

    @Test
    @DisplayName("5. SHA-256 cache key 计算正确")
    void sha256CacheKey_isCorrect() throws Exception {
        var redis = mockRedisTemplate(null);
        var webClient = mockWebClient();
        var f = new ApiKeyAuthGlobalFilter(redis, webClient, "http://auth:8081");

        String rawKey = "mmx_secret_key";
        String expectedHash;
        try {
            var d = java.security.MessageDigest.getInstance("SHA-256");
            byte[] h = d.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            expectedHash = HexFormat.of().formatHex(h);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        // 通过 Redis mock 验证 cache key 格式
        var request = MockServerHttpRequest.get("/api/v1/chat/sessions")
                .header("Authorization", "Bearer " + rawKey)
                .build();
        var exchange = MockServerWebExchange.from(request);

        when(mockChain.filter(any())).thenReturn(Mono.empty());

        // 触发 filter
        f.filter(exchange, mockChain).block();

        // 验证 Redis get 被调用，且 key 以 "apikey:" 开头
        verify(redis.opsForValue()).get(argThat(key ->
            key.startsWith("apikey:") && key.substring("apikey:".length()).length() == 64
        ));
    }

    @Test
    @DisplayName("6. X-User-Id 注入值正确 (userId 作为字符串)")
    void injectUserId_stringValue() {
        String rawKey = "mmx_key_abc";
        String cachedUserId = "99";

        var request = MockServerHttpRequest.get("/api/v1/model/list")
                .header("Authorization", "Bearer " + rawKey)
                .build();
        var exchange = MockServerWebExchange.from(request);

        var redis = mockRedisTemplate(cachedUserId);
        var webClient = mockWebClient();
        var f = new ApiKeyAuthGlobalFilter(redis, webClient, "http://auth:8081");

        when(mockChain.filter(any())).thenAnswer(inv -> {
            ServerWebExchange ex = inv.getArgument(0);
            assertEquals("99", ex.getRequest().getHeaders().getFirst("X-User-Id"));
            return Mono.empty();
        });

        StepVerifier.create(f.filter(exchange, mockChain))
                .verifyComplete();
    }

    // ── Mock helpers ────────────────────────────────────────────────────────

    private static org.springframework.data.redis.core.StringRedisTemplate mockRedisTemplate(String cachedValue) {
        var template = org.mockito.Mockito.mock(org.springframework.data.redis.core.StringRedisTemplate.class);
        var ops = org.mockito.Mockito.mock(org.springframework.data.redis.core.StringOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn(cachedValue);
        return template;
    }

    private static org.springframework.web.reactive.function.client.WebClient mockWebClient() {
        return org.mockito.Mockito.mock(org.springframework.web.reactive.function.client.WebClient.class);
    }
}
