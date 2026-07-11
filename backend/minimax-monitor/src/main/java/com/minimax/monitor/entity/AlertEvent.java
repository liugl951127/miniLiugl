package com.minimax.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alert_event")
public class AlertEvent {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ruleId;
    private String ruleName;
    private String severity;
    private String metricName;
    private java.math.BigDecimal metricValue;
    private java.math.BigDecimal threshold;
    private String message;
    private String status;            // firing / acked / resolved
    private LocalDateTime firedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime ackedAt;
    private Long ackedBy;
    private Long duration;            // 持续时间(秒)
}
