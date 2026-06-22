package com.minimax.auth.controller;

import com.minimax.auth.dto.CreateApiKeyRequest;
import com.minimax.auth.service.UserApiKeyService;
import com.minimax.auth.vo.ApiKeyResponse;
import com.minimax.common.result.Result;
import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户 API Key 控制器 (V5.33 Day 18).
 *
 * 端点:
 *   GET    /auth/apikeys           — 列出我的 Key
 *   POST   /auth/apikeys           — 创建 Key（返回 rawKey 一次）
 *   PATCH  /auth/apikeys/{id}/toggle — 禁用/启用
 *   DELETE /auth/apikeys/{id}      — 删除 Key
 *   POST   /auth/apikeys/{id}/rotate — 轮换 Key（删旧创新）
 */
@Tag(name = "用户 API Key")
@RestController
@RequestMapping("/auth/apikeys")
@RequiredArgsConstructor
public class UserApiKeyController {

    private final UserApiKeyService apiKeyService;

    @Operation(summary = "列出我的 API Key")
    @GetMapping
    public Result<List<ApiKeyResponse>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return Result.ok(apiKeyService.listKeys(user.getUserId()));
    }

    @Operation(summary = "创建 API Key（返回原始 Key，仅此次可见）")
    @PostMapping
    public Result<ApiKeyResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
                                          @Valid @RequestBody(required = false) CreateApiKeyRequest req) {
        if (req == null) req = new CreateApiKeyRequest();
        return Result.ok(apiKeyService.createKey(user.getUserId(), req));
    }

    @Operation(summary = "禁用/启用 Key")
    @PatchMapping("/{id}/toggle")
    public Result<Void> toggle(@AuthenticationPrincipal AuthenticatedUser user,
                                @PathVariable Long id,
                                @RequestParam boolean enable) {
        apiKeyService.toggleStatus(user.getUserId(), id, enable);
        return Result.ok();
    }

    @Operation(summary = "删除 API Key")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@AuthenticationPrincipal AuthenticatedUser user,
                                @PathVariable Long id) {
        apiKeyService.deleteKey(user.getUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "轮换 Key（删除旧 Key 并创建新 Key）")
    @PostMapping("/{id}/rotate")
    public Result<ApiKeyResponse> rotate(@AuthenticationPrincipal AuthenticatedUser user,
                                          @PathVariable Long id,
                                          @RequestBody(required = false) CreateApiKeyRequest req) {
        if (req == null) req = new CreateApiKeyRequest();
        return Result.ok(apiKeyService.rotateKey(user.getUserId(), id, req));
    }
}
