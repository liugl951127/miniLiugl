package com.minimax.agent.config;

import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * h2local 沙箱模式: 为所有 API 注入 mock 认证用户。
 * 所有请求会设置 AuthenticatedUser(1L, "sandbox")。
 */
@Slf4j
@Component
@Profile("h2local")
public class AgentH2localMockAuthFilter extends OncePerRequestFilter {

    private static final AuthenticatedUser MOCK_USER = new AuthenticatedUser(1L, "sandbox");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        log.info("[MockAuth] 注入 mock 用户 sandbox (h2local 模式)");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                MOCK_USER, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // h2local 模式下: 拦截所有请求注入 mock 用户
        return false;
    }
}
