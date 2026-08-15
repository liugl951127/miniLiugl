package com.minimax.rag.config;

import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * h2local 沙箱模式：注入一个 mock 认证用户，避免 @AuthenticationPrincipal user 为 null。
 */
@Component
@Profile("h2local")
public class H2localMockAuthFilter extends OncePerRequestFilter {

    private static final AuthenticatedUser MOCK_USER = new AuthenticatedUser(0L, "sandbox");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                MOCK_USER, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }
}
