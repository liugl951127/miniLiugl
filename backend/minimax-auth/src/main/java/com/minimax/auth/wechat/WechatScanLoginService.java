package com.minimax.auth.wechat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.auth.entity.*;
import com.minimax.auth.mapper.*;
import com.minimax.auth.service.AuthService;
import com.minimax.auth.vo.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 微信扫码登录服务 (V5).
 *
 * 完整流程:
 *   1. 前端调 GET /auth/wechat/qrcode
 *      → 生成 ticket + sceneId + qrcodeUrl (微信扫码 URL) + expiresAt
 *      → 返回前端显示二维码
 *
 *   2. 前端每 2 秒轮询 GET /auth/wechat/status?ticket=xxx
 *      → 返回 status: pending/scanned/confirmed/expired
 *      → status=confirmed 时, 拿到 accessToken + refreshToken, 自动登录
 *
 *   3. 用户用手机微信扫码 + 确认
 *      → 微信重定向到 /auth/wechat/callback?code=xxx&state=sceneId
 *      → 后端 code 换 access_token + openid
 *      → 用 openid 查/建平台账号, 绑定 wechat_openid
 *      → 生成 JWT, 写入 session.access_token / refresh_token
 *      → status 改为 confirmed
 *
 *   4. 前端下次轮询拿到 confirmed, 自动跳转
 *
 * @since 2026-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatScanLoginService {

    private final WechatScanSessionMapper sessionMapper;
    private final WechatUserBindingMapper bindingMapper;
    private final WechatConfigMapper configMapper;
    private final SysUserMapper userMapper;
    private final AuthService authService;
    private final WechatApiClient wechatApi;

    @Value("${minimax.auth.wechat.ticket-expire-seconds:300}")
    private int ticketExpireSeconds;

    /**
     * 生成二维码 (前端调)
     */
    public Map<String, Object> createQrCode(String clientIp, String userAgent) {
        String ticket = "t_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String sceneId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 拿 mp 配置
        WechatConfig cfg = configMapper.selectOne(
                new LambdaQueryWrapper<WechatConfig>().eq(WechatConfig::getAppType, "mp"));
        boolean mockMode = cfg == null
                || cfg.getAppSecret() == null
                || cfg.getAppSecret().startsWith("PLACEHOLDER")
                || cfg.getAppSecret().startsWith("mock")
                || cfg.getEnabled() == 0;

        // 微信扫码 URL (官方 /connect/qrconnect)
        String appId = (cfg != null && !mockMode) ? cfg.getAppId() : "wx0000000000000000";
        String redirectUri = cfg != null && cfg.getRedirectUri() != null
                ? cfg.getRedirectUri() : "http://localhost:8081/api/v1/auth/wechat/callback";
        String scope = cfg != null && cfg.getScope() != null ? cfg.getScope() : "snsapi_login";

        String qrcodeUrl = "https://open.weixin.qq.com/connect/qrconnect"
                + "?appid=" + appId
                + "&redirect_uri=" + java.net.URLEncoder.encode(redirectUri, java.nio.charset.StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=" + scope
                + "&state=" + sceneId
                + "#wechat_redirect";

        // 写会话
        WechatScanSession session = new WechatScanSession();
        session.setTicket(ticket);
        session.setSceneId(sceneId);
        session.setStatus("pending");
        session.setClientIp(clientIp);
        session.setUserAgent(userAgent);
        session.setExpiresAt(LocalDateTime.now().plusSeconds(ticketExpireSeconds));
        sessionMapper.insert(session);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ticket", ticket);
        out.put("sceneId", sceneId);
        out.put("qrcodeUrl", qrcodeUrl);
        out.put("expiresIn", ticketExpireSeconds);
        out.put("expiresAt", session.getExpiresAt());
        out.put("mock", mockMode);
        out.put("scanUrl", mockMode
                // mock 模式: 直接给一个回调 URL 让用户点 "模拟扫码"
                ? "/auth/wechat/mock-scan?ticket=" + ticket
                : qrcodeUrl);
        return out;
    }

    /**
     * 轮询扫码状态 (前端 2s 调)
     */
    public Map<String, Object> getStatus(String ticket) {
        WechatScanSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<WechatScanSession>().eq(WechatScanSession::getTicket, ticket));
        if (session == null) {
            return Map.of("status", "not_found", "message", "ticket 不存在");
        }
        // 过期
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now())) {
            if (!"confirmed".equals(session.getStatus())) {
                session.setStatus("expired");
                sessionMapper.updateById(session);
                return Map.of("status", "expired");
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", session.getStatus());
        out.put("openid", session.getOpenid());
        out.put("nickname", session.getNickname());
        out.put("avatar", session.getAvatar());
        if ("confirmed".equals(session.getStatus())) {
            out.put("accessToken", session.getAccessToken());
            out.put("refreshToken", session.getRefreshToken());
            out.put("userId", session.getUserId());
        }
        return out;
    }

    /**
     * 微信回调 (用户扫码确认后重定向到此)
     * GET /auth/wechat/callback?code=xxx&state=sceneId
     */
    @Transactional
    public Map<String, Object> handleCallback(String code, String state) {
        if (code == null || code.isBlank()) {
            return Map.of("ok", false, "message", "code 不能为空");
        }
        // 找会话 (按 sceneId=state)
        WechatScanSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<WechatScanSession>().eq(WechatScanSession::getSceneId, state));
        if (session == null) {
            return Map.of("ok", false, "message", "session 不存在 (state=" + state + ")");
        }
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.setStatus("expired");
            sessionMapper.updateById(session);
            return Map.of("ok", false, "message", "二维码已过期, 请刷新");
        }
        // 拿配置 + 换 access_token
        WechatConfig cfg = configMapper.selectOne(
                new LambdaQueryWrapper<WechatConfig>().eq(WechatConfig::getAppType, "mp"));
        boolean mockMode = cfg == null
                || cfg.getAppSecret() == null
                || cfg.getAppSecret().startsWith("PLACEHOLDER")
                || cfg.getAppSecret().startsWith("mock");

        String appId = cfg != null ? cfg.getAppId() : "mock_appid";
        String appSecret = cfg != null ? cfg.getAppSecret() : "mock_secret";

        Map<String, Object> tokenInfo = wechatApi.code2AccessToken(appId, appSecret, code);
        String accessToken = (String) tokenInfo.get("access_token");
        String openid = (String) tokenInfo.get("openid");
        String unionid = (String) tokenInfo.getOrDefault("unionid", null);

        // 拿用户信息
        Map<String, Object> userInfo = wechatApi.getUserInfo(accessToken, openid);
        String nickname = (String) userInfo.getOrDefault("nickname", "微信用户");
        String avatar = (String) userInfo.getOrDefault("headimgurl", null);

        // 更新会话
        session.setOpenid(openid);
        session.setUnionid(unionid);
        session.setNickname(nickname);
        session.setAvatar(avatar);
        session.setStatus("scanned");
        sessionMapper.updateById(session);

        // 找/建平台账号
        SysUser sysUser = findOrCreateUser(openid, unionid, nickname, avatar, mockMode);
        session.setUserId(sysUser.getId());

        // 生成 JWT
        LoginResponse login = authService.issueLoginResponse(sysUser, null);
        session.setAccessToken(login.getAccessToken());
        session.setRefreshToken(login.getRefreshToken());
        session.setStatus("confirmed");
        session.setConfirmedAt(LocalDateTime.now());
        sessionMapper.updateById(session);

        log.info("微信扫码登录成功: user={} openid={}", sysUser.getUsername(), openid);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("message", "登录成功");
        out.put("userId", sysUser.getId());
        out.put("username", sysUser.getUsername());
        out.put("nickname", nickname);
        out.put("accessToken", login.getAccessToken());
        out.put("refreshToken", login.getRefreshToken());
        return out;
    }

    /**
     * 模拟扫码 (mock 模式给前端按钮用)
     * GET /auth/wechat/mock-scan?ticket=xxx
     */
    @Transactional
    public Map<String, Object> mockScan(String ticket) {
        WechatScanSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<WechatScanSession>().eq(WechatScanSession::getTicket, ticket));
        if (session == null) {
            return Map.of("ok", false, "message", "ticket 不存在");
        }
        if ("expired".equals(session.getStatus())
                || (session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now()))) {
            return Map.of("ok", false, "message", "已过期");
        }
        if ("confirmed".equals(session.getStatus())) {
            return Map.of("ok", true, "message", "已确认, 请前端轮询");
        }

        String mockOpenid = "mock_openid_" + ticket.substring(0, Math.min(10, ticket.length()));
        String mockNickname = "微信用户_" + ticket.substring(2, 8);
        session.setOpenid(mockOpenid);
        session.setUnionid("mock_unionid_" + ticket.substring(0, 8));
        session.setNickname(mockNickname);
        session.setAvatar("https://thirdwx.qlogo.cn/mmopen/mock/" + ticket.substring(0, 6) + ".png");
        session.setStatus("scanned");
        sessionMapper.updateById(session);

        SysUser sysUser = findOrCreateUser(mockOpenid, mockOpenid, mockNickname, session.getAvatar(), true);
        session.setUserId(sysUser.getId());
        LoginResponse login = authService.issueLoginResponse(sysUser, null);
        session.setAccessToken(login.getAccessToken());
        session.setRefreshToken(login.getRefreshToken());
        session.setStatus("confirmed");
        session.setConfirmedAt(LocalDateTime.now());
        sessionMapper.updateById(session);

        return Map.of("ok", true, "message", "mock 扫码成功");
    }

    /**
     * 移动端静默登录 (小程序 / 公众号已关注用户)
     * POST /auth/wechat/mobile-login
     * body: { code, appType: "mini" | "mp" }
     */
    @Transactional
    public LoginResponse mobileLogin(String code, String appType) {
        if (code == null || code.isBlank()) {
            throw new RuntimeException("code 不能为空");
        }
        if (appType == null) appType = "mini";
        WechatConfig cfg = configMapper.selectOne(
                new LambdaQueryWrapper<WechatConfig>().eq(WechatConfig::getAppType, appType));
        boolean mockMode = cfg == null
                || cfg.getAppSecret() == null
                || cfg.getAppSecret().startsWith("PLACEHOLDER")
                || cfg.getAppSecret().startsWith("mock");

        String appId = cfg != null ? cfg.getAppId() : "mock_appid";
        String appSecret = cfg != null ? cfg.getAppSecret() : "mock_secret";

        Map<String, Object> tokenInfo = wechatApi.code2AccessToken(appId, appSecret, code);
        String openid = (String) tokenInfo.get("openid");
        Map<String, Object> userInfo = wechatApi.getUserInfo(
                (String) tokenInfo.get("access_token"), openid);
        String nickname = (String) userInfo.getOrDefault("nickname", "微信用户");
        String avatar = (String) userInfo.getOrDefault("headimgurl", null);
        String unionid = (String) userInfo.getOrDefault("unionid", null);

        SysUser sysUser = findOrCreateUser(openid, unionid, nickname, avatar, mockMode);
        return authService.issueLoginResponse(sysUser, null);
    }

    /**
     * 根据 openid 找/建平台账号
     */
    @Transactional
    public SysUser findOrCreateUser(String openid, String unionid, String nickname, String avatar, boolean mockMode) {
        // 1. 先查 binding 表
        WechatUserBinding binding = bindingMapper.selectOne(
                new LambdaQueryWrapper<WechatUserBinding>().eq(WechatUserBinding::getOpenid, openid));
        if (binding != null) {
            SysUser u = userMapper.selectById(binding.getUserId());
            if (u != null && u.getStatus() != null && u.getStatus() == 1) {
                binding.setLastLoginAt(LocalDateTime.now());
                bindingMapper.updateById(binding);
                return u;
            }
        }
        // 2. 看 sys_user.wechat_openid
        SysUser u = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getWechatOpenid, openid));
        if (u != null) {
            // 写 binding
            WechatUserBinding b = new WechatUserBinding();
            b.setUserId(u.getId());
            b.setOpenid(openid);
            b.setUnionid(unionid);
            b.setAppType("mp");
            b.setNickname(nickname);
            b.setAvatar(avatar);
            b.setLastLoginAt(LocalDateTime.now());
            bindingMapper.insert(b);
            return u;
        }
        // 3. 新建账号
        SysUser newUser = new SysUser();
        newUser.setUsername("wx_" + openid.substring(0, Math.min(16, openid.length())));
        newUser.setNickname(nickname != null ? nickname : "微信用户");
        // 随机 BCrypt 密码 (微信用户不能密码登录, 只能微信登录).
        // 格式: $2a$10$<22-char-salt><31-char-hash> = 60 chars total
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 22);
        String hash = UUID.randomUUID().toString().replace("-", "").substring(0, 31);
        newUser.setPassword("$2a$10$" + salt + hash);
        newUser.setEmail(openid + "@wechat.local");
        newUser.setAvatar(avatar);
        newUser.setGender(0);
        newUser.setStatus(1);
        newUser.setTenantId(1L);
        newUser.setWechatOpenid(openid);
        newUser.setWechatUnionid(unionid);
        newUser.setWechatNickname(nickname);
        newUser.setWechatAvatar(avatar);
        newUser.setWechatBoundAt(LocalDateTime.now());
        userMapper.insert(newUser);
        // 写 binding (user_role 不在这里加, 微信用户首次登录后由前端引导绑定手机号或后续完善)
        WechatUserBinding b = new WechatUserBinding();
        b.setUserId(newUser.getId());
        b.setOpenid(openid);
        b.setUnionid(unionid);
        b.setAppType("mp");
        b.setNickname(nickname);
        b.setAvatar(avatar);
        b.setLastLoginAt(LocalDateTime.now());
        bindingMapper.insert(b);
        log.info("微信用户自动注册: username={} openid={}", newUser.getUsername(), openid);
        return newUser;
    }
}
