// =============================================================
// MiniMax - 网关级 JWT 鉴权 GlobalFilter (V5.5)
// =============================================================
//
// 位置: minimax-gateway 模块 (WebFlux 响应式)
// 作用: 所有外部 HTTP 请求进来时, 在 gateway 层统一校验 JWT
//       校验通过后, 把 userId/username/roles 注入到 X-User-* 头
//       下游 12 个微服务直接读 X-User-Id 即可, 不用各自解析 JWT
//
// 关键设计:
//   - 全局唯一鉴权点 (节省 12 份重复代码)
//   - 公开路径白名单 (登录/注册/微信/文档等直接放行)
//   - 失败直接返 401 JSON, 不进业务模块
//   - Ordered = -100: 在限流/Metrics 之前, 最高优先级
//
// @since 2026-06 (V5.5 引入)
// =============================================================

// 当前包: gateway filter 目录
package com.minimax.gateway.filter;

// JJWT 库 - Claims 是 JWT 解析后的载荷 Map
import io.jsonwebtoken.Claims;
// JJWT 库 - 入口
import io.jsonwebtoken.Jwts;
// JJWT 库 - HMAC 密钥工厂
import io.jsonwebtoken.security.Keys;
// Lombok - 自动生成 log 字段
import lombok.extern.slf4j.Slf4j;
// Spring - 注入 yml 配置
import org.springframework.beans.factory.annotation.Value;
// Spring Cloud Gateway - 过滤器链 (WebFlux 响应式)
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
// Spring Cloud Gateway - 全局过滤器接口
import org.springframework.cloud.gateway.filter.GlobalFilter;
// Spring - 排序接口 (Order 越小优先级越高)
import org.springframework.core.Ordered;
// Spring - 响应式数据缓冲 (WebFlux 用 Mono<DataBuffer>)
import org.springframework.core.io.buffer.DataBuffer;
// Spring HTTP - 状态码
import org.springframework.http.HttpStatus;
// Spring HTTP - MIME 类型
import org.springframework.http.MediaType;
// Spring HTTP 响应式 - 响应对象
import org.springframework.http.server.reactive.ServerHttpResponse;
// Spring - 注册为 Bean
import org.springframework.stereotype.Component;
// Spring WebFlux - 服务端 Web 交换 (请求 + 响应)
import org.springframework.web.server.ServerWebExchange;
// Reactor - 响应式 Mono (异步流)
import reactor.core.publisher.Mono;

// Java 标准 - HMAC SHA 密钥
import javax.crypto.SecretKey;
// Java 标准 - 字符串编码
import java.nio.charset.StandardCharsets;
// Java 标准 - 工具类
import java.util.*;

/**
 * 网关级 JWT 鉴权 GlobalFilter (V5.5).
 *
 * 流程:
 *   1. 检查路径是否在白名单 (public-paths)
 *   2. 提取 Authorization Header
 *   3. 解析 JWT, 验证签名和过期
 *   4. 注入 user_id / username 到下游请求 Header
 *   5. 失败返回 401 JSON
 *
 * 优势:
 *   - 12 个微服务不再各自解析 JWT (节省 12 份重复代码)
 *   - 微服务可以信任 X-User-Id Header (无需再次校验)
 *   - 统一拦截, 防止越权
 */
