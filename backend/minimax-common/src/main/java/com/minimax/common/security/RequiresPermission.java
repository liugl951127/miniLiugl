package com.minimax.common.security;

import java.lang.annotation.*;

/**
 * 权限注解 (V2.7.9)
 *
 * 用法: {@code @RequiresPermission("ai.admin")}
 * 支持数组: {@code @RequiresPermission({"ai.use", "ai.admin"})}
 * 默认模式: 全部需要 (ALL)
 * 配合 PermissionAspect 拦截
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {
    String[] value();
    Mode mode() default Mode.ALL;

    enum Mode { ALL, ANY }
}
