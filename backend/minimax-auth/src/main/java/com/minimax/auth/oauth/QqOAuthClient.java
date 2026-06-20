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
 * QQ 互联 OAuth 客户端 (V5.2).
 *
 * 流程 (类似微信):
 *   1. GET https://graph.qq.com/oauth2.0/authorize?response_type=code&client_id=&redirect_uri=&state=&scope=
 *      → 用户授权 → QQ 重定向到 redirect_uri?code=xxx&state=xxx
 *   2. GET https://graph.qq.com/oauth2.0/token?grant_type=authorization_code&client_id=&client_secret=&code=&redirect_uri=
 *      → access_token + expires_in + refresh_token
 *   3. GET https://graph.qq.com/oauth2.0/me?access_token=
 *      → openid (回调 JSONP, 需解析 callback)
 *   4. GET https://graph.qq.com/user/get_user_info?access_token=&oauth_consumer_key=&openid=
 *      → nickname + figureurl + gender + ...
 *
 * @since 2026-06
 */
@Slf4j
@Component
public class QqOAuthClient implements OAuthPlatformClient {

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    @Override
    public String platform() { return "qq"; }

    @Override
    public Map<String, Object> code2Token(String appId, String appSecret, String code, String redirectUri) {
        if (isMock(appId, appSecret)) {
            return Map.of(
                    "access_token", "mock_qq_at_" + System.currentTimeMillis(),
                    "expires_in", 7200,
                    "refresh_token", "mock_qq_rt_" + System.currentTimeMillis(),
                    "openid", "mock_qq_openid_" + code.substring(0, Math.min(8, code.length())),
                    "unionid", "mock_qq_unionid_" + code.substring(0, Math.min(8, code.length()))
            );
        }
        String url = "https://graph.qq.com/oauth2.0/token"
                + "?grant_type=authorization_code"
                + "&client_id=" + enc(appId)
                + "&client_secret=" + enc(appSecret)
                + "&code=" + enc(code)
                + "&redirect_uri=" + enc(redirectUri != null ? redirectUri : "");
        String body = getRaw(url);
        Map<String, Object> out = new HashMap<>();
        // QQ 返回 key=value&key=value 格式
        for (String kv : body.split("&")) {
            String[] p = kv.split("=", 2);
            if (p.length == 2) out.put(p[0], p[1]);
        }
        // 单独再调 /me 拿 openid
        if (out.containsKey("access_token")) {
            try {
                String meUrl = "https://graph.qq.com/oauth2.0/me?access_token=" + out.get("access_token");
                String meBody = getRaw(meUrl);
                // callback({"client_id":"...","openid":"..."})
                int s = meBody.indexOf("{"), e = meBody.indexOf("}");
                if (s >= 0 && e > s) {
                    Map<String, Object> me = json.readValue(meBody.substring(s, e + 1), Map.class);
                    out.put("openid", me.get("openid"));
                    out.put("unionid", me.getOrDefault("unionid", out.get("openid")));
                }
            } catch (Exception ex) {
                log.warn("QQ /me 失败: {}", ex.getMessage());
            }
        }
        return out;
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken, String openid) {
        // openid 同时需要 app_id (client_id)
        // 生产环境: 需要 caller 传 appId; 这里用 mock
        if (openid != null && openid.startsWith("mock_")) {
            return Map.of(
                    "openid", openid,
                    "nickname", "QQ用户_" + openid.substring(Math.max(0, openid.length() - 6)),
                    "figureurl", "https://q.qlogo.cn/headimgdl?/mock/qq.png",
                    "gender", "男",
                    "unionid", "mock_qq_unionid_" + openid
            );
        }
        // 真实 API 需要 client_id 参数, 这里给简化
        String url = "https://graph.qq.com/user/get_user_info"
                + "?access_token=" + enc(accessToken)
                + "&oauth_consumer_key=" + enc("APP_ID_NEEDED")
                + "&openid=" + enc(openid);
        return getJson(url);
    }

    @Override
    public Map<String, Object> refreshToken(String appId, String refreshToken) {
        if (isMock(appId, "")) {
            return Map.of(
                    "access_token", "mock_qq_at_refreshed_" + System.currentTimeMillis(),
                    "expires_in", 7200,
                    "refresh_token", "mock_qq_rt_refreshed_" + System.currentTimeMillis()
            );
        }
        String url = "https://graph.qq.com/oauth2.0/token"
                + "?grant_type=refresh_token"
                + "&client_id=" + enc(appId)
                + "&refresh_token=" + enc(refreshToken);
        return getJson(url);
    }

    @Override
    public String buildAuthorizeUrl(String appId, String redirectUri, String state, String scope) {
        return "https://graph.qq.com/oauth2.0/authorize"
                + "?response_type=code"
                + "&client_id=" + enc(appId)
                + "&redirect_uri=" + enc(redirectUri != null ? redirectUri : "")
                + "&state=" + enc(state != null ? state : "")
                + "&scope=" + enc(scope != null ? scope : "get_user_info");
    }

    @Override
    public boolean isMock(String appId, String appSecret) {
        return appId == null || appId.isBlank()
                || appId.startsWith("PLACEHOLDER")
                || appId.startsWith("mock_");
    }

    // ================ 工具 ================
    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private String getRaw(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) throw new RuntimeException("HTTP " + resp.statusCode());
            return resp.body();
        } catch (Exception e) {
            log.warn("QQ API 调用失败: {} - {}", url, e.getMessage());
            throw new RuntimeException("QQ API 调用失败: " + e.getMessage());
        }
    }

    private Map<String, Object> getJson(String url) {
        try {
            return json.readValue(getRaw(url), Map.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}