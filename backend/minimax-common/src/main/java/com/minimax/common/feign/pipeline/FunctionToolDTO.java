package com.minimax.common.feign.pipeline;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 跨服务共享的工具定义 DTO
 * pipeline → agent 通过 HTTP/Feign 传递
 */
@Data
public class FunctionToolDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String displayName;
    private String description;
    private String category;
    private String scope;
    private Long ownerId;
    /** JSON Schema string */
    private String parameters;
    /** FQN 或 HTTP URL */
    private String endpoint;
    private String httpMethod;
    private Integer enabled;
    private String tags;
    /** 风险等级: SAFE / LOW / MEDIUM / HIGH / CRITICAL */
    private String riskLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ==================== 常量 ====================
    public static final String RISK_SAFE     = "SAFE";
    public static final String RISK_LOW      = "LOW";
    public static final String RISK_MEDIUM   = "MEDIUM";
    public static final String RISK_HIGH     = "HIGH";
    public static final String RISK_CRITICAL = "CRITICAL";

    public static final String STATUS_ENABLED  = "ENABLED";
    public static final String STATUS_DISABLED = "DISABLED";

    /** 判断是否需要审批 */
    public boolean needsApproval() {
        return RISK_HIGH.equals(riskLevel) || RISK_CRITICAL.equals(riskLevel);
    }
}
