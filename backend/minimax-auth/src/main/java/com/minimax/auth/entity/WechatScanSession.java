package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
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

    private String ticket;
    private String sceneId;
    private String status;

    private String openid;
    private String unionid;
    private String nickname;
    private String avatar;

    private Long userId;
    private String accessToken;
    private String refreshToken;

    private String clientIp;
    private String userAgent;

    private LocalDateTime expiresAt;
    private LocalDateTime confirmedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
