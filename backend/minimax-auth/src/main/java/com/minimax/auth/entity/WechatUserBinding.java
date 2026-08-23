package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 微信用户绑定 (V5).
 * 一个 openid 绑定一个平台 user_id.
 */
@Data
@TableName("wechat_user_binding")
public class WechatUserBinding {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("openid")
    private String openid;
    @TableField("unionid")
    private String unionid;
    @TableField("app_type")
    private String appType;     // mp/mini/open/web
    @TableField("nickname")
    private String nickname;
    @TableField("avatar")
    private String avatar;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime boundAt;

    private LocalDateTime lastLoginAt;
}
