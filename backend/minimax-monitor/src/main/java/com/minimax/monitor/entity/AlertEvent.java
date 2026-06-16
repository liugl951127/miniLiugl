package com.minimax.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("alert_event")
public class AlertEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ruleId;
    private String ruleName;
    private String severity;
    private String metricName;
    private BigDecimal metricValue;
    private BigDecimal threshold;
    private String message;
    private String status;
    private LocalDateTime firedAt;
    private LocalDateTime resolvedAt;
}
