package com.minimax.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("metric_snapshot")
public class MetricSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String service;
    private String metricName;
    private BigDecimal metricValue;
    private String tags;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime recordedAt;
}
