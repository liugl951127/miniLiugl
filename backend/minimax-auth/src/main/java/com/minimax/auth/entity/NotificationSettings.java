package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知设置 (T1-backend-apis / P0)
 *
 * 每用户一条 (uk_userId 唯一约束), 用于持久化前端"通知设置"对话框内容。
 *
 * 字段含义:
 *   - channels:  启用的通知渠道, 逗号分隔 [email, sms, dingtalk, webhook, push]
 *   - events:    启用的通知事件, 逗号分隔 [login, error, alert, system]
 *   - quietStart/quietEnd: 免打扰时段 (HH:mm)
 *
 * @since V7.2
 */
@Data
@TableName("notification_settings")
public class NotificationSettings {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 渠道列表 (CSV) */
    private String channels;

    /** 事件列表 (CSV) */
    private String events;

    /** 免打扰开始 (HH:mm) */
    private String quietStart;

    /** 免打扰结束 (HH:mm) */
    private String quietEnd;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
