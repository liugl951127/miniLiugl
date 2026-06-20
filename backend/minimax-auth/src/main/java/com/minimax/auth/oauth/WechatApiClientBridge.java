package com.minimax.auth.oauth;

import com.minimax.auth.wechat.WechatApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 微信 OAuth 客户端桥接 (V5.2).
 * 让 WechatApiClient 实现 OAuthPlatformClient 接口.
 *
 * @since 2026-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatApiClientBridge implements OAuthPlatformClient {

    private final WechatApiClient wechatApi;

    @Override
    public String platform() { return "wechat"; }

    @Override
    public Map<String, Object> code2Token(String appId, String appSecret, String code, String redirectUri) {
        return wechatApi.code2AccessToken(appId, appSecret, code);
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken, String openid) {
        return wechatApi.getUserInfo(accessToken, openid);
    }

    @Override
    public Map<String, Object> refreshToken(String appId, String refreshToken) {
        return wechatApi.refreshToken(appId, refreshToken);
    }

    @Override
    public String buildAuthorizeUrl(String appId, String redirectUri, String state, String scope) {
        return "https://open.weixin.qq.com/connect/oauth2/authorize"
                + "?appid=" + enc(appId)
                + "&redirect_uri=" + enc(redirectUri != null ? redirectUri : "")
                + "&response_type=code"
                + "&scope=" + enc(scope != null ? scope : "snsapi_base")
                + "&state=" + enc(state != null ? state : "")
                + "#wechat_redirect";
    }

    @Override
    public boolean isMock(String appId, String appSecret) {
        return appId == null || appId.isBlank()
                || appId.startsWith("PLACEHOLDER")
                || appId.startsWith("mock_");
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}