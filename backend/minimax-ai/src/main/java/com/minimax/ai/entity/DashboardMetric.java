package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 看板指标 (V3.2.1)
 *
 * <p>每次聚合时存一条 (时间序列), 用于回看历史趋势
 */
@Data
@TableName("dashboard_metric")
public class DashboardMetric {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 指标名 (e.g. "user.total", "ai.call.count", "ai.tool.usage") */
    @TableField("metric")
    private String metric;
    /** 维度 (e.g. "global", "user:123", "tool:ppt.gen") */
    @TableField("dimension")
    private String dimension;
    /** 数值 */
    @TableField("value")
    private Double value;
    /** 额外标签 (JSON) */
    @TableField("tags")
    private String tags;
    /** 快照时间 (聚合时间) */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime timestamp;
}
