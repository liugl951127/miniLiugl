package com.minimax.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 告警通知渠道配置 (V5.33 Day 18).
 *
 * channel_type: EMAIL / DINGTALK
 * config JSON 格式:
 *   EMAIL:    {"email": "oncall@company.com", "smtpHost": "smtp.company.com", "smtpPort": 465}
 *   DINGTALK: {"webhook": "https://oapi.dingtalk.com/robot/send?access_token=xxx", "secret": "SEC..."}
 */
@Data
@TableName("alert_channel")
public class AlertChannel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String channelType;  // EMAIL / DINGTALK
    private String config;        // JSON
    private Integer enabled;
    private Integer priority;     // 越小优先级越高

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
