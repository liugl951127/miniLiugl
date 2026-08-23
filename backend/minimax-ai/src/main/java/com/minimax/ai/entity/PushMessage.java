package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField("message_id")
    private String messageId;
    /** 标题 */
    @TableField("title")
    private String title;
    /** 内容 */
    @TableField("body")
    private String body;
    /** 图标 URL */
    @TableField("icon")
    private String icon;
    /** 点击 URL (action) */
    @TableField("click_action")
    private String clickAction;
    /** 数据 payload (JSON) */
    @TableField("data")
    private String data;
    /** 目标类型: all / user / topic */
    @TableField("target_type")
    private String targetType;
    /** 目标值 (userId / topic 名) */
    @TableField("target_value")
    private String targetValue;
    /** 状态: PENDING / SENT / FAILED / DELIVERED */
    @TableField("status")
    private String status;
    /** 成功数 */
    @TableField("success_count")
    private Integer successCount;
    /** 失败数 */
    @TableField("failure_count")
    private Integer failureCount;
    /** 错误信息 */
    @TableField("error")
    private String error;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
