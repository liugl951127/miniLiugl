package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OAuth 跨平台 binding (V5.2).
 * 替代 wechat_user_binding, 支持任意平台 (wechat/qq/alipay/weibo/github).
 *
 * @since 2026-06
 */
@Data
@TableName("oauth_binding")
public class OAuthBinding {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("platform")
    private String platform;

    @TableField("app_type")
    private String appType;

    @TableField("openid")
    private String openid;

    @TableField("unionid")
    private String unionid;

    @TableField("nickname")
    private String nickname;

    @TableField("avatar")
    private String avatar;

    @TableField("access_token")
    private String accessToken;

    @TableField("refresh_token")
    private String refreshToken;

    @TableField("token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @TableField("raw_data")
    private String rawData;

    @TableField("bound_at")
    private LocalDateTime boundAt;

    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;
}