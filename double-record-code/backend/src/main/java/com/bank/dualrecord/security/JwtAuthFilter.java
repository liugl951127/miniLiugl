package com.bank.dualrecord.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT 认证 Filter - 集成撤销机制
 *
 * <p>修复 DRL-2026-002:Token 撤销检查
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenManager tokenManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        String token = extractToken(request);
        if (StringUtils.hasText(token)) {
            try {
                Claims claims = tokenManager.parse(token);
                String jti = claims.getId();
                String userId = claims.getSubject();

                // 1. 校验 token 是否被撤销
                if (jti != null && tokenManager.isRevoked(jti)) {
                    log.debug("JWT 已被撤销: jti={}", jti);
                    chain.doFilter(request, response);
                    return;
                }

                // 2. 校验用户是否被强制下线
                if (userId != null && tokenManager.isUserRevoked(userId)) {
                    log.debug("用户已被强制下线: userId={}", userId);
                    chain.doFilter(request, response);
                    return;
                }

                // 3. 通过认证
                String role = claims.get("role", String.class);
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + (role == null ? "USER" : role))
                );
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, token, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException e) {
                log.debug("JWT 解析失败: {}", e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest req) {
        String bearer = req.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
