package com.minimax.common.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

/**
 * V3.1: 租户查询辅助工具
 * 
 * SUPER_ADMIN (TenantContext.isSuperAdmin()=true): 不过滤，看到所有租户数据
 * 普通用户: 只看到当前租户的数据
 */
public final class TenantQueryHelper {

    private TenantQueryHelper() {}

    /** 为查询 wrapper 添加租户过滤条件（SUPER_ADMIN 跳过） */
    public static <T> void applyTenantFilter(LambdaQueryWrapper<T> wrapper, SFunction<T, Long> field) {
        if (TenantContext.isSuperAdmin()) return;
        Long tid = TenantContext.currentTenantId();
        if (tid != null && tid > 0) {
            wrapper.eq(field, tid);
        }
    }

    /** 为更新 wrapper 添加租户过滤条件（SUPER_ADMIN 跳过） */
    public static <T> void applyTenantFilter(LambdaUpdateWrapper<T> wrapper, SFunction<T, Long> field) {
        if (TenantContext.isSuperAdmin()) return;
        Long tid = TenantContext.currentTenantId();
        if (tid != null && tid > 0) {
            wrapper.eq(field, tid);
        }
    }

    /** 如果当前用户是 SUPER_ADMIN，强制覆盖 tenantId 为其真实所属租户（用于写操作防串租户） */
    public static Long resolveTenantIdForWrite(Long userTenantId) {
        // SUPER_ADMIN 做管理操作时保持原有 tenantId（跨租户）
        if (TenantContext.isSuperAdmin()) return null; // null 表示不强制约束
        return userTenantId;
    }
}
