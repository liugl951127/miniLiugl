package com.minimax.common.security;

/**
 * 安全上下文 (V2.7.9 - ThreadLocal)
 *
 * <p>通过 JwtFilter 注入, 供 PermissionAspect / Controller 使用</p>
 */
public class SecurityContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

    public static void set(Long userId, String username, String role, String tenant) {
        USER_ID.set(userId);
        USERNAME.set(username);
        ROLE.set(role);
        TENANT.set(tenant);
    }

    public static Long currentUserId() { return USER_ID.get(); }
    public static String currentUsername() { return USERNAME.get(); }
    public static String currentRole() { return ROLE.get(); }
    public static String currentTenant() { return TENANT.get(); }

    public static void clear() {
        USER_ID.remove();
        USERNAME.remove();
        ROLE.remove();
        TENANT.remove();
    }

    public static boolean isLoggedIn() { return USER_ID.get() != null; }

    public static boolean isSuperAdmin() { return "SUPER_ADMIN".equals(ROLE.get()); }

    public static boolean isAdmin() { return "ADMIN".equals(ROLE.get()) || isSuperAdmin(); }
}
