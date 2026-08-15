package com.minimax.auth.wechat;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 微信 API 调用客户端 (V5).
 *
 * 调用流程 (网页扫码登录):
 *   1. GET /connect/qrconnect?appid=...&redirect_uri=...&state=...&scope=snsapi_login
 *      → 用户扫码 → 微信重定向到 redirect_uri?code=xxx&state=xxx
 *   2. POST /sns/oauth2/access_token?appid=...&secret=...&code=...&grant_type=authorization_code
 *      → access_token + openid + unionid
 *   3. GET /sns/userinfo?access_token=...&openid=...&lang=zh_CN
 *      → nickname + headimgurl + unionid
 *
 * 注: 测试环境无真实 AppID, 用 mock 模式返回假数据.
 *
 * @since 2026-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatApiClient {

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    /**
     * 用 code 换 access_token + openid
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> code2AccessToken(String appId, String appSecret, String code) {
        // Mock 模式
        if (isMock(appId, appSecret)) {
            // V5.1: 加了以 "TU_" 开头的 code (test unionid) — mock 模式模拟跨应用打通场景
            // 同一 prefix (code 前 4 字符) 的 code 返回同一 unionid
            String prefix = code.length() >= 4 ? code.substring(0, 4) : code;
            if (code.startsWith("TU_")) {
                String sharedUnion = "test_unionid_" + prefix;
                return Map.of(
                        "access_token", "mock_at_" + System.currentTimeMillis(),
                        "expires_in", 7200,
                        "refresh_token", "mock_rt_" + System.currentTimeMillis(),
                        "openid", "mock_openid_" + code,
                        "unionid", sharedUnion,
                        "scope", "snsapi_login"
                );
            }
            return Map.of(
                    "access_token", "mock_at_" + System.currentTimeMillis(),
                    "expires_in", 7200,
                    "refresh_token", "mock_rt_" + System.currentTimeMillis(),
                    "openid", "mock_openid_" + code.substring(0, Math.min(8, code.length())),
                    "unionid", "test_unionid_" + prefix,  // V5.1: 统一 unionid 前缀
                    "scope", "snsapi_login"
            );
        }
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token"
                + "?appid=" + urlEncode(appId)
                + "&secret=" + urlEncode(appSecret)
                + "&code=" + urlEncode(code)
                + "&grant_type=authorization_code";
        return getJson(url);
    }

    /**
     * 用 access_token 拿用户信息
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserInfo(String accessToken, String openid) {
        if (isMockOpenid(openid)) {
            return Map.of(
                    "openid", openid,
                    "nickname", "微信用户_" + openid.substring(Math.max(0, openid.length() - 6)),
                    "sex", 1,
                    "province", "北京",
                    "city", "海淀",
                    "country", "中国",
                    "headimgurl", "https://thirdwx.qlogo.cn/mmopen/mock/avatar.png",
                    "unionid", "mock_unionid_" + openid
            );
        }
        String url = "https://api.weixin.qq.com/sns/userinfo"
                + "?access_token=" + urlEncode(accessToken)
                + "&openid=" + urlEncode(openid)
                + "&lang=zh_CN";
        return getJson(url);
    }

    /**
     * 小程序: jscode2session (V5.1).
     * GET https://api.weixin.qq.com/sns/jscode2session?appid=&secret=&js_code=&grant_type=authorization_code
     * 返回: { openid, session_key, unionid } (unionid 需在同一开放平台)
     *
     * @return { openid, session_key, unionid }
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> jscode2session(String appId, String appSecret, String jsCode) {
        if (isMock(appId, appSecret)) {
            // V5.1: TU_ prefix → 共享 unionid (用于跨应用打通测试)
            if (jsCode.startsWith("TU_")) {
                String prefix = jsCode.length() >= 4 ? jsCode.substring(0, 4) : jsCode;
                return Map.of(
                        "openid", "mock_mini_openid_" + jsCode,
                        "session_key", "mock_session_key_" + System.currentTimeMillis(),
                        "unionid", "test_unionid_" + prefix
                );
            }
            return Map.of(
                    "openid", "mock_mini_openid_" + jsCode.substring(0, Math.min(8, jsCode.length())),
                    "session_key", "mock_session_key_" + System.currentTimeMillis(),
                    "unionid", "mock_mini_unionid_" + jsCode.substring(0, Math.min(8, jsCode.length()))
            );
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session"
                + "?appid=" + urlEncode(appId)
                + "&secret=" + urlEncode(appSecret)
                + "&js_code=" + urlEncode(jsCode)
                + "&grant_type=authorization_code";
        return getJson(url);
    }

    /**
     * 刷新 access_token
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> refreshToken(String appId, String refreshToken) {
        if (isMockRefresh(refreshToken)) {
            return Map.of(
                    "access_token", "mock_at_refreshed_" + System.currentTimeMillis(),
                    "expires_in", 7200,
                    "refresh_token", "mock_rt_refreshed_" + System.currentTimeMillis(),
                    "openid", "mock_openid_refreshed",
                    "scope", "snsapi_login"
            );
        }
        String url = "https://api.weixin.qq.com/sns/oauth2/refresh_token"
                + "?appid=" + urlEncode(appId)
                + "&grant_type=refresh_token"
                + "&refresh_token=" + urlEncode(refreshToken);
        return getJson(url);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getJson(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new RuntimeException("HTTP " + resp.statusCode());
            }
            return json.readValue(resp.body(), Map.class);
        } catch (Exception e) {
            log.warn("微信 API 调用失败: {} - {}", url, e.getMessage());
            throw new RuntimeException("微信 API 调用失败: " + e.getMessage());
        }
    }

    private String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private boolean isMock(String appId, String appSecret) {
        return appId == null || appId.isBlank() || appSecret == null
                || appSecret.isBlank() || appSecret.startsWith("PLACEHOLDER")
                || appSecret.startsWith("mock");
    }

    private boolean isMockOpenid(String openid) {
        return openid == null || openid.startsWith("mock_");
    }

    private boolean isMockRefresh(String rt) {
        return rt == null || rt.startsWith("mock_");
    }
}
