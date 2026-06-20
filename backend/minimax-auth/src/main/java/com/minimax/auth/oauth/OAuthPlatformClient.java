package com.minimax.auth.oauth;

import java.util.Map;

/**
 * 跨平台 OAuth 抽象层 (V5.2).
 *
 * 统一接口, 让 Wechat/Qq/Alipay 等实现同一套方法,
 * 业务层不需要关心具体平台的 API 差异.
 *
 * @since 2026-06
 */
public interface OAuthPlatformClient {

    /**
     * 平台标识: wechat / qq / alipay / weibo / github
     */
    String platform();

    /**
     * 用授权码换 access_token + openid (类似微信 code2access_token)
     */
    Map<String, Object> code2Token(String appId, String appSecret, String code, String redirectUri);

    /**
     * 用 access_token 拿用户信息 (昵称/头像)
     */
    Map<String, Object> getUserInfo(String accessToken, String openid);

    /**
     * 刷新 token
     */
    Map<String, Object> refreshToken(String appId, String refreshToken);

    /**
     * 生成授权 URL (前端跳转用)
     */
    String buildAuthorizeUrl(String appId, String redirectUri, String state, String scope);

    /**
     * 是否 mock 模式 (appId 是 PLACEHOLDER 时)
     */
    boolean isMock(String appId, String appSecret);
}