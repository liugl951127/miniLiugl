package com.minimax.ai.webhook;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Webhook 订阅 (V2.9.1)
 *
 * <p>用户可订阅平台事件, 当事件触发时, 平台向用户 URL 发送 HTTP POST 通知.</p>
 *
 * <h3>事件类型</h3>
 * <ul>
 *   <li>USER_LOGIN: 用户登录</li>
 *   <li>USER_REGISTER: 用户注册</li>
 *   <li>MODEL_TRAINED: 模型训练完成</li>
 *   <li>AGENT_PUBLISHED: Agent 发布到市场</li>
 *   <li>COLLAB_MESSAGE: 协作消息 (仅 OWNER)</li>
 *   <li>AUDIT_FAILED: 审计失败</li>
 *   <li>ALERT_TRIGGERED: 告警触发</li>
 *   <li>WEBHOOK_TEST: 测试 (Ping)</li>
 * </ul>
 *
 * <h3>安全</h3>
 * <ul>
 *   <li>HMAC-SHA256 签名: <code>X-Hub-Signature: sha256=...</code></li>
 *   <li>Header: <code>X-Webhook-Id / X-Webhook-Event / X-Webhook-Timestamp</code></li>
 *   <li>Body: JSON 序列化事件</li>
 * </ul>
 *
 * @author MiniMax
 * @since V2.9.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("webhook")
public class Webhook {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 唯一 ID (UUID) */
    @TableField("webhookId")
    private String webhookId;

    /** 名称 */
    @TableField("name")
    private String name;

    /** 描述 */
    @TableField("description")
    private String description;

    /** 订阅 URL (POST 接收) */
    @TableField("url")
    private String url;

    /** 事件类型 (逗号分隔) */
    @TableField("events")
    private String events;

    /** 密钥 (HMAC 签名) */
    @TableField("secret")
    private String secret;

    /** 自定义 Headers (JSON) */
    @TableField("customHeaders")
    private String customHeaders;

    /** 启用 */
    @TableField("enabled")
    private Integer enabled;

    /** 状态: ACTIVE / PAUSED / DISABLED */
    @TableField("status")
    private String status;

    /** 投递次数 */
    @TableField("deliveryCount")
    private Long deliveryCount;

    /** 成功次数 */
    @TableField("successCount")
    private Long successCount;

    /** 失败次数 */
    @TableField("failCount")
    private Long failCount;

    /** 最后投递时间 */
    @TableField("lastDeliveryAt")
    private LocalDateTime lastDeliveryAt;

    /** 最后投递状态码 */
    @TableField("lastStatus")
    private Integer lastStatus;

    /** 所有者用户 ID */
    @TableField("ownerId")
    private Long ownerId;

    @TableField(value = "createdAt", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updatedAt", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
