package com.bank.dualrecord.controller;

import com.bank.dualrecord.dto.ApiResponse;
import com.bank.dualrecord.security.JwtTokenManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证 Controller
 *
 * <p>修复 DRL-2026-002:JWT 撤销
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "登录/登出/刷新")
public class AuthController {

    private final JwtTokenManager tokenManager;

    /**
     * 登录
     */
    @PostMapping("/login")
    @Operation(summary = "登录(演示)")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest req) {
        // 实际应校验数据库用户
        String token = tokenManager.issue(
            "user_" + req.getUserNo(),
            req.getRole() == null ? "USER" : req.getRole(),
            req.getBranchId()
        );
        log.info("用户登录: userNo={}, role={}", req.getUserNo(), req.getRole());
        return ApiResponse.ok(Map.of(
            "token", token,
            "expiresIn", tokenManager.getExpiration()
        ));
    }

    /**
     * 登出(撤销 token)
     */
    @PostMapping("/logout")
    @Operation(summary = "登出(撤销 token)")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            tokenManager.revoke(token);
        }
        return ApiResponse.ok();
    }

    /**
     * 刷新 token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新 token(轮换 jti)")
    public ApiResponse<Map<String, Object>> refresh(@RequestBody RefreshRequest req) {
        String newToken = tokenManager.refresh(req.getToken());
        return ApiResponse.ok(Map.of(
            "token", newToken,
            "expiresIn", tokenManager.getExpiration()
        ));
    }

    /**
     * 强制下线(管理员)
     */
    @PostMapping("/revoke/{userId}")
    @Operation(summary = "强制下线指定用户")
    public ApiResponse<Void> revokeUser(@PathVariable String userId) {
        tokenManager.revokeAllForUser(userId);
        return ApiResponse.ok();
    }

    @Data
    public static class LoginRequest {
        private String userNo;
        private String password;
        private String role;
        private String branchId;
    }

    @Data
    public static class RefreshRequest {
        private String token;
    }
}
