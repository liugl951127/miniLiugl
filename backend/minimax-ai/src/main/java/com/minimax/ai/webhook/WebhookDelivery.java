package com.minimax.ai.webhook;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * WebhookDelivery (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * Webhook - WebhookDelivery.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 WebhookDelivery 的业务能力</li>
 *   <li>参与 AI 平台整体架构</li>
 *   <li>支持 Spring 依赖注入</li>
 * </ul>
 *
 * <h2>依赖</h2>
 * <ul>
 *   <li>Spring Framework (自动注入)</li>
 *   <li>Lombok (简化代码)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V6.1
 */
@TableName("webhook_delivery")
public class WebhookDelivery {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("webhookId")
    private String webhookId;

    /** 事件类型 */
    @TableField("eventType")
    private String eventType;

    /** 事件 ID (UUID) */
    @TableField("eventId")
    private String eventId;

    /** 事件负载 (JSON) */
    @TableField("payload")
    private String payload;

    /** 响应状态码 */
    @TableField("responseStatus")
    private Integer responseStatus;

    /** 响应体 (前 1000 字符) */
    @TableField("responseBody")
    private String responseBody;

    /** 耗时 (ms) */
    @TableField("durationMs")
    private Long durationMs;

    /** 状态: SUCCESS / FAILED / PENDING / RETRY */
    @TableField("status")
    private String status;

    /** 重试次数 */
    @TableField("retryCount")
    private Integer retryCount;

    /** 错误信息 */
    @TableField("errorMsg")
    private String errorMsg;

    @TableField(value = "createdAt", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
