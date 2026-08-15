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
    private String notes;             // 确认备注 (Day 34)
    private Long duration;            // 持续时间(秒)
    private LocalDateTime silencedUntil; // 静默截止时间 (Day 35)
    /** V7.0 Flow⑤: 关联的会话ID (跳转到对话) */
    private String sessionId;
}
