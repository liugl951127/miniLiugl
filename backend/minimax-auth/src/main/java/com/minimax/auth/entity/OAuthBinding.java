package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
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

    private Long userId;

    private String platform;

    private String appType;

    private String openid;

    private String unionid;

    private String nickname;

    private String avatar;

    private String accessToken;

    private String refreshToken;

    private LocalDateTime tokenExpiresAt;

    private String rawData;

    private LocalDateTime boundAt;

    private LocalDateTime lastLoginAt;
}