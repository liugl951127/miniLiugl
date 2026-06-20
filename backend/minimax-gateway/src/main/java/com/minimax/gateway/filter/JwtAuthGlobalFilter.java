package com.minimax.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 网关级 JWT 鉴权 GlobalFilter (V5.5).
 *
 * 流程:
 *   1. 检查路径是否在白名单 (public-paths)
 *   2. 提取 Authorization Header
 *   3. 解析 JWT, 验证签名和过期
 *   4. 注入 user_id / username 到下游请求 Header
 *   5. 失败返回 401
 *
 * 优势:
 *   - 12 个微服务不再各自解析 JWT (节省 12 份重复代码)
 *   - 微服务可以信任 X-User-Id Header
 *   - 统一拦截, 防止越权
 *
 * @since 2026-06
 */
@Slf4j
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    @Value("${minimax.jwt.secret}")
    private String jwtSecret;

    @Value("${minimax.jwt.gateway-auth.public-paths:}")
    private List<String> publicPathsConfig;

    private SecretKey key;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 1. 公开路径直接放行
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 2. 提取 Authorization
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "missing_authorization");
        }

        String token = authHeader.substring(7);
        try {
            // 3. 解析 JWT
            if (key == null) {
                key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            }
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = claims.get("sub", Long.class);
            String username = claims.get("uname", String.class);
            List<String> roles = claims.get("roles", List.class);

            if (userId == null) {
                return unauthorized(exchange, "invalid_token_payload");
            }

            // 4. 注入到下游请求 (微服务读 X-User-Id 即可)
            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r.header("X-User-Id", String.valueOf(userId))
                            .header("X-Username", username != null ? username : "")
                            .header("X-User-Roles", roles != null ? String.join(",", roles) : ""))
                    .build();

            log.debug("JWT OK: user={} path={}", userId, path);
            return chain.filter(mutated);
        } catch (Exception e) {
            log.warn("JWT 失败: {} path={}", e.getMessage(), path);
            return unauthorized(exchange, "invalid_token");
        }
    }

    private boolean isPublicPath(String path) {
        // 默认白名单
        if (path.startsWith("/api/v1/auth/login")
                || path.startsWith("/api/v1/auth/register")
                || path.startsWith("/api/v1/auth/refresh")
                || path.startsWith("/api/v1/auth/oauth")
                || path.startsWith("/api/v1/auth/wechat")
                || path.equals("/api/v1/health")
                || path.equals("/health")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/doc.html")
                || path.startsWith("/actuator")) {
            return true;
        }
        // 配置文件白名单
        if (publicPathsConfig != null) {
            for (String p : publicPathsConfig) {
                if (path.startsWith(p)) return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String code) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"code\":401,\"message\":\"%s\",\"data\":null}",
                code.equals("invalid_token") ? "Token 无效或已过期" :
                code.equals("missing_authorization") ? "缺少 Authorization 头" :
                code.equals("invalid_token_payload") ? "Token 缺少用户信息" :
                "鉴权失败");
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;  // 最高优先级, 限流之前
    }
}