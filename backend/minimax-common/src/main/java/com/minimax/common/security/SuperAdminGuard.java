package com.minimax.common.security;

import com.minimax.common.exception.BizException;
import com.minimax.common.result.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;

/**
 * V3: 超级管理员 (adminLiugl) 权限检查工具
 *
 * 使用场景:
 *   - AdminController 删 user
 *   - MonitorController 改告警
 *   - 任何敏感操作
 *
 * 设计:
 *   - adminLiugl 拥有 ROLE_SUPER_ADMIN, 跳过所有业务权限检查
 *   - 普通 ADMIN 也可做大多数操作, 但不能管理 adminLiugl 自身
 */
public final class SuperAdminGuard {

    public static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private SuperAdminGuard() {}

    /** 当前用户是否超级管理员 */
    public static boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return hasRole(auth.getAuthorities(), SUPER_ADMIN_ROLE);
    }

    /** 强制要求超级管理员 (否则抛 BizException 403) */
    public static void requireSuperAdmin() {
        if (!isSuperAdmin()) {
            throw new BizException(ResultCode.FORBIDDEN, "需要超级管理员 (adminLiugl) 权限");
        }
    }

    /** 当前用户是否普通 ADMIN 或 SUPER_ADMIN */
    public static boolean isAdminOrAbove() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        Collection<? extends GrantedAuthority> auths = auth.getAuthorities();
        return hasRole(auths, SUPER_ADMIN_ROLE) || hasRole(auths, "ADMIN");
    }

    private static boolean hasRole(Collection<? extends GrantedAuthority> auths, String role) {
        if (auths == null) return false;
        String target = "ROLE_" + role;
        for (GrantedAuthority a : auths) {
            if (target.equals(a.getAuthority())) return true;
        }
        return false;
    }
}
