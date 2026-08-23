package com.minimax.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField("service")
    private String service;
    @TableField("metric_name")
    private String metricName;
    @TableField("metric_value")
    private BigDecimal metricValue;
    @TableField("tags")
    private String tags;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime recordedAt;
}
