package com.minimax.common.feign.pipeline;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 跨服务共享的审批记录 DTO
 * pipeline → agent 通过 HTTP/Feign 传递
 */
@Data
public class SkillApprovalDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ==================== 常量 ====================
    public static final String STATUS_PENDING   = "PENDING";
    public static final String STATUS_APPROVED  = "APPROVED";
    public static final String STATUS_REJECTED  = "REJECTED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final String RISK_HIGH     = "HIGH";
    public static final String RISK_CRITICAL = "CRITICAL";

    public static boolean needsApproval(String riskLevel) {
        return RISK_HIGH.equals(riskLevel) || RISK_CRITICAL.equals(riskLevel);
    }
}
