package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * AuditLog (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * 数据库实体 - AuditLog.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 AuditLog 的业务能力</li>
 *   <li>参与 AI 平台整体架构</li>
 *   <li>支持 Spring 依赖注入</li>
 * </ul>
 *
 * <h2>依赖</h2>
 * <ul>
 *   <li>Spring Framework (自动注入)</li>
 *   <li>Lombok (简化代码)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V6.1
 */
@TableName("audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String traceId;
    private Long userId;
    private String username;
    private String userIp;
    private String userAgent;
    private String action;
    private String resourceType;
    private String resourceId;
    private String method;
    private String path;
    private String requestBody;
    private Integer responseStatus;
    private String result;
    private String errorMsg;
    private Integer durationMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
