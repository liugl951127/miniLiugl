package com.minimax.agent.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 请求拦截器：自动从当前 HTTP 请求上下文获取 JWT token 并透传到下游服务。
 *
 * <p>解决问题：agent → pipeline 等 Feign 调用返回 401（未登录或登录已过期）</p>
 *
 * <p>优先级：
 * <ol>
 *   <li>当前请求的 Authorization 头（JWT token）</li>
 *   <li>当前请求的 X-User-Id 头</li>
 * </ol>
 *
 * @since 2026-08-23
 */
@Slf4j
@Component
public class FeignAuthInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }
            HttpServletRequest request = attrs.getRequest();

            // 透传 Authorization 头（JWT token）
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && !authHeader.isBlank()) {
                template.header("Authorization", authHeader);
            }

            // 透传 X-User-Id 头
            String userId = request.getHeader("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                template.header("X-User-Id", userId);
            }
        } catch (Exception e) {
            // 非 HTTP 请求上下文（如定时任务），忽略
            log.debug("[FeignAuth] 无法获取请求上下文: {}", e.getMessage());
        }
    }
}
