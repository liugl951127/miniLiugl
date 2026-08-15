package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OAuth 跨平台应用配置 (V5.2).
 *
 * 统一管理 微信/QQ/支付宝/微博/GitHub 等平台的应用凭证.
 * 一行配置 = 一个平台 × 一个应用类型.
 *
 * @since 2026-06
 */
@Data
@TableName("oauth_app_config")
public class OAuthAppConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** wechat / qq / alipay / weibo / github */
    private String platform;

    /** mp / mini / open / web / app / h5 */
    private String appType;

    private String appId;

    private String appSecret;

    /** 支付宝 RSA 公钥 */
    private String publicKey;

    private String redirectUri;

    /** 多 scopes 用逗号分隔 */
    private String scopes;

    private Integer enabled;

    /** JSON 格式额外配置 */
    private String extraConfig;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}