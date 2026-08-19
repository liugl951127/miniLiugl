package com.minimax.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.minimax.auth.dto.LoginRequest;
import com.minimax.auth.dto.RefreshRequest;
import com.minimax.auth.dto.RegisterRequest;
import com.minimax.common.audit.Audited;
import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
import com.minimax.auth.service.AuthService;
import com.minimax.auth.service.UserPreferenceService;
import com.minimax.auth.vo.LoginResponse;
import com.minimax.auth.mapper.AuthLoginLogMapper;
import com.minimax.auth.mapper.SysUserMapper;
import com.minimax.auth.entity.SysUser;
import com.minimax.auth.entity.AuthLoginLog;
import com.minimax.common.result.Result;
import com.minimax.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "认证授权")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserPreferenceService preferenceService;
    private final SysUserMapper userMapper;
    private final AuthLoginLogMapper loginLogMapper;

    @Operation(summary = "用户注册")
    @Audited(action = "REGISTER", resourceType = "User")
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest req,
                                          HttpServletRequest http) {
        return Result.ok(authService.register(req, http));
    }

    @Operation(summary = "用户登录")
    @Audited(action = "LOGIN", resourceType = "User")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req,
                                       HttpServletRequest http) {
        return Result.ok(authService.login(req, http));
    }

    @Operation(summary = "刷新访问令牌")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshRequest req,
                                         HttpServletRequest http) {
        return Result.ok(authService.refresh(req.getRefreshToken(), http));
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout(@AuthenticationPrincipal AuthenticatedUser principal,
                               @RequestBody(required = false) RefreshRequest req) {
        Long uid = principal == null ? null : principal.id();
        String rt = req == null ? null : req.getRefreshToken();
        authService.logout(uid, rt);
        return Result.ok();
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<LoginResponse.UserInfo> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return Result.ok(authService.me(principal.id()));
    }

    /**
     * 认证统计 (V6.8.2)：供 admin dashboard 调用。
     * 返回用户总数 + 今日登录次数。
     */
    @Operation(summary = "认证统计（admin 调用）")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        // 总用户数（未删除）
        long totalUsers = userMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeleted, 0)
        );

        // 今日登录次数
        long todayLogins = loginLogMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AuthLoginLog>()
                .ge(AuthLoginLog::getCreatedAt, LocalDate.now().atStartOfDay())
        );

        return Result.ok(Map.of(
            "totalUsers", totalUsers,
            "todayLogins", todayLogins
        ));
    }

    /**
     * 校验 access token 是否有效 (V6.8.9)。
     * 返回 valid + userId + expiresAt，失败抛 401。
     */
    @Operation(summary = "校验 access token 是否有效")
    @GetMapping("/validate")
    public Result<Map<String, Object>> validate(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "令牌无效或已过期");
        }
        return Result.ok(Map.of(
            "valid", true,
            "userId", principal.id(),
            "username", principal.username()
        ));
    }

    // ============== 用户偏好 (V6.8.9) ==============

    @Operation(summary = "获取当前用户偏好")
    @GetMapping("/preferences")
    public Result<Map<String, String>> getPreferences(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal == null) throw new BizException(ResultCode.UNAUTHORIZED);
        var pref = preferenceService.getOrCreate(principal.id());
        return Result.ok(Map.of(
            "theme", pref.getTheme() != null ? pref.getTheme() : "light",
            "language", pref.getLanguage() != null ? pref.getLanguage() : "zh-CN"
        ));
    }

    @Operation(summary = "更新主题偏好")
    @PatchMapping("/preferences/theme")
    public Result<Map<String, String>> updateTheme(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody Map<String, String> body) {
        if (principal == null) throw new BizException(ResultCode.UNAUTHORIZED);
        String theme = body.getOrDefault("theme", "light");
        var pref = preferenceService.updateTheme(principal.id(), theme);
        return Result.ok(Map.of("theme", pref.getTheme()));
    }
}
