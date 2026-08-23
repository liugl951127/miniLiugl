package com.minimax.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alert_event")
public class AlertEvent {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("rule_id")
    private Long ruleId;
    @TableField("rule_name")
    private String ruleName;
    @TableField("severity")
    private String severity;
    @TableField("metric_name")
    private String metricName;
    private java.math.BigDecimal metricValue;
    private java.math.BigDecimal threshold;
    @TableField("message")
    private String message;
    @TableField("status")
    private String status;            // firing / acked / resolved
    @TableField("fired_at")
    private LocalDateTime firedAt;
    @TableField("resolved_at")
    private LocalDateTime resolvedAt;
    @TableField("acked_at")
    private LocalDateTime ackedAt;
    @TableField("acked_by")
    private Long ackedBy;
    @TableField("notes")
    private String notes;             // 确认备注 (Day 34)
    @TableField("duration")
    private Long duration;            // 持续时间(秒)
    @TableField("silenced_until")
    private LocalDateTime silencedUntil; // 静默截止时间 (Day 35)
    /** V7.0 Flow⑤: 关联的会话ID (跳转到对话) */
    @TableField("session_id")
    private String sessionId;
    /** Day 45: 是否已升级 (true=已触发升级通知) */
    @TableField("escalated")
    private Boolean escalated;
    /** Day 45: 升级时间 */
    @TableField("escalated_at")
    private LocalDateTime escalatedAt;
    /** Day 46: 自动恢复操作人 (SYSTEM = 自动恢复, 其他 = 用户ID) */
    @TableField("resolved_by")
    private String resolvedBy;
}
