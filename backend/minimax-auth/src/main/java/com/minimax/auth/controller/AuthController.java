package com.minimax.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.minimax.auth.dto.LoginRequest;
import com.minimax.auth.dto.RefreshRequest;
import com.minimax.auth.dto.RegisterRequest;
import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
import com.minimax.auth.service.AuthService;
import com.minimax.auth.vo.LoginResponse;
import com.minimax.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证授权")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest req,
                                          HttpServletRequest http) {
        return Result.ok(authService.register(req, http));
    }

    @Operation(summary = "用户登录")
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
}
