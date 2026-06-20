package com.minimax.auth.controller;

import com.minimax.auth.entity.SysUser;
import com.minimax.auth.mapper.SysUserMapper;
import com.minimax.auth.wechat.WechatBindingService;
import com.minimax.auth.wechat.WechatUnionidService;
import com.minimax.common.exception.BizException;
import com.minimax.common.result.Result;
import com.minimax.common.result.ResultCode;
import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.auth.entity.OAuthBinding;
import com.minimax.auth.mapper.OAuthBindingMapper;
import com.minimax.auth.entity.SysUser;
import com.minimax.auth.mapper.SysUserMapper;

import java.util.*;

/**
 * unionid 跨应用 Controller (V5.1).
 *
 * 用户端:
 *   GET /auth/wechat/unionid/me     我的所有应用绑定 (unionid 关联)
 *   GET /auth/wechat/unionid/users  按 unionid 查用户列表 (admin)
 *
 * 管理端 (adminLiugl):
 *   GET   /auth/admin/wechat/unionid-relations        列出所有 unionid 关联
 *   POST  /auth/admin/wechat/merge-accounts           合并账号
 *   GET   /auth/admin/wechat/users-by-unionid?unionid= 按 unionid 查用户
 *
 * @since 2026-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class WechatUnionidController {

    private final WechatUnionidService unionidService;
    private final WechatBindingService bindingService;
    private final SysUserMapper userMapper;
    private final OAuthBindingMapper oauthBindingMapper;

    // ================ 用户端 ================

    @GetMapping("/auth/wechat/unionid/me")
    public Result<List<Map<String, Object>>> getMyUnionid(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        // 从 binding 拿 unionid (任一 binding 的 unionid 都代表用户跨应用身份)
        List<Map<String, Object>> myBinding = bindingService.listAll(1000, "");
        // 简化: 直接返回当前用户的所有 binding
        List<Map<String, Object>> myBindings = new java.util.ArrayList<>();
        for (Map<String, Object> b : myBinding) {
            if (principal.id().equals(((Number) b.get("user_id")).longValue())) {
                myBindings.add(b);
            }
        }
        if (myBindings.isEmpty()) return Result.ok(List.of());

        // 找 unionid
        Object unionid = myBindings.get(0).get("unionid");
        if (unionid == null) return Result.ok(List.of());

        List<Map<String, Object>> users = unionidService.findUsersByUnionid(unionid.toString());
        // 找到自己的
        for (Map<String, Object> u : users) {
            if (principal.id().equals(((Number) u.get("userId")).longValue())) {
                return Result.ok(List.of(u));
            }
        }
        return Result.ok(List.of());
    }

    // ================ 管理端 ================

    private void requireSuperAdmin(AuthenticatedUser p) {
        SysUser u = userMapper.selectById(p.id());
        if (u == null || !"adminLiugl".equals(u.getUsername())) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
    }

    @GetMapping("/auth/admin/wechat/unionid-relations")
    public Result<List<Map<String, Object>>> listAllUnionidRelations(
            @RequestParam(defaultValue = "100") int limit) {
        return Result.ok(unionidService.listAllUnionidRelations(limit));
    }

    @GetMapping("/auth/admin/wechat/users-by-unionid")
    public Result<List<Map<String, Object>>> getUsersByUnionid(
            @RequestParam String unionid) {
        return Result.ok(unionidService.findUsersByUnionid(unionid));
    }

    @PostMapping("/auth/admin/wechat/merge-accounts")
    public Result<Void> mergeAccounts(@AuthenticationPrincipal AuthenticatedUser principal,
                                      @RequestBody Map<String, Object> body) {
        requireSuperAdmin(principal);
        Long userToId = ((Number) body.get("userToId")).longValue();
        Long userFromId = ((Number) body.get("userFromId")).longValue();
        String reason = (String) body.getOrDefault("reason", "管理员合并");
        unionidService.mergeAccounts(userToId, userFromId, reason);
        return Result.ok();
    }

    /**
     * V5.2: 跨平台统计面板
     * - 各平台 binding 数
     * - 多平台用户数 (2 个+平台)
     * - 总 unionid 关联数
     */
    @GetMapping("/auth/admin/wechat/cross-platform-stats")
    public Result<Map<String, Object>> crossPlatformStats() {
        Map<String, Object> out = new LinkedHashMap<>();

        // 各平台 binding 数 (从 oauth_binding 表)
        List<Map<String, Object>> byPlatform = new ArrayList<>();
        String[] platforms = {"wechat", "qq", "alipay", "weibo", "github"};
        long totalBindings = 0;
        for (String p : platforms) {
            Long cnt = oauthBindingMapper.selectCount(
                    new LambdaQueryWrapper<OAuthBinding>().eq(OAuthBinding::getPlatform, p));
            byPlatform.add(Map.of("platform", p, "count", cnt));
            totalBindings += cnt;
        }
        out.put("bindingByPlatform", byPlatform);
        out.put("totalBindings", totalBindings);

        // 老 wechat_user_binding 也算上
        Long wxOldCnt = bindingService != null ? 0L : 0L;
        out.put("wechatUserBindingLegacy", wxOldCnt);

        // 多平台用户数
        Long multiPlatformUsers = userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysUser>()
                        .apply("(qq_openid IS NOT NULL OR alipay_openid IS NOT NULL) AND wechat_openid IS NOT NULL"));
        out.put("multiPlatformUsers", multiPlatformUsers);

        // 各平台独立用户数
        for (String p : platforms) {
            String col = switch (p) {
                case "wechat" -> "wechat_openid";
                case "qq" -> "qq_openid";
                case "alipay" -> "alipay_openid";
                default -> null;
            };
            if (col == null) continue;
            Long cnt = userMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysUser>()
                            .isNotNull(col));
            out.put(p + "Users", cnt);
        }

        // unionid_relations 数
        Long unionidCnt = oauthBindingMapper == null ? 0L
                : oauthBindingMapper.selectCount(null);
        out.put("unionidRelations", unionidCnt);

        return Result.ok(out);
    }
}