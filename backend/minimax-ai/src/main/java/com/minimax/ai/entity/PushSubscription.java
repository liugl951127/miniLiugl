package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推送订阅 (V3.3.1)
 *
 * <p>用户设备订阅记录, 1 用户可订阅多设备
 * <p>兼容 Web Push / FCM / APNs (统一用 endpoint + p256dh + auth 字段)
 */
@Data
@TableName("push_subscription")
public class PushSubscription {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订阅 ID (UUID) */
    private String subscriptionId;
    /** 用户 ID */
    private Long userId;
    /** 设备类型: web / ios / android */
    private String platform;
    /** 端点 URL (Web Push endpoint / FCM token / APNs token) */
    private String endpoint;
    /** 加密公钥 (Web Push p256dh) */
    private String p256dhKey;
    /** 认证密钥 (Web Push auth) */
    private String authKey;
    /** 用户代理 (浏览器/设备描述) */
    private String userAgent;
    /** 状态: ACTIVE / EXPIRED / UNSUBSCRIBED */
    private String status;
    /** 最后活跃时间 */
    private LocalDateTime lastActiveAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
