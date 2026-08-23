package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField("subscription_id")
    private String subscriptionId;
    /** 用户 ID */
    @TableField("user_id")
    private Long userId;
    /** 设备类型: web / ios / android */
    @TableField("platform")
    private String platform;
    /** 端点 URL (Web Push endpoint / FCM token / APNs token) */
    @TableField("endpoint")
    private String endpoint;
    /** 加密公钥 (Web Push p256dh) */
    @TableField("p256dh_key")
    private String p256dhKey;
    /** 认证密钥 (Web Push auth) */
    @TableField("auth_key")
    private String authKey;
    /** 用户代理 (浏览器/设备描述) */
    @TableField("user_agent")
    private String userAgent;
    /** 状态: ACTIVE / EXPIRED / UNSUBSCRIBED */
    @TableField("status")
    private String status;
    /** 最后活跃时间 */
    @TableField("last_active_at")
    private LocalDateTime lastActiveAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
