package com.minimax.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alert_rule")
public class AlertRule {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;
    @TableField("description")
    private String description;
    @TableField("metric_name")
    private String metricName;
    @TableField("service")
    private String service;
    @TableField("operator")
    private String operator;
    private java.math.BigDecimal threshold;
    @TableField("severity")
    private String severity;
    @TableField("cooldown_minutes")
    private Integer cooldownMinutes;
    @TableField("enabled")
    private Integer enabled;
    @TableField("tags")
    private String tags;
    @TableField("notify_channel")
    private String notifyChannel;     // 兼容旧字段 (逗号分隔 ID)
    @TableField("silenced_until")
    private LocalDateTime silencedUntil; // 静默截止时间 (Day 35)
    /** V7.0 Flow⑤: 关联的会话ID (可选) */
    @TableField("session_id")
    private String sessionId;
    /** Day 45: 升级等待分钟数（CRITICAL 告警触发后超过此时间未解决则升级）*/
    @TableField("escalate_after_minutes")
    private Integer escalateAfterMinutes;
    /** Day 45: 升级通知渠道（逗号分隔，如 "DINGTALK,EMAIL"）*/
    @TableField("escalation_channel")
    private String escalationChannel;
    /** Day 45: 自动恢复分钟数（超过此时间自动从 firing 变为 resolved）*/
    @TableField("auto_resolve_minutes")
    private Integer autoResolveMinutes;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
