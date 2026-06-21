package com.minimax.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.minimax.auth.entity.SysUser;
import com.minimax.auth.entity.Tenant;
import com.minimax.auth.service.TenantService;
import com.minimax.common.result.Result;
import com.minimax.common.security.SuperAdminGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V3.1: 多租户 API
 *
 * 端点:
 *   GET    /auth/tenants                列出所有租户 (adminLiugl)
 *   GET    /auth/tenants/{id}           租户详情
 *   POST   /auth/tenants                创建租户 (adminLiugl)
 *   POST   /auth/tenants/{id}/status    启/停 (adminLiugl)
 *   POST   /auth/tenants/{id}/quota     调整配额 (adminLiugl)
 *   DELETE /auth/tenants/{id}           删除 (adminLiugl, 不能删 default)
 *   GET    /auth/tenants/{id}/users     租户下用户
 *   GET    /auth/me/tenant              当前用户租户信息
 */
@Tag(name = "认证授权")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @Operation(summary = "获取当前用户租户信息")
    @GetMapping("/me/tenant")
    public Result<Map<String, Object>> myTenant() {
        // 当前用户租户信息 (从 token 推断)
        Long userId = com.minimax.common.tenant.TenantContext.currentTenantId();
        return Result.ok(Map.of(
                "tenantId", userId,
                "crossTenant", com.minimax.common.tenant.TenantContext.isSuperAdmin()
        ));
    }

    @Operation(summary = "列出所有租户")
    @GetMapping("/tenants")
    public Result<List<Tenant>> list() {
        SuperAdminGuard.requireSuperAdmin();
        return Result.ok(tenantService.listAll());
    }

    @Operation(summary = "获取租户详情")
    @GetMapping("/tenants/{id}")
    public Result<Tenant> get(@PathVariable Long id) {
        SuperAdminGuard.requireSuperAdmin();
        return Result.ok(tenantService.getById(id));
    }

    @Operation(summary = "创建租户")
    @PostMapping("/tenants")
    public Result<Long> create(@RequestBody Map<String, Object> body) {
        SuperAdminGuard.requireSuperAdmin();
        Long id = tenantService.create(
                (String) body.get("code"),
                (String) body.get("name"),
                (String) body.get("plan"),
                body.get("maxUsers") == null ? null : ((Number) body.get("maxUsers")).intValue(),
                body.get("maxModels") == null ? null : ((Number) body.get("maxModels")).intValue(),
                body.get("qpsLimit") == null ? null : ((Number) body.get("qpsLimit")).intValue(),
                body.get("monthlyQuota") == null ? null : ((Number) body.get("monthlyQuota")).longValue(),
                (String) body.get("contactEmail"),
                (String) body.get("remark"));
        return Result.ok(id);
    }

    @Operation(summary = "设置租户状态（启用/停用）")
    @PostMapping("/tenants/{id}/status")
    public Result<Boolean> setStatus(@PathVariable Long id, @RequestParam Integer status) {
        SuperAdminGuard.requireSuperAdmin();
        return Result.ok(tenantService.setStatus(id, status));
    }

    @Operation(summary = "调整租户配额")
    @PostMapping("/tenants/{id}/quota")
    public Result<Boolean> updateQuota(@PathVariable Long id, @RequestParam Long quota) {
        SuperAdminGuard.requireSuperAdmin();
        return Result.ok(tenantService.updateQuota(id, quota));
    }

    @Operation(summary = "删除租户")
    @DeleteMapping("/tenants/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        SuperAdminGuard.requireSuperAdmin();
        return Result.ok(tenantService.delete(id));
    }

    @Operation(summary = "列出租户下所有用户")
    @GetMapping("/tenants/{id}/users")
    public Result<List<Map<String, Object>>> listUsers(@PathVariable Long id) {
        SuperAdminGuard.requireSuperAdmin();
        List<SysUser> users = tenantService.listTenantUsers(id);
        return Result.ok(users.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("email", u.getEmail());
            m.put("status", u.getStatus());
            return m;
        }).toList());
    }
}
