package com.minimax.common.tenant;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;

/**
 * V3.1: 租户解析器接口
 *  - minimax-auth 提供默认实现 (查 sys_user)
 *  - 其他模块也可注入自定义实现
 */
public interface TenantResolver {

    TenantInfo resolve(Long userId);

    record TenantInfo(Long tenantId, String tenantCode, boolean superAdmin) {}

    /**
     * 默认实现: 从 SecurityContext 取角色判断超级管理员
     * tenantId = userId % 10 (沙箱/演示用, 真实应查 DB)
     */
    static TenantResolver defaultResolver() {
        return userId -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) return new TenantInfo(0L, "default", false);
            Collection<? extends GrantedAuthority> auths = auth.getAuthorities();
            boolean superAdmin = false;
            for (GrantedAuthority a : auths) {
                if ("ROLE_SUPER_ADMIN".equals(a.getAuthority())) {
                    superAdmin = true;
                    break;
                }
            }
            if (superAdmin) return new TenantInfo(0L, "platform", true);
            return new TenantInfo(1L, "default", false);
        };
    }
}
