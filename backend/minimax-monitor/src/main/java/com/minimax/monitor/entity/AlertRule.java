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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
