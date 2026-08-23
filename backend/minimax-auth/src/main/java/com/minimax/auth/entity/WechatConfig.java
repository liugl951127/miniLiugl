package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 微信应用配置 (V5).
 */
@Data
@TableName("wechat_config")
public class WechatConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("app_type")
    private String appType;     // mp/mini/open/web
    @TableField("app_id")
    private String appId;
    @TableField("app_secret")
    private String appSecret;
    @TableField("token")
    private String token;
    @TableField("aes_key")
    private String aesKey;
    @TableField("redirect_uri")
    private String redirectUri;
    @TableField("scope")
    private String scope;
    @TableField("enabled")
    private Integer enabled;
    @TableField("remark")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
