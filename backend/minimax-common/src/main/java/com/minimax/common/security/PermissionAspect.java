package com.minimax.common.security;

import com.minimax.common.exception.BizException;
import com.minimax.common.security.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 权限拦截 AOP (V2.7.9)
 *
 * <p>从 SecurityContext 取当前用户角色, 校验 @RequiresPermission</p>
 */
@Slf4j
@Aspect
@Component
@Order(1)
public class PermissionAspect {

    @Around("@annotation(com.minimax.common.security.RequiresPermission) " +
            "|| @within(com.minimax.common.security.RequiresPermission)")
    public Object check(ProceedingJoinPoint pjp) throws Throwable {
        // 1. 拿注解 (方法 > 类)
        RequiresPermission ann = null;
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method m = sig.getMethod();
        if (m.isAnnotationPresent(RequiresPermission.class)) {
            ann = m.getAnnotation(RequiresPermission.class);
        } else if (pjp.getTarget().getClass().isAnnotationPresent(RequiresPermission.class)) {
            ann = pjp.getTarget().getClass().getAnnotation(RequiresPermission.class);
        }

        if (ann == null) return pjp.proceed();

        // 2. 取当前角色
        String role = SecurityContext.currentRole();
        if (role == null) {
            log.warn("Permission denied: no role in context (method={})", m.getName());
            throw new BizException(401, "未登录");
        }

        // 3. 校验
        boolean ok = switch (ann.mode()) {
            case ALL -> PermissionService.hasAll(role, ann.value());
            case ANY -> PermissionService.hasAny(role, ann.value());
        };

        if (!ok) {
            log.warn("Permission denied: role={} need={} (method={})",
                    role, String.join(",", ann.value()), m.getName());
            throw new BizException(403, "无权限: " + String.join(",", ann.value()));
        }

        return pjp.proceed();
    }
}
