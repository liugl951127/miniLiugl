package com.minimax.ai.controller;

import com.minimax.common.result.Result;
import com.minimax.common.security.PermissionService;
import com.minimax.common.security.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 权限 API (V2.7.9)
 *
 * 端点:
 *   GET  /api/ai/permission/me        当前用户权限
 *   GET  /api/ai/permission/roles     所有角色 + 权限映射
 *   POST /api/ai/permission/check     校验某权限
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/permission")
@RequiredArgsConstructor
public class PermissionController {

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        String role = SecurityContext.currentRole();
        if (role == null) return Result.fail("未登录");
        Set<String> perms = PermissionService.permissionsOf(role);
        return Result.ok(Map.of(
                "userId", SecurityContext.currentUserId(),
                "username", SecurityContext.currentUsername(),
                "role", role,
                "tenant", String.valueOf(SecurityContext.currentTenant()),
                "permissions", perms
        ));
    }

    @GetMapping("/roles")
    public Result<List<Map<String, Object>>> roles() {
        return Result.ok(PermissionService.listRoles().stream()
                .map(r -> Map.<String, Object>of(
                        "role", r,
                        "permissions", PermissionService.permissionsOf(r)
                ))
                .toList());
    }

    @PostMapping("/check")
    public Result<Map<String, Object>> check(@RequestBody Map<String, Object> req) {
        String role = (String) req.getOrDefault("role", SecurityContext.currentRole());
        @SuppressWarnings("unchecked")
        List<String> perms = (List<String>) req.getOrDefault("permissions", List.of());
        if (role == null) return Result.fail("role 必填");

        Map<String, Boolean> result = new LinkedHashMap<>();
        for (String p : perms) {
            result.put(p, PermissionService.has(role, p));
        }
        return Result.ok(Map.of("role", role, "results", result));
    }
}
