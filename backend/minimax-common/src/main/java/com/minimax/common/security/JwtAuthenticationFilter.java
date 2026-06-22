// =============================================================
// MiniMax - 共享 JWT 鉴权过滤器 (V5.0+ 演进到 V5.29)
// =============================================================
//
// 位置: minimax-common 模块 (被所有业务微服务复用, gateway 除外)
// 作用: 解析 HTTP Authorization 头, 校验 JWT, 把用户信息注入 Spring SecurityContext
// 触发时机: 每个 HTTP 请求进来时 (由 Spring Security 过滤器链调度)
//
// 设计原则:
//   1. access token 短命 (30min) + refresh token 长命 (7d) 双 token 机制
//   2. token 缺失 / 格式错误: 放行 (让下游 SecurityConfig 决定是否 401)
//   3. token 无效: 清空 SecurityContext (不抛异常, 避免打断 SSE 流)
//   4. 密钥从 yml 读取 (生产可注入环境变量覆盖)
// =============================================================

// 当前类所属包: com.minimax.common.security
package com.minimax.common.security;

// 业务异常类 (微服务层自定义异常)
import com.minimax.common.exception.BizException;
// 统一返回码 (所有 ResultCode.* 都是这个枚举)
import com.minimax.common.result.ResultCode;
// JJWT 库 - JWT 解析需要的 Claims 数据结构
import io.jsonwebtoken.Claims;
// JJWT 库 - JWT 解析入口
import io.jsonwebtoken.Jwts;
// JJWT 库 - HMAC SHA 密钥生成工具
import io.jsonwebtoken.security.Keys;
// Jakarta Servlet API (Spring Boot 3.x 用 jakarta, 不是 javax)
import jakarta.servlet.FilterChain;       // 过滤器链 - 调用 chain.doFilter() 放行
import jakarta.servlet.ServletException;  // Servlet 异常
import jakarta.servlet.http.HttpServletRequest;   // HTTP 请求对象
import jakarta.servlet.http.HttpServletResponse;  // HTTP 响应对象
// Lombok - 自动生成 log 字段 (private static final Logger)
import lombok.extern.slf4j.Slf4j;
// Spring - 注入 yml 配置值到字段
import org.springframework.beans.factory.annotation.Value;
// Spring Security - 用户名密码认证 token (本场景用 userId 当 principal)
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// Spring Security - 角色权限模型
import org.springframework.security.core.authority.SimpleGrantedAuthority;
// Spring Security - 当前线程的认证上下文 (ThreadLocal)
import org.springframework.security.core.context.SecurityContextHolder;
// Spring Security - 认证详情 (IP / SessionId 等)
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
// Spring Boot 自动配置条件 - 只在 Servlet 容器中生效 (排除 WebFlux)
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
// Spring 注解 - 注册为 Bean, 让 Spring 扫描管理
import org.springframework.stereotype.Component;
// Spring Web 工具 - 确保过滤器只执行一次 (防止 forward/include 重复触发)
import org.springframework.web.filter.OncePerRequestFilter;

// Java 标准 - 用于 HMAC SHA 密钥生成
import javax.crypto.SecretKey;
// Java 标准 - 字符串编码
import java.nio.charset.StandardCharsets;
// Java 标准 - SHA-256 摘要算法 (把任意长度 secret 转 256-bit)
import java.security.MessageDigest;
// Java 标准 - 摘要算法异常
import java.security.NoSuchAlgorithmException;
// Java 标准 - List 集合
import java.util.List;

/**
 * 共享 JWT 鉴权过滤器.
 *
 * 用法 (无需额外配置): 各业务模块 (auth/chat/memory/...) 只要依赖 minimax-common, Spring 会自动扫描并装配此 filter.
 *
 * 关键设计:
 *  1. access token 短命 (30min) + refresh token 长命 (7d) 双 token 机制
 *  2. 头缺失 / 格式错误: 放行 (让下游 SecurityConfig 决定要不要 401)
 *  3. token 无效: 直接清空 SecurityContext (不抛异常, 避免影响 SSE 流)
 *
 * 注: gateway 用自己的 JwtAuthGlobalFilter (WebFlux 体系), 此 filter 只用于 servlet 业务模块.
 */
@Slf4j                                                                       // Lombok 生成 log 字段
@Component                                                                   // 注册为 Spring Bean
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET) // 仅 servlet 容器生效 (排除 gateway)
public class JwtAuthenticationFilter extends OncePerRequestFilter {          // 继承 OncePerRequestFilter 保证单次执行

