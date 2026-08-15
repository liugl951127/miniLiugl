package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推送消息记录 (V3.3.1)
 */
@Data
@TableName("push_message")
public class PushMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 消息 ID (UUID) */
    private String messageId;
    /** 标题 */
    private String title;
    /** 内容 */
    private String body;
    /** 图标 URL */
    private String icon;
    /** 点击 URL (action) */
    private String clickAction;
    /** 数据 payload (JSON) */
    private String data;
    /** 目标类型: all / user / topic */
    private String targetType;
    /** 目标值 (userId / topic 名) */
    private String targetValue;
    /** 状态: PENDING / SENT / FAILED / DELIVERED */
    private String status;
    /** 成功数 */
    private Integer successCount;
    /** 失败数 */
    private Integer failureCount;
    /** 错误信息 */
    private String error;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
