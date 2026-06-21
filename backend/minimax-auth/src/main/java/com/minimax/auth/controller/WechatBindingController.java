package com.minimax.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.minimax.auth.entity.SysUser;
import com.minimax.auth.mapper.SysUserMapper;
import com.minimax.auth.wechat.WechatBindingService;
import com.minimax.common.exception.BizException;
import com.minimax.common.result.Result;
import com.minimax.common.result.ResultCode;
import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 微信绑定管理 Controller (V5).
 *
 * 用户端:
 *   GET  /auth/wechat/binding/me    查自己的绑定
 *   DELETE /auth/wechat/binding/me  解绑
 *
 * 管理端 (adminLiugl):
 *   GET   /auth/admin/wechat/bindings         列出全部绑定
 *   GET   /auth/admin/wechat/find?openid=xx   按 openid 找
 *   POST  /auth/admin/wechat/bind            强制绑定
 *   DELETE /auth/admin/wechat/bind/{userId}  强制解绑
 *
 * @since 2026-06
 */
@Slf4j
@Tag(name = "认证授权")
@RestController
@RequiredArgsConstructor
public class WechatBindingController {

    private final WechatBindingService bindingService;
    private final SysUserMapper userMapper;

    // ================ 用户端 ================

    @Operation(summary = "查询当前用户微信绑定信息")
    @GetMapping("/auth/wechat/binding/me")
    public Result<Map<String, Object>> getMyBinding(@AuthenticationPrincipal AuthenticatedUser principal) {
        return Result.ok(bindingService.getMyBinding(principal.id()));
    }

    @Operation(summary = "当前用户解绑微信")
    @DeleteMapping("/auth/wechat/binding/me")
    public Result<Void> unbindMyself(@AuthenticationPrincipal AuthenticatedUser principal) {
        bindingService.unbindMyself(principal.id());
        return Result.ok();
    }

    // ================ 管理端 (adminLiugl) ================

    private void requireSuperAdmin(AuthenticatedUser p) {
        SysUser u = userMapper.selectById(p.id());
        if (u == null) throw new BizException(ResultCode.UNAUTHORIZED);
        // 简化: 用 username 判断 (实际生产应查 roles)
        if (!"adminLiugl".equals(u.getUsername())) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
    }

    @Operation(summary = "列出所有微信绑定记录（管理员）")
    @GetMapping("/auth/admin/wechat/bindings")
    public Result<List<Map<String, Object>>> listAll(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String keyword) {
        return Result.ok(bindingService.listAll(limit, keyword));
    }

    @Operation(summary = "按 openid 查询绑定记录（管理员）")
    @GetMapping("/auth/admin/wechat/find")
    public Result<Map<String, Object>> findByOpenid(@RequestParam String openid) {
        return Result.ok(bindingService.findByOpenid(openid));
    }

    @Operation(summary = "管理员强制绑定微信")
    @PostMapping("/auth/admin/wechat/bind")
    public Result<Void> bindByAdmin(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @RequestBody Map<String, Object> body) {
        requireSuperAdmin(principal);
        Long userId = ((Number) body.get("userId")).longValue();
        String openid = (String) body.get("openid");
        String unionid = (String) body.get("unionid");
        String nickname = (String) body.get("nickname");
        String avatar = (String) body.get("avatar");
        String appType = (String) body.getOrDefault("appType", "mp");
        bindingService.bindByAdmin(userId, openid, unionid, nickname, avatar, appType, principal.id());
        return Result.ok();
    }

    @Operation(summary = "管理员强制解绑微信")
    @DeleteMapping("/auth/admin/wechat/bind/{userId}")
    public Result<Void> unbindByAdmin(@AuthenticationPrincipal AuthenticatedUser principal,
                                     @PathVariable Long userId) {
        requireSuperAdmin(principal);
        bindingService.unbindByAdmin(userId, principal.id());
        return Result.ok();
    }
}
