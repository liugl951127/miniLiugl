package com.minimax.auth.security;

import com.minimax.auth.jwt.JwtProperties;
import com.minimax.auth.jwt.JwtTokenProvider;
import com.minimax.common.exception.BizException;
import com.minimax.common.result.ResultCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 过滤器：从 Authorization 头解析 token，写入 SecurityContext。
 * - 头缺失 / 格式错误：放行（让下游 SecurityConfig 决定要不要 401）
 * - token 无效：直接 401，不再放行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwt;
    private final JwtProperties props;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader(props.getHeader());
        if (header != null && header.startsWith(props.getPrefix())) {
            String token = header.substring(props.getPrefix().length());
            try {
                Claims claims = jwt.parse(token);
                Long userId = jwt.extractUserId(claims);
                String username = String.valueOf(claims.get("uname"));
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) claims.get("roles");
                List<SimpleGrantedAuthority> auth = roles == null ? List.of()
                        : roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(new AuthenticatedUser(userId, username), null, auth);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (BizException e) {
                SecurityContextHolder.clearContext();
                // 401 直接交给全局异常 / 入口点处理
                throw e;
            } catch (Exception e) {
                log.debug("JWT 解析异常: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, resp);
    }

    /** 存放在 principal 中的最小用户信息，避免 Spring 把整个 UserDetails 塞进 session。 */
    public record AuthenticatedUser(Long id, String username) {}
}
