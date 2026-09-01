package com.minimax.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * RCA 分析结果知识条目 (Day 58).
 *
 * <p>存储已保存的 RCA 分析结果，供后续同类告警检索参考。
 * 由用户手动点击「保存到知识库」触发。
 *
 * <p>category 字段对应 AlertRcaService.RootCauseCategory 枚举值:
 * RESOURCE_BOTTLENECK / CONFIG_ERROR / EXTERNAL_DEPENDENCY / CODE_BUG /
 * TRAFFIC_SPIKE / NETWORK / UNKNOWN
 */
@Data
@TableName("alert_rca_knowledge")
public class AlertRcaKnowledge implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联告警 ID */
    @TableField("alert_id")
    private Long alertId;

    /** 指标名称 */
    @TableField("metric_name")
    private String metricName;

    /** 告警规则名 */
    @TableField("rule_name")
    private String ruleName;

    /** 告警级别: CRITICAL / WARNING / INFO */
    @TableField("severity")
    private String severity;

    /** 根因分类 */
    @TableField("category")
    private String category;

    /** 根因分析内容 */
    @TableField("cause")
    private String cause;

    /** 建议操作 (JSON 数组字符串) */
    @TableField("suggested_actions")
    private String suggestedActions;

    /** 置信度 0~1 */
    @TableField("confidence")
    private Double confidence;

    /** 分析方法: rule-based / llm / fallback */
    @TableField("method")
    private String method;

    /** 历史经验 (JSON 数组字符串) */
    @TableField("historical_knowledge")
    private String historicalKnowledge;

    /** 保存人用户 ID */
    @TableField("saved_by")
    private Long savedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
