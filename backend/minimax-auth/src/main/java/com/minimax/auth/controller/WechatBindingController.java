package com.minimax.auth.controller;

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
@RestController
@RequiredArgsConstructor
public class WechatBindingController {

    private final WechatBindingService bindingService;
    private final SysUserMapper userMapper;

    // ================ 用户端 ================

    @GetMapping("/auth/wechat/binding/me")
    public Result<Map<String, Object>> getMyBinding(@AuthenticationPrincipal AuthenticatedUser principal) {
        return Result.ok(bindingService.getMyBinding(principal.id()));
    }

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

    @GetMapping("/auth/admin/wechat/bindings")
    public Result<List<Map<String, Object>>> listAll(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String keyword) {
        return Result.ok(bindingService.listAll(limit, keyword));
    }

    @GetMapping("/auth/admin/wechat/find")
    public Result<Map<String, Object>> findByOpenid(@RequestParam String openid) {
        return Result.ok(bindingService.findByOpenid(openid));
    }

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

    @DeleteMapping("/auth/admin/wechat/bind/{userId}")
    public Result<Void> unbindByAdmin(@AuthenticationPrincipal AuthenticatedUser principal,
                                     @PathVariable Long userId) {
        requireSuperAdmin(principal);
        bindingService.unbindByAdmin(userId, principal.id());
        return Result.ok();
    }
}
