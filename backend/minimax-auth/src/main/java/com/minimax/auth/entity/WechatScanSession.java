package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 微信扫码会话 (V5).
 */
@Data
@TableName("wechat_scan_session")
public class WechatScanSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("ticket")
    private String ticket;
    @TableField("scene_id")
    private String sceneId;
    @TableField("status")
    private String status;

    @TableField("openid")
    private String openid;
    @TableField("unionid")
    private String unionid;
    @TableField("nickname")
    private String nickname;
    @TableField("avatar")
    private String avatar;

    @TableField("user_id")
    private Long userId;
    @TableField("access_token")
    private String accessToken;
    @TableField("refresh_token")
    private String refreshToken;

    @TableField("client_ip")
    private String clientIp;
    @TableField("user_agent")
    private String userAgent;

    @TableField("expires_at")
    private LocalDateTime expiresAt;
    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
