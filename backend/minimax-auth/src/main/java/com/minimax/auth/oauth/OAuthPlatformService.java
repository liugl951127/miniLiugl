package com.minimax.auth.oauth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.auth.entity.*;
import com.minimax.auth.mapper.*;
import com.minimax.auth.service.AuthService;
import com.minimax.auth.vo.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 跨平台 OAuth 统一服务 (V5.2).
 *
 * 抽象 3 个 OAuth 客户端 (wechat/qq/alipay) 的一致接口:
 *   - 拿配置 (oauth_app_config)
 *   - code → token → userInfo
 *   - findOrCreateUser (跨平台 unionid 打通)
 *   - 写 oauth_binding
 *   - 写 unionid_relations (binding_count++)
 *   - 生成 JWT
 *
 * @since 2026-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthPlatformService {

    private final OAuthAppConfigMapper configMapper;
    public OAuthAppConfigMapper getConfigMapper() { return configMapper; }
    private final SysUserMapper userMapper;
    private final OAuthBindingMapper bindingMapper;
    private final UnionidRelationsMapper unionidMapper;
    private final WechatApiClientBridge wechatBridge;
    private final QqOAuthClient qqClient;
    private final AlipayOAuthClient alipayClient;
    private final AuthService authService;

    /**
     * 根据 platform 拿对应的 OAuth 客户端
     */
    public OAuthPlatformClient getClient(String platform) {
        return switch (platform) {
            case "wechat" -> wechatBridge;
            case "qq" -> qqClient;
            case "alipay" -> alipayClient;
            default -> throw new RuntimeException("不支持的平台: " + platform);
        };
    }

    /**
     * 拿应用配置
     */
    public OAuthAppConfig getConfig(String platform, String appType) {
        return configMapper.selectOne(
                new LambdaQueryWrapper<OAuthAppConfig>()
                        .eq(OAuthAppConfig::getPlatform, platform)
                        .eq(OAuthAppConfig::getAppType, appType));
    }

    /**
     * 跨平台登录 (H5/移动端通用入口).
     *
     * @param platform wechat/qq/alipay
     * @param appType  mp/mini/open/web/app/h5
     * @param code     授权码
     */
    @Transactional
    public LoginResponse login(String platform, String appType, String code, String redirectUri) {
        OAuthPlatformClient client = getClient(platform);
        OAuthAppConfig cfg = getConfig(platform, appType);
        boolean mockMode = cfg == null || client.isMock(
                cfg != null ? cfg.getAppId() : null,
                cfg != null ? cfg.getAppSecret() : null);

        String appId = cfg != null ? cfg.getAppId() : "PLACEHOLDER_" + platform.toUpperCase();
        String appSecret = cfg != null ? cfg.getAppSecret() : "PLACEHOLDER_" + platform.toUpperCase() + "_SECRET";

        // Step 1: code → token + openid
        Map<String, Object> tokenInfo = client.code2Token(appId, appSecret, code, redirectUri);
        if (tokenInfo.containsKey("error")) {
            throw new RuntimeException(platform + " OAuth 失败: " + tokenInfo.get("error"));
        }
        String accessToken = (String) tokenInfo.get("access_token");
        String refreshToken = (String) tokenInfo.get("refresh_token");
        String openid = (String) tokenInfo.get("openid");
        String unionid = (String) tokenInfo.getOrDefault("unionid", openid);

        // Step 2: 拿用户信息
        Map<String, Object> userInfo = client.getUserInfo(accessToken, openid);
        String nickname = userInfo != null
                ? (String) userInfo.getOrDefault("nickname", userInfo.getOrDefault("nick_name", platform + "用户"))
                : platform + "用户";
        String avatar = userInfo != null
                ? (String) userInfo.getOrDefault("headimgurl", userInfo.getOrDefault("figureurl", userInfo.getOrDefault("avatar", null)))
                : null;

        // Step 3: findOrCreateUser (跨平台 unionid 打通)
        SysUser sysUser = findOrCreateUser(platform, appType, openid, unionid, nickname, avatar, mockMode);

        // Step 4: 写 oauth_binding
        upsertBinding(sysUser.getId(), platform, appType, openid, unionid, nickname, avatar,
                accessToken, refreshToken);

        // Step 5: 写 unionid_relations
        recordUnionid(sysUser.getId(), unionid, platform);

        return authService.issueLoginResponse(sysUser, null);
    }

    /**
     * 跨平台 findOrCreateUser (V5.2).
     *
     * 优先级:
     *   1. oauth_binding (platform + app_type + openid) 精确匹配
     *   2. ★ unionid 跨平台打通 (同 unionid 跨 wechat/qq/alipay)
     *   3. sys_user 主平台字段
     *   4. 新建账号
     */
    @Transactional
    public SysUser findOrCreateUser(String platform, String appType, String openid, String unionid,
                                    String nickname, String avatar, boolean mockMode) {
        // 0. mock 模式: unionid 复用 openid (模拟跨平台)
        if (mockMode) {
            unionid = unionid != null ? unionid : openid;
        }

        // 1. binding (platform + app_type + openid) 精确匹配
        OAuthBinding binding = bindingMapper.selectOne(
                new LambdaQueryWrapper<OAuthBinding>()
                        .eq(OAuthBinding::getPlatform, platform)
                        .eq(OAuthBinding::getAppType, appType)
                        .eq(OAuthBinding::getOpenid, openid));
        if (binding != null) {
            SysUser u = userMapper.selectById(binding.getUserId());
            if (u != null && u.getStatus() != null && u.getStatus() == 1) {
                binding.setLastLoginAt(LocalDateTime.now());
                bindingMapper.updateById(binding);
                updateUserPlatformInfo(u.getId(), platform, openid, unionid, nickname, avatar);
                return u;
            }
        }

        // 2. ★ unionid 跨平台打通
        if (unionid != null && !unionid.isBlank()) {
            List<OAuthBinding> sameUnionid = bindingMapper.selectList(
                    new LambdaQueryWrapper<OAuthBinding>()
                            .eq(OAuthBinding::getUnionid, unionid)
                            .ne(OAuthBinding::getPlatform, platform));
            if (!sameUnionid.isEmpty()) {
                SysUser existing = userMapper.selectById(sameUnionid.get(0).getUserId());
                if (existing != null && existing.getStatus() != null && existing.getStatus() == 1) {
                    updateUserPlatformInfo(existing.getId(), platform, openid, unionid, nickname, avatar);
                    log.info("跨平台 unionid 打通: user={} {}→{} unionid={}",
                            existing.getUsername(),
                            sameUnionid.get(0).getPlatform(), platform, unionid);
                    return existing;
                }
            }
        }

        // 3. sys_user 主平台字段
        SysUser u = queryUserByPlatformOpenid(platform, openid);
        if (u != null) {
            return u;
        }

        // 4. 新建账号
        SysUser newUser = new SysUser();
        String prefix = platform.substring(0, Math.min(3, platform.length()));
        // V5.2: 用 code 加 salt 让 username 唯一 (避免同名冲突)
        String uSalt = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        newUser.setUsername(prefix + "_" + openid.hashCode() + "_" + uSalt);
        newUser.setNickname(nickname != null ? nickname : prefix + "用户");
        // BCrypt 60 字符密码
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 22);
        String hash = UUID.randomUUID().toString().replace("-", "").substring(0, 31);
        newUser.setPassword("$2a$10$" + salt + hash);
        newUser.setEmail(openid + "@" + platform + ".local");
        newUser.setAvatar(avatar);
        newUser.setStatus(1);
        newUser.setTenantId(1L);
        // 平台字段 (insert 前 set, insert 后会被覆盖, 所以后面再 update)
        switch (platform) {
            case "wechat":
                newUser.setWechatOpenid(openid);
                newUser.setWechatUnionid(unionid);
                newUser.setWechatNickname(nickname);
                newUser.setWechatAvatar(avatar);
                newUser.setWechatBoundAt(LocalDateTime.now());
                break;
            case "qq":
                newUser.setQqOpenid(openid);
                newUser.setQqUnionid(unionid);
                newUser.setQqNickname(nickname);
                newUser.setQqAvatar(avatar);
                newUser.setQqBoundAt(LocalDateTime.now());
                break;
            case "alipay":
                newUser.setAlipayOpenid(openid);
                newUser.setAlipayUserId(openid);
                newUser.setAlipayNickname(nickname);
                newUser.setAlipayAvatar(avatar);
                newUser.setAlipayBoundAt(LocalDateTime.now());
                break;
        }
        userMapper.insert(newUser);
        log.info("{} 用户自动注册: username={} openid={} unionid={}",
                platform, newUser.getUsername(), openid, unionid);
        return newUser;
    }

    /**
     * 更新用户主平台字段 (带 user_id)
     */
    @Transactional
    public void updateUserPlatformInfo(Long userId, String platform, String openid, String unionid,
                                       String nickname, String avatar) {
        if (userId == null) {
            // 临时设到新对象
            // 实际上要 insert 后再 update
            return;
        }
        SysUser update = new SysUser();
        switch (platform) {
            case "wechat":
                update.setWechatOpenid(openid);
                update.setWechatUnionid(unionid);
                update.setWechatNickname(nickname);
                update.setWechatAvatar(avatar);
                update.setWechatBoundAt(LocalDateTime.now());
                break;
            case "qq":
                update.setQqOpenid(openid);
                update.setQqUnionid(unionid);
                update.setQqNickname(nickname);
                update.setQqAvatar(avatar);
                update.setQqBoundAt(LocalDateTime.now());
                break;
            case "alipay":
                update.setAlipayOpenid(openid);
                update.setAlipayUserId(openid);  // 支付宝 user_id
                update.setAlipayNickname(nickname);
                update.setAlipayAvatar(avatar);
                update.setAlipayBoundAt(LocalDateTime.now());
                break;
        }
        if (userId != null) {
            userMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysUser>()
                    .eq(SysUser::getId, userId)
                    .set(SysUser::getWechatOpenid, "wechat".equals(platform) ? openid : null)
                    .set(SysUser::getWechatUnionid, "wechat".equals(platform) ? unionid : null)
                    .set(SysUser::getWechatNickname, "wechat".equals(platform) ? nickname : null)
                    .set(SysUser::getWechatAvatar, "wechat".equals(platform) ? avatar : null)
                    .set(SysUser::getWechatBoundAt, "wechat".equals(platform) ? LocalDateTime.now() : null)
                    .set(SysUser::getQqOpenid, "qq".equals(platform) ? openid : null)
                    .set(SysUser::getQqUnionid, "qq".equals(platform) ? unionid : null)
                    .set(SysUser::getQqNickname, "qq".equals(platform) ? nickname : null)
                    .set(SysUser::getQqAvatar, "qq".equals(platform) ? avatar : null)
                    .set(SysUser::getQqBoundAt, "qq".equals(platform) ? LocalDateTime.now() : null)
                    .set(SysUser::getAlipayOpenid, "alipay".equals(platform) ? openid : null)
                    .set(SysUser::getAlipayUserId, "alipay".equals(platform) ? openid : null)
                    .set(SysUser::getAlipayNickname, "alipay".equals(platform) ? nickname : null)
                    .set(SysUser::getAlipayAvatar, "alipay".equals(platform) ? avatar : null)
                    .set(SysUser::getAlipayBoundAt, "alipay".equals(platform) ? LocalDateTime.now() : null));
        }
    }

    private SysUser queryUserByPlatformOpenid(String platform, String openid) {
        switch (platform) {
            case "wechat": return userMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getWechatOpenid, openid));
            case "qq": return userMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getQqOpenid, openid));
            case "alipay": return userMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getAlipayOpenid, openid));
        }
        return null;
    }

    /**
     * upsert oauth_binding
     */
    private void upsertBinding(Long userId, String platform, String appType, String openid, String unionid,
                               String nickname, String avatar, String accessToken, String refreshToken) {
        OAuthBinding b = bindingMapper.selectOne(
                new LambdaQueryWrapper<OAuthBinding>()
                        .eq(OAuthBinding::getPlatform, platform)
                        .eq(OAuthBinding::getAppType, appType)
                        .eq(OAuthBinding::getOpenid, openid));
        if (b == null) {
            b = new OAuthBinding();
            b.setUserId(userId);
            b.setPlatform(platform);
            b.setAppType(appType);
            b.setOpenid(openid);
            b.setUnionid(unionid);
            b.setNickname(nickname);
            b.setAvatar(avatar);
            b.setAccessToken(accessToken);
            b.setRefreshToken(refreshToken);
            b.setBoundAt(LocalDateTime.now());
            b.setLastLoginAt(LocalDateTime.now());
            bindingMapper.insert(b);
        } else {
            b.setUserId(userId);
            b.setUnionid(unionid);
            b.setNickname(nickname);
            b.setAvatar(avatar);
            b.setAccessToken(accessToken);
            b.setRefreshToken(refreshToken);
            b.setLastLoginAt(LocalDateTime.now());
            bindingMapper.updateById(b);
        }
    }

    private void recordUnionid(Long userId, String unionid, String platform) {
        if (unionid == null || unionid.isBlank()) return;
        UnionidRelations exist = unionidMapper.selectOne(
                new LambdaQueryWrapper<UnionidRelations>()
                        .eq(UnionidRelations::getUserId, userId)
                        .eq(UnionidRelations::getUnionid, unionid));
        if (exist == null) {
            UnionidRelations r = new UnionidRelations();
            r.setUserId(userId);
            r.setUnionid(unionid);
            r.setPlatform(platform);
            r.setFirstSeenAt(LocalDateTime.now());
            r.setLastSeenAt(LocalDateTime.now());
            r.setBindingCount(1);
            unionidMapper.insert(r);
            return;
        }
        Long realCount = bindingMapper.selectCount(
                new LambdaQueryWrapper<OAuthBinding>()
                        .eq(OAuthBinding::getUserId, userId)
                        .eq(OAuthBinding::getUnionid, unionid));
        UnionidRelations update = new UnionidRelations();
        update.setId(exist.getId());
        update.setPlatform(platform);  // 更新为最近活跃平台
        update.setLastSeenAt(LocalDateTime.now());
        update.setBindingCount(realCount.intValue());
        unionidMapper.updateById(update);
    }
}