@Slf4j                                                                          // Lombok 生成 log
@Component                                                                      // 注册为 Spring Bean
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {             // 实现 GlobalFilter + Ordered 接口

    // 从 yml 注入: JWT 签名密钥 (生产用环境变量)
    // 注: 必须配置, 没有默认值 (gateway 强制要求)
    @Value("${minimax.jwt.secret}")
    private String jwtSecret;

    // 从 yml 注入: 自定义白名单路径 (e.g. "/api/v1/wechat/**")
    @Value("${minimax.jwt.gateway-auth.public-paths:}")
    private List<String> publicPathsConfig;

    // HMAC 密钥 (懒加载, 第一次请求时初始化)
    private SecretKey key;

    /**
     * 每个 HTTP 请求都会经过此方法.
     *
     * @param exchange WebFlux 请求/响应包装
     * @param chain    过滤器链 (chain.filter() 放行到下游)
     * @return Mono<Void> 响应式返回值
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 取出请求路径 (e.g. "/api/v1/auth/login")
        String path = exchange.getRequest().getPath().value();

        // 1. 白名单路径直接放行 (无需 token)
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 2. 提取 Authorization 头
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 没带 token, 直接 401
            return unauthorized(exchange, "missing_authorization");
        }

        // 3. 去掉 "Bearer " 前缀 (7 字符), 拿到纯 token
        String token = authHeader.substring(7);
        try {
            // 4. 懒加载密钥 (第一次请求时构造, 后续复用)
            if (key == null) {
                key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            }
            // 5. 解析并验证 JWT (签名 + 过期)
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 6. 提取用户信息 (sub=userId, uname=username, roles=角色列表)
            Long userId = claims.get("sub", Long.class);
            String username = claims.get("uname", String.class);
            List<String> roles = claims.get("roles", List.class);

            // 7. 校验 payload 必须有 userId
            if (userId == null) {
                return unauthorized(exchange, "invalid_token_payload");
            }

            // 8. 把 userId / username / roles 注入到下游请求头 (mutate 不改原 exchange, 而是新生成一个)
            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r.header("X-User-Id", String.valueOf(userId))                                // 用户 ID
                            .header("X-Username", username != null ? username : "")                            // 用户名
                            .header("X-User-Roles", roles != null ? String.join(",", roles) : ""))             // 角色 (逗号分隔)
                    .build();

            log.debug("JWT OK: user={} path={}", userId, path);
            // 9. 放行到下游 (携带注入的头)
            return chain.filter(mutated);
        } catch (Exception e) {
            // 10. token 无效 (签名错 / 过期 / 格式错), 401
            log.warn("JWT 失败: {} path={}", e.getMessage(), path);
            return unauthorized(exchange, "invalid_token");
        }
    }

    /**
     * 判断路径是否在白名单.
     *
     * 白名单包含:
     *   - 认证相关: /auth/login /auth/register /auth/refresh
     *   - 第三方登录: /auth/oauth /auth/wechat
     *   - 健康检查: /health /actuator/*
     *   - API 文档: /v3/api-docs /swagger-ui /doc.html
     *
     * @param path 请求路径
     * @return true 表示放行 (无需 token)
     */
    private boolean isPublicPath(String path) {
        // 默认白名单 (硬编码, 覆盖常用路径)
        if (path.startsWith("/api/v1/auth/login")
                || path.startsWith("/api/v1/auth/register")
                || path.startsWith("/api/v1/auth/refresh")
                || path.startsWith("/api/v1/auth/oauth")
                || path.startsWith("/api/v1/auth/wechat")
                || path.equals("/api/v1/health")
                || path.equals("/health")
                || path.startsWith("/v3/api-docs")         // OpenAPI JSON
                || path.startsWith("/swagger-ui")         // Swagger UI
                || path.startsWith("/doc.html")            // Knife4j UI
                || path.startsWith("/actuator")) {         // Spring Boot Actuator
            return true;
        }
        // 配置文件白名单 (用户可在 yml 自定义)
        if (publicPathsConfig != null) {
            for (String p : publicPathsConfig) {
                if (path.startsWith(p)) return true;
            }
        }
        return false;
    }

    /**
     * 构造 401 JSON 响应.
     *
     * WebFlux 响应式写响应: response.writeWith(Mono.just(buffer))
     *
     * @param exchange WebFlux exchange
     * @param code     错误码 (missing_authorization / invalid_token 等)
     * @return Mono<Void> 写完响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String code) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);                                  // HTTP 401
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);                // JSON 响应

        // 根据错误码返回不同中文消息
        String body = String.format(
                "{\"code\":401,\"message\":\"%s\",\"data\":null}",
                code.equals("invalid_token") ? "Token 无效或已过期" :
                code.equals("missing_authorization") ? "缺少 Authorization 头" :
                code.equals("invalid_token_payload") ? "Token 缺少用户信息" :
                "鉴权失败");
        // 把 JSON 字符串包装成响应式 DataBuffer
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 过滤器执行顺序.
     *
     * @return -100 (最高优先级, 在限流和 Metrics 之前执行)
     */
    @Override
    public int getOrder() {
        return -100;  // 越小越先执行, -100 表示最早执行
    }
}
