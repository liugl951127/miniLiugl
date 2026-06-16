package com.minimax.common.tenant;

import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * V3.1: 多租户拦截器
 *
 * 行为:
 *   - 从 JWT 取 userId → 查 sys_user.tenant_id → 设置 TenantContext
 *   - adminLiugl (SUPER_ADMIN 角色) 自动 tenantId=0 (跨租户)
 *   - 其他用户 tenantId 来自数据库, 所有数据按此过滤
 */
@Slf4j
public class TenantInterceptor implements HandlerInterceptor {

    private final TenantResolver resolver;

    public TenantInterceptor(TenantResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
                // 未登录, 清空上下文 (后续 SecurityConfig 会 401)
                TenantContext.clear();
                return true;
            }

            // 查用户的 tenant_id
            TenantResolver.TenantInfo info = resolver.resolve(user.id());
            TenantContext.set(info.tenantId(), info.tenantCode());

            // 跨租户标记
            if (info.superAdmin()) {
                request.setAttribute("minimax.crossTenant", Boolean.TRUE);
            }
            return true;
        } catch (Exception e) {
            log.warn("TenantInterceptor 异常: {}", e.getMessage());
            TenantContext.clear();
            return true;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContext.clear();
    }
}
