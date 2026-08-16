package com.minimax.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alert_rule")
public class AlertRule {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private String metricName;
    private String service;
    private String operator;
    private java.math.BigDecimal threshold;
    private String severity;
    private Integer cooldownMinutes;
    private Integer enabled;
    private String tags;
    private String notifyChannel;     // 兼容旧字段 (逗号分隔 ID)
    private LocalDateTime silencedUntil; // 静默截止时间 (Day 35)
    /** V7.0 Flow⑤: 关联的会话ID (可选) */
    private String sessionId;
    /** Day 45: 升级等待分钟数（CRITICAL 告警触发后超过此时间未解决则升级）*/
    private Integer escalateAfterMinutes;
    /** Day 45: 升级通知渠道（逗号分隔，如 "DINGTALK,EMAIL"）*/
    private String escalationChannel;
    /** Day 45: 自动恢复分钟数（超过此时间自动从 firing 变为 resolved）*/
    private Integer autoResolveMinutes;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
