// =============================================================
// MiniMax - 网关级 JWT 鉴权 GlobalFilter (V3.5.5+ 完整注释版)
// =============================================================
//
// 位置: minimax-gateway 模块 (WebFlux 响应式)
// 作用: 所有外部 HTTP 请求进来时, 在 gateway 层统一校验 JWT
//       校验通过后, 把 userId/username/roles 注入到 X-User-* 头
//       下游 16 个微服务直接读 X-User-Id 即可, 不用各自解析 JWT
//
// 关键设计:
//   - 全局唯一鉴权点 (节省 16 份重复代码)
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
import java.util.List;

/**
 * 网关级 JWT 鉴权 GlobalFilter (V3.5.5+ 完整注释版)
 *
 * <h2>业务定位</h2>
 * 整个 16 微服务架构的"安全守门员":
 * <ul>
 *   <li>所有外部 HTTP 请求先经过 gateway</li>
 *   <li>gateway 统一校验 JWT (签名 + 过期)</li>
 *   <li>校验通过: 提取 user 信息, 注入 X-User-* 头, 放行到下游</li>
 *   <li>校验失败: 直接返 401, 不进业务</li>
 * </ul>
 *
 * <h2>为什么在 gateway 而不是每个服务</h2>
 * <ul>
 *   <li>节省 16 份重复 JWT 解析代码</li>
 *   <li>微服务信任 X-User-Id (内网可信), 无需再次校验</li>
 *   <li>统一拦截, 防止越权 + 减少 token 泄露面</li>
 * </ul>
 *
 * <h2>流程</h2>
 * <pre>
 *   HTTP 请求
 *     ↓
 *   1. 检查白名单 (登录/微信/文档)? → 直接放行
 *     ↓ 不是
 *   2. 提取 Authorization 头 (Bearer xxx)
 *     ↓ 没带
 *   3. 401 missing_authorization
 *     ↓ 有
 *   4. 解析 JWT (签名 + 过期)
 *     ↓ 失败
 *   5. 401 invalid_token
 *     ↓ 成功
 *   6. 提取 userId/username/roles
 *   7. 注入 X-User-* 头到下游请求
 *   8. 放行
 * </pre>
 *
 * @author MiniMax
 * @since V5.5 (2026-06)
 */