    // 从 application.yml 注入: HTTP 头名称 (默认 "Authorization")
    @Value("${minimax.jwt.header:Authorization}")
    private String header;

    // 从 yml 注入: token 前缀 (默认 "Bearer ", 注意末尾有空格)
    @Value("${minimax.jwt.prefix:Bearer }")
    private String prefix;

    // 从 yml 注入: HMAC SHA 签名密钥 (生产用环境变量覆盖)
    // 默认值: 0f6beadebfcee3e97845856757a3babf97b2af8c80f0b95690783ccc7a595352 (64 字符 hex)
    @Value("${minimax.jwt.secret:0f6beadebfcee3e97845856757a3babf97b2af8c80f0b95690783ccc7a595352}")
    private String secret;

    // 从 yml 注入: JWT 签发者 (校验 iss 字段, 防跨系统 token 误用)
    @Value("${minimax.jwt.issuer:minimax-platform}")
    private String issuer;

    /**
     * 生成 HMAC SHA-256 密钥.
     *
     * JJWT 要求 HMAC-SHA 密钥 ≥ 256 bit (32 字节).
     * 如果用户配置的 secret 不足 32 字节, 用 SHA-256 摘要扩展到 32 字节.
     *
     * @return SecretKey 用于 JJWT 签名/校验
     */
    private SecretKey key() {
        try {
            // 把字符串 secret 转成 UTF-8 字节数组
            byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
            // 用 SHA-256 摘要 (固定 32 字节输出)
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw);
            // 构造 JJWT 需要的 SecretKey 对象
            return Keys.hmacShaKeyFor(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 标准算法, 找不到说明 JVM 坏了
            throw new IllegalStateException(e);
        }
    }

    /**
     * 每次 HTTP 请求都会执行此方法.
     *
     * 流程:
     *   1. 读取 Authorization 头
     *   2. 校验前缀 (Bearer )
     *   3. 解析 JWT (签名验证 + 过期检查 + issuer 校验)
     *   4. 提取 userId / username / roles
     *   5. 构造 Authentication 放入 SecurityContext
     *   6. 放行到下一个过滤器
     *
     * @param req   HTTP 请求
     * @param resp  HTTP 响应
     * @param chain Spring Security 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        // 1. 读取 Authorization 头
        String h = req.getHeader(header);
        // 2. 检查是否以 Bearer 开头 (避免空头 / 错格式 token 报错)
        if (h != null && h.startsWith(prefix)) {
            // 3. 去掉 "Bearer " 前缀, 拿到纯 token
            String token = h.substring(prefix.length());
            try {
                // 4. 用 JJWT 解析: 验证签名 + 过期 + issuer
                Claims claims = Jwts.parser()
                        .verifyWith(key())                          // 用 HMAC 密钥校验签名
                        .requireIssuer(issuer)                      // 必须等于配置的 issuer
                        .build()
                        .parseSignedClaims(token)                   // 解析并验证 (失败抛 JwtException)
                        .getPayload();
                // 5. 提取 userId (JWT sub 字段约定存 userId 字符串)
                Long userId = Long.parseLong(claims.getSubject());
                // 6. 提取 roles (自定义 claim, 存角色列表如 ["USER","ADMIN"])
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) claims.get("roles");
                // 7. 构造 Spring Security 的 Authority 列表 (ROLE_USER / ROLE_ADMIN)
                List<SimpleGrantedAuthority> auth = roles == null ? List.of()
                        : roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
                // 8. 构造 Authentication 对象, principal 存 AuthenticatedUser (userId + username)
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                            new AuthenticatedUser(userId, claims.get("uname", String.class)),  // principal
                            null,                                                                // credentials (不需要密码)
                            auth);                                                               // authorities
                // 9. 补充认证详情 (IP / SessionId)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                // 10. 放入 SecurityContext (本线程后续代码可用 @AuthenticationPrincipal 取 userId)
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (Exception e) {
                // token 解析失败: 不抛异常, 清空上下文让 SecurityConfig 返 401
                log.debug("JWT 解析失败: {}", e.getMessage());
                SecurityContextHolder.clearContext();
                // 注意: 不重抛 BizException, 让下游 SecurityConfig 走 EntryPoint 返 JSON 401
                // (SSE 流场景下也会走到同一处, 避免断流)
            }
        }
        // 11. 放行到下一个过滤器 (不管 token 是否有效, 都不拦截)
        chain.doFilter(req, resp);
    }

    /** 存放在 principal 中的最小用户信息. 业务代码可用 @AuthenticationPrincipal AuthenticatedUser user 取. */
    public record AuthenticatedUser(Long id, String username) {}
}
