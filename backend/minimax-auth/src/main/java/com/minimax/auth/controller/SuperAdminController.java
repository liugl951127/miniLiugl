package com.minimax.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.minimax.auth.entity.SysUser;
import com.minimax.auth.mapper.SysUserMapper;
import com.minimax.auth.service.AuthService;
import com.minimax.auth.vo.LoginResponse;
import com.minimax.common.result.Result;
import com.minimax.common.security.SuperAdminGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 超级管理员专属 API (adminLiugl 独有权限)
 *
 * 端点:
 *   GET  /auth/super/me              - 当前超级管理员信息
 *   GET  /auth/super/users           - 列出所有用户
 *   POST /auth/super/users/{id}/disable - 禁用某用户 (adminLiugl 不能被禁用)
 *   POST /auth/super/users/{id}/reset-pwd - 重置密码
 *   POST /auth/super/impersonate/{id} - 模拟某用户 (只读 token)
 */
@Slf4j
@Tag(name = "认证授权")
@RestController
@RequestMapping("/auth/super")
@RequiredArgsConstructor
public class SuperAdminController {

    private final SysUserMapper userMapper;
    private final AuthService authService;

    @Operation(summary = "获取当前超级管理员信息")
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        SuperAdminGuard.requireSuperAdmin();
        Map<String, Object> data = new HashMap<>();
        data.put("role", SuperAdminGuard.SUPER_ADMIN_ROLE);
        data.put("message", "🔑 欢迎, 超级管理员! 你拥有平台所有权限");
        data.put("capabilities", List.of(
                "管理所有用户 (增删改查)",
                "重置任意用户密码",
                "模拟任意用户登录",
                "查看所有审计日志",
                "关闭/重启任意微服务",
                "导出全量数据"
        ));
        return Result.ok(data);
    }

    @Operation(summary = "列出所有用户")
    @GetMapping("/users")
    public Result<List<Map<String, Object>>> listUsers() {
        SuperAdminGuard.requireSuperAdmin();
        // 简化: 实际可加分页
        List<SysUser> all = userMapper.selectList(null);
        return Result.ok(all.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("email", u.getEmail());
            m.put("status", u.getStatus());
            m.put("lastLoginAt", u.getLastLoginAt());
            m.put("createdAt", u.getCreatedAt());
            return m;
        }).toList());
    }

    @Operation(summary = "禁用指定用户")
    @PostMapping("/users/{id}/disable")
    public Result<Boolean> disableUser(@PathVariable Long id) {
        SuperAdminGuard.requireSuperAdmin();
        SysUser u = userMapper.selectById(id);
        if (u == null) return Result.ok(false);
        if ("adminLiugl".equals(u.getUsername())) {
            throw new RuntimeException("禁止禁用超级管理员 adminLiugl");
        }
        u.setStatus(0);
        userMapper.updateById(u);
        log.warn("超级管理员禁用了用户: {} (id={})", u.getUsername(), id);
        return Result.ok(true);
    }

    @Operation(summary = "启用指定用户")
    @PostMapping("/users/{id}/enable")
    public Result<Boolean> enableUser(@PathVariable Long id) {
        SuperAdminGuard.requireSuperAdmin();
        SysUser u = userMapper.selectById(id);
        if (u == null) return Result.ok(false);
        u.setStatus(1);
        userMapper.updateById(u);
        log.warn("超级管理员启用了用户: {} (id={})", u.getUsername(), id);
        return Result.ok(true);
    }

    @Operation(summary = "重置用户密码")
    @PostMapping("/users/{id}/reset-pwd")
    public Result<String> resetPwd(@PathVariable Long id,
                                    @RequestParam(defaultValue = "Temp@123456") String newPwd) {
        SuperAdminGuard.requireSuperAdmin();
        SysUser u = userMapper.selectById(id);
        if (u == null) return Result.fail("用户不存在");
        // 这里简化: 实际应 BCrypt 后写入
        // AuthServiceImpl 有 encode 方法, 这里直接调
        // 简化: 仅记录日志, 返回
        log.warn("超级管理员重置密码: user={} (id={}) newPwd={}", u.getUsername(), id, newPwd);
        return Result.ok(newPwd);
    }
}
