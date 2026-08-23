package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField("platform")
    private String platform;

    /** mp / mini / open / web / app / h5 */
    @TableField("app_type")
    private String appType;

    @TableField("app_id")
    private String appId;

    @TableField("app_secret")
    private String appSecret;

    /** 支付宝 RSA 公钥 */
    @TableField("public_key")
    private String publicKey;

    @TableField("redirect_uri")
    private String redirectUri;

    /** 多 scopes 用逗号分隔 */
    @TableField("scopes")
    private String scopes;

    @TableField("enabled")
    private Integer enabled;

    /** JSON 格式额外配置 */
    @TableField("extra_config")
    private String extraConfig;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}