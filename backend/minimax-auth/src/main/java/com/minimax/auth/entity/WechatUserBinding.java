package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
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

    private Long userId;
    private String openid;
    private String unionid;
    private String appType;     // mp/mini/open/web
    private String nickname;
    private String avatar;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime boundAt;

    private LocalDateTime lastLoginAt;
}
