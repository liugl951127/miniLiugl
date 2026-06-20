package com.minimax.auth.controller;

import com.minimax.auth.entity.OAuthAppConfig;
import com.minimax.auth.oauth.OAuthPlatformClient;
import com.minimax.auth.oauth.OAuthPlatformService;
import com.minimax.auth.vo.LoginResponse;
import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 跨平台 OAuth Controller (V5.2).
 *
 * H5 / 移动端通用登录入口:
 *   GET  /auth/oauth/{platform}/authorize-url?appType=&redirect=&state=
 *      → 返回授权 URL, 前端跳转
 *   GET  /auth/oauth/{platform}/callback?code=&state=&appType=
 *      → 平台回调, 处理 code
 *   POST /auth/oauth/{platform}/login {code, appType, redirectUri}
 *      → 移动端直接传 code (小程序/公众号 OAuth 已处理回调)
 *   GET  /auth/oauth/{platform}/config?appType=
 *      → 查应用配置 (前端展示)
 *
 * @since 2026-06
 */
@Slf4j
@RestController
@RequestMapping("/auth/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthPlatformService oauthService;

    /**
     * 生成授权 URL (前端跳转用)
     */
    @GetMapping("/{platform}/authorize-url")
    public Result<Map<String, Object>> authorizeUrl(
            @PathVariable String platform,
            @RequestParam(defaultValue = "web") String appType,
            @RequestParam(required = false) String redirectUri,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String scope) {
        OAuthAppConfig cfg = oauthService.getConfig(platform, appType);
        String appId = cfg != null ? cfg.getAppId() : "PLACEHOLDER_" + platform.toUpperCase();
        OAuthPlatformClient client = oauthService.getClient(platform);
        String url = client.buildAuthorizeUrl(appId, redirectUri, state, scope);
        return Result.ok(Map.of(
                "platform", platform,
                "appType", appType,
                "authorizeUrl", url,
                "mock", client.isMock(appId, cfg != null ? cfg.getAppSecret() : "")
        ));
    }

    /**
     * 跨平台登录 (POST 直接传 code)
     * 移动端/小程序/公众号 OAuth 已在客户端处理回调, 直接 POST code 过来即可.
     */
    @PostMapping("/{platform}/login")
    public Result<LoginResponse> login(@PathVariable String platform,
                                       @RequestBody Map<String, Object> body) {
        String code = (String) body.get("code");
        String appType = (String) body.getOrDefault("appType", "web");
        String redirectUri = (String) body.get("redirectUri");
        if (code == null || code.isBlank()) {
            return Result.fail(4001, "code 不能为空");
        }
        LoginResponse resp = oauthService.login(platform, appType, code, redirectUri);
        return Result.ok(resp);
    }

    /**
     * 平台回调 (GET, 类似微信回调)
     */
    @GetMapping("/{platform}/callback")
    public Result<LoginResponse> callback(@PathVariable String platform,
                                          @RequestParam String code,
                                          @RequestParam(required = false) String state,
                                          @RequestParam(defaultValue = "web") String appType,
                                          @RequestParam(required = false) String redirectUri) {
        LoginResponse resp = oauthService.login(platform, appType, code, redirectUri);
        return Result.ok(resp);
    }

    /**
     * 查应用配置
     */
    @GetMapping("/{platform}/config")
    public Result<Map<String, Object>> getConfig(@PathVariable String platform,
                                                 @RequestParam(defaultValue = "web") String appType) {
        OAuthAppConfig cfg = oauthService.getConfig(platform, appType);
        if (cfg == null) {
            return Result.ok(Map.of(
                    "platform", platform,
                    "appType", appType,
                    "configured", false,
                    "enabled", false
            ));
        }
        return Result.ok(Map.of(
                "platform", cfg.getPlatform(),
                "appType", cfg.getAppType(),
                "appId", cfg.getAppId(),
                "redirectUri", cfg.getRedirectUri(),
                "scopes", cfg.getScopes(),
                "enabled", cfg.getEnabled() == 1,
                "configured", !cfg.getAppId().startsWith("PLACEHOLDER"),
                "mock", cfg.getAppId().startsWith("PLACEHOLDER")
        ));
    }

    /**
     * 列出所有平台配置 (admin)
     */
    @GetMapping("/configs")
    public Result<List<Map<String, Object>>> listConfigs() {
        List<OAuthAppConfig> all = oauthService.getConfigMapper() == null ? List.of()
                : oauthService.getConfigMapper().selectList(null);
        List<Map<String, Object>> out = new ArrayList<>();
        for (OAuthAppConfig cfg : all) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", cfg.getId());
            row.put("platform", cfg.getPlatform());
            row.put("appType", cfg.getAppType());
            row.put("appId", cfg.getAppId());
            row.put("redirectUri", cfg.getRedirectUri());
            row.put("enabled", cfg.getEnabled() == 1);
            row.put("mock", cfg.getAppId() != null && cfg.getAppId().startsWith("PLACEHOLDER"));
            out.add(row);
        }
        return Result.ok(out);
    }
}