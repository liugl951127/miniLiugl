package com.minimax.auth.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝 OAuth 客户端 (V5.2).
 *
 * 流程 (网页应用):
 *   1. GET https://openauth.alipay.com/oauth2/publicAppAuthorize.htm?app_id=&scope=auth_user&redirect_uri=
 *      → 用户授权 → 支付宝重定向到 redirect_uri?auth_code=xxx
 *   2. POST https://openapi.alipay.com/gateway.do (应用授权令牌)
 *      → access_token + user_id
 *   3. POST https://openapi.alipay.com/gateway.do (用户信息查询)
 *      → nick_name + avatar + ...
 *
 * 注: 支付宝用 user_id 而非 openid, 唯一标识.
 *
 * @since 2026-06
 */
@Slf4j
@Component
public class AlipayOAuthClient implements OAuthPlatformClient {

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    @Override
    public String platform() { return "alipay"; }

    @Override
    public Map<String, Object> code2Token(String appId, String appSecret, String code, String redirectUri) {
        if (isMock(appId, appSecret)) {
            return Map.of(
                    "access_token", "mock_alipay_at_" + System.currentTimeMillis(),
                    "expires_in", 7200,
                    "refresh_token", "mock_alipay_rt_" + System.currentTimeMillis(),
                    "openid", "mock_alipay_userid_" + code.substring(0, Math.min(8, code.length())),
                    "unionid", "mock_alipay_userid_" + code.substring(0, Math.min(8, code.length()))
            );
        }
        // 真实 API 走 gateway.do + 签名, 复杂签名逻辑略 (生产需要 alipay-sdk-java)
        // 这里返回错误提示使用 SDK
        Map<String, Object> out = new HashMap<>();
        out.put("error", "生产环境需集成 alipay-sdk-java");
        return out;
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken, String openid) {
        if (openid != null && openid.startsWith("mock_")) {
            return Map.of(
                    "user_id", openid,
                    "nick_name", "支付宝用户_" + openid.substring(Math.max(0, openid.length() - 6)),
                    "avatar", "https://t.alipayobjects.com/mock/alipay.png"
            );
        }
        Map<String, Object> out = new HashMap<>();
        out.put("error", "生产环境需集成 alipay-sdk-java");
        return out;
    }

    @Override
    public Map<String, Object> refreshToken(String appId, String refreshToken) {
        if (isMock(appId, "")) {
            return Map.of("access_token", "mock_alipay_at_refreshed_" + System.currentTimeMillis(),
                    "expires_in", 7200);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("error", "生产环境需集成 alipay-sdk-java");
        return out;
    }

    @Override
    public String buildAuthorizeUrl(String appId, String redirectUri, String state, String scope) {
        return "https://openauth.alipay.com/oauth2/publicAppAuthorize.htm"
                + "?app_id=" + enc(appId)
                + "&scope=" + enc(scope != null ? scope : "auth_user")
                + "&redirect_uri=" + enc(redirectUri != null ? redirectUri : "")
                + "&state=" + enc(state != null ? state : "");
    }

    @Override
    public boolean isMock(String appId, String appSecret) {
        return appId == null || appId.isBlank()
                || appId.startsWith("PLACEHOLDER")
                || appId.startsWith("mock_");
    }

    private String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}