@Slf4j
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    // ============== 配置注入 ==============

    /**
     * JWT 签名密钥 (从 yml 注入, 生产用环境变量)
     * 注: 必须配置, 没有默认值 (gateway 强制要求)
     */
    @Value("${minimax.jwt.secret}")
    private String jwtSecret;

    /**
     * 自定义白名单路径 (e.g. "/api/v1/wechat/**")
     * 从 yml 的 minimax.jwt.gateway-auth.public-paths 读取
     * 默认值空列表
     */
    @Value("${minimax.jwt.gateway-auth.public-paths:}")
    private List<String> publicPathsConfig;

    // ============== 运行时状态 ==============

    /**
     * HMAC 密钥 (懒加载, 第一次请求时初始化)
     * <p>为什么懒加载: @Value 注入时还没解析, 第一次请求才需要
     */
    private SecretKey key;

    // ============== 业务常量 ==============

    /** Bearer 前缀长度 (含空格) */
    private static final int BEARER_PREFIX_LENGTH = 7;

    /** Authorization 头名前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    /** 注入到下游请求的用户 ID header */
    private static final String HEADER_USER_ID = "X-User-Id";

    /** 注入到下游请求的用户名 header */
    private static final String HEADER_USERNAME = "X-Username";

    /** 注入到下游请求的角色 header (逗号分隔) */
    private static final String HEADER_USER_ROLES = "X-User-Roles";

    /** JWT claim key: 用户 ID */
    private static final String CLAIM_SUB = "sub";

    /** JWT claim key: 用户名 */
    private static final String CLAIM_UNAME = "uname";

    /** JWT claim key: 角色列表 */
    private static final String CLAIM_ROLES = "roles";

    /** 过滤器执行顺序: -100 (在限流和 Metrics 之前) */
    private static final int FILTER_ORDER = -100;

    // ============== 核心: filter ==============

    /**
     * 每个 HTTP 请求都会经过此方法 (Spring Cloud Gateway 调用)
     *
     * @param exchange WebFlux 请求/响应包装
     * @param chain    过滤器链 (chain.filter() 放行到下游)
     * @return Mono&lt;Void&gt; 响应式返回值 (异步)
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 取请求路径 (e.g. "/api/v1/auth/login")
        String path = exchange.getRequest().getPath().value();

        // 2. 白名单路径直接放行 (无需 token)
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 3. 提取 Authorization 头
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        // 没带 token 或格式不对 (不 "Bearer " 开头), 401
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, "missing_authorization");
        }

        // 4. 去掉 "Bearer " 前缀 (7 字符), 拿到纯 token
        String token = authHeader.substring(BEARER_PREFIX_LENGTH);

        try {
            // 5. 懒加载 HMAC 密钥 (第一次请求时构造, 后续复用)
            if (key == null) {
                // hmacShaKeyFor: 字节数组转 SecretKey
                key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            }

            // 6. 解析并验证 JWT (签名 + 过期)
            //    parseSignedClaims: 验证签名 + 解析 payload
            //    失败抛异常 (e.g. 签名错 / 过期 / 格式错)
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 7. 提取用户信息
            //    sub: 用户 ID (Long 类型, 与 auth-service 签发时一致)
            //    uname: 用户名
            //    roles: 角色列表 (e.g. ["USER", "ADMIN"])
            Long userId = claims.get(CLAIM_SUB, Long.class);
            String username = claims.get(CLAIM_UNAME, String.class);
            List<String> roles = claims.get(CLAIM_ROLES, List.class);

            // 8. 校验 payload 必须有 userId
            if (userId == null) {
                return unauthorized(exchange, "invalid_token_payload");
            }

            // 9. 注入 userId / username / roles 到下游请求头
            //    mutate: 不改原 exchange, 生成新 exchange
            //    这样多个请求不会互相干扰 (响应式不可变)
            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r.header(HEADER_USER_ID, String.valueOf(userId))
                            .header(HEADER_USERNAME, username != null ? username : "")
                            .header(HEADER_USER_ROLES, roles != null ? String.join(",", roles) : ""))
                    .build();

            log.debug("JWT OK: user={} path={}", userId, path);

            // 10. 放行到下游 (携带注入的头)
            return chain.filter(mutated);
        } catch (Exception e) {
            // 11. token 无效 (签名错 / 过期 / 格式错), 401
            log.warn("JWT 失败: {} path={}", e.getMessage(), path);
            return unauthorized(exchange, "invalid_token");
        }
    }

    // ============== 白名单匹配 ==============

    /**
     * 判断路径是否在白名单 (无需 token)
     *
     * <p>白名单包含:
     * <ul>
     *   <li>认证相关: /api/v1/auth/{login, register, refresh, oauth, wechat}</li>
     *   <li>健康检查: /api/v1/health /health /actuator/*</li>
     *   <li>API 文档: /v3/api-docs /swagger-ui /doc.html</li>
     * </ul>
     *
     * <p>另外支持 yml 自定义白名单 (minimax.jwt.gateway-auth.public-paths)
     *
     * @param path 请求路径
     * @return true 表示放行 (无需 token)
     */
    private boolean isPublicPath(String path) {
        // 1. 默认白名单 (硬编码, 覆盖常用公开路径)
        if (path.startsWith("/api/v1/auth/login")
                || path.startsWith("/api/v1/auth/register")
                || path.startsWith("/api/v1/auth/refresh")
                || path.startsWith("/api/v1/auth/oauth")
                || path.startsWith("/api/v1/auth/wechat")
                || path.equals("/api/v1/health")
                || path.equals("/health")
                || path.startsWith("/v3/api-docs")         // OpenAPI JSON
                || path.startsWith("/swagger-ui")          // Swagger UI
                || path.startsWith("/doc.html")            // Knife4j UI
                || path.startsWith("/actuator")) {         // Spring Boot Actuator
            return true;
        }

        // 2. 配置文件白名单 (用户可在 yml 自定义)
        //    空/null 都跳过
        if (publicPathsConfig != null) {
            for (String p : publicPathsConfig) {
                // 前缀匹配 (支持 "/api/v1/wechat/**" 这类)
                if (path.startsWith(p)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ============== 401 响应构造 ==============

    /**
     * 构造 401 JSON 响应
     *
     * <p>WebFlux 响应式写响应: {@code response.writeWith(Mono.just(buffer))}
     * 而不是传统 Spring MVC 的 {@code response.getWriter().write(...)}
     *
     * @param exchange WebFlux exchange
     * @param code     错误码 (missing_authorization / invalid_token / invalid_token_payload)
     * @return Mono&lt;Void&gt; 写完响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String code) {
        // 1. 拿响应对象
        ServerHttpResponse response = exchange.getResponse();
        // 2. 设状态码 401 Unauthorized
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        // 3. 设 Content-Type: application/json
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 4. 根据错误码返回不同中文消息
        String message = switch (code) {
            case "invalid_token" -> "Token 无效或已过期";
            case "missing_authorization" -> "缺少 Authorization 头";
            case "invalid_token_payload" -> "Token 缺少用户信息";
            default -> "鉴权失败";
        };

        // 5. 构造 JSON 响应体 (标准 {code, message, data} 结构)
        String body = String.format(
                "{\"code\":401,\"message\":\"%s\",\"data\":null}",
                message
        );

        // 6. 把 JSON 字符串包装成响应式 DataBuffer
        //    response.bufferFactory().wrap(...) 申请内存 buffer
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        // 7. 写响应 (响应式, Mono.just 单元素流)
        return response.writeWith(Mono.just(buffer));
    }

    // ============== 过滤器顺序 ==============

    /**
     * 过滤器执行顺序
     *
     * <p>数值越小越先执行, -100 表示最早执行
     * (在限流 / Metrics 之前, 是请求处理链的第一个过滤器)
     *
     * @return -100
     */
    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }
}
