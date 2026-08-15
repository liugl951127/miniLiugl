package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
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

    private String appType;     // mp/mini/open/web
    private String appId;
    private String appSecret;
    private String token;
    private String aesKey;
    private String redirectUri;
    private String scope;
    private Integer enabled;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
