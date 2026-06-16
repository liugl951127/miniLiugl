package com.minimax.common.security;

import com.minimax.common.exception.BizException;
import com.minimax.common.result.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * 共享 JWT 鉴权过滤器。
 * 从 application.yml 的 minimax.jwt.* 读取配置（无需单独 JwtProperties 类）。
 * 各业务模块（auth/chat/memory/...）都引用此 filter。
 *
 * 关键设计：
 *  1. access token 短命(30min) + refresh token 长命(7d) 双 token
 *  2. 头缺失 / 格式错误：放行（让下游 SecurityConfig 决定要不要 401）
 *  3. token 无效：直接清空 SecurityContext
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${minimax.jwt.header:Authorization}")
    private String header;

    @Value("${minimax.jwt.prefix:Bearer }")
    private String prefix;

    @Value("${minimax.jwt.secret:minimax-default-secret-please-override-in-production-32+}")
    private String secret;

    @Value("${minimax.jwt.issuer:minimax-platform}")
    private String issuer;

    private SecretKey key() {
        try {
            byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw);
            return Keys.hmacShaKeyFor(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader(header);
        if (h != null && h.startsWith(prefix)) {
            String token = h.substring(prefix.length());
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(key())
                        .requireIssuer(issuer)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
                Long userId = Long.parseLong(claims.getSubject());
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) claims.get("roles");
                List<SimpleGrantedAuthority> auth = roles == null ? List.of()
                        : roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(new AuthenticatedUser(userId, claims.get("uname", String.class)), null, auth);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (Exception e) {
                log.debug("JWT 解析失败: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, resp);
    }

    /** 存放在 principal 中的最小用户信息。 */
    public record AuthenticatedUser(Long id, String username) {}
}
