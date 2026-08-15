package com.minimax.pipeline.function_ext.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Skill/工具 审批记录 (V6.8.1+)
 *
 * 触发条件: Agent 调用的工具 riskLevel = HIGH 或 CRITICAL
 * 流程: PENDING → APPROVED/REJECTED → CANCELLED
 *
 * @since 2026-08-12
 */
@Data
@TableName("skill_approval")
public class SkillApproval implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的 Agent 任务 ID */
    private String taskId;

    /** 申请人 userId */
    private Long userId;

    /** 申请人姓名 */
    private String username;

    /** 申请执行的工具名 */
    private String toolName;

    /** 工具风险等级: HIGH / CRITICAL */
    private String riskLevel;

    /** Agent 执行目标 */
    private String goal;

    /** 工具调用参数 (JSON) */
    private String toolParams;

    /** 审批状态: PENDING / APPROVED / REJECTED / CANCELLED */
    private String status;

    /** 审批人 userId */
    private Long approverId;

    /** 审批人姓名 */
    private String approverName;

    /** 审批意见 */
    private String reason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    // ==================== 常量 ====================
    public static final String STATUS_PENDING   = "PENDING";
    public static final String STATUS_APPROVED  = "APPROVED";
    public static final String STATUS_REJECTED  = "REJECTED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final String RISK_HIGH      = "HIGH";
    public static final String RISK_CRITICAL = "CRITICAL";

    /** 需要审批的风险等级 */
    public static boolean needsApproval(String riskLevel) {
        return RISK_HIGH.equals(riskLevel) || RISK_CRITICAL.equals(riskLevel);
    }
}
