package com.minimax.common.security;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 权限服务 (V2.7.9 - 内存实现, 简化)
 *
 * <p>生产应换 Spring Security + 数据库. 这里用内存 Map 演示.</p>
 *
 * <h3>角色 -> 权限映射</h3>
 * <pre>
 *   SUPER_ADMIN: 全部权限
 *   ADMIN: 除 super.* 外的所有权限
 *   USER: ai.use / chat.read / kg.read
 *   GUEST: 只读基础
 * </pre>
 */
public class PermissionService {

    private static final Map<String, Set<String>> ROLE_PERMS = new ConcurrentHashMap<>();

    static {
        // 超级管理员
        ROLE_PERMS.put("SUPER_ADMIN", Set.of(
                "*"
        ));
        // 管理员
        ROLE_PERMS.put("ADMIN", Set.of(
                "ai.use", "ai.admin", "ai.tool.*", "user.manage", "system.config",
                "alert.*", "audit.read", "doc.parse", "training.run"
        ));
        // 普通用户
        ROLE_PERMS.put("USER", Set.of(
                "ai.use", "ai.chat", "ai.image", "ai.workflow",
                "doc.parse", "training.view"
        ));
        // 访客
        ROLE_PERMS.put("GUEST", Set.of(
                "ai.chat", "ai.read"
        ));
    }

    /**
     * 是否有某权限
     */
    public static boolean has(String role, String perm) {
        if (role == null) return false;
        Set<String> perms = ROLE_PERMS.getOrDefault(role, Set.of());
        // 通配符 *
        if (perms.contains("*")) return true;
        // 精确匹配
        if (perms.contains(perm)) return true;
        // 通配符匹配 (ai.*, alert.*)
        for (String p : perms) {
            if (p.endsWith(".*")) {
                String prefix = p.substring(0, p.length() - 2);
                if (perm.startsWith(prefix + ".")) return true;
            }
        }
        return false;
    }

    /**
     * 检查多个权限 (任一通过)
     */
    public static boolean hasAny(String role, String... perms) {
        return Arrays.stream(perms).anyMatch(p -> has(role, p));
    }

    /**
     * 检查多个权限 (全部需要)
     */
    public static boolean hasAll(String role, String... perms) {
        return Arrays.stream(perms).allMatch(p -> has(role, p));
    }

    /**
     * 获取某角色所有权限
     */
    public static Set<String> permissionsOf(String role) {
        return ROLE_PERMS.getOrDefault(role, Set.of());
    }

    /**
     * 列出所有角色
     */
    public static List<String> listRoles() {
        return List.copyOf(ROLE_PERMS.keySet());
    }
}
