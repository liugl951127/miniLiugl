package com.minimax.common.tenant;

/**
 * V3.1: 多租户上下文 (基于 ThreadLocal)
 *
 * 设计:
 *   - tenantId = 0: 平台所有者 (adminLiugl 跨租户, 不受隔离限制)
 *   - tenantId = N: 普通租户, 所有数据按 tenant_id 隔离
 *
 * 流程:
 *   请求 → TenantInterceptor 解析 token → 设置 TenantContext
 *        → 各 Service 用 TenantContext.currentTenantId() 过滤数据
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<String> CODE = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(Long tenantId, String code) {
        CURRENT.set(tenantId);
        CODE.set(code);
    }

    public static Long currentTenantId() {
        Long t = CURRENT.get();
        return t == null ? 0L : t;
    }

    public static String currentTenantCode() {
        return CODE.get();
    }

    public static boolean isSuperAdmin() {
        return currentTenantId() == 0L;
    }

    public static void clear() {
        CURRENT.remove();
        CODE.remove();
    }
}
