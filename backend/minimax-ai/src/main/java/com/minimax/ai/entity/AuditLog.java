package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
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

    @TableField("trace_id")
    private String traceId;
    @TableField("user_id")
    private Long userId;
    @TableField("username")
    private String username;
    @TableField("user_ip")
    private String userIp;
    @TableField("user_agent")
    private String userAgent;
    @TableField("action")
    private String action;
    @TableField("resource_type")
    private String resourceType;
    @TableField("resource_id")
    private String resourceId;
    @TableField("method")
    private String method;
    @TableField("path")
    private String path;
    @TableField("request_body")
    private String requestBody;
    @TableField("response_status")
    private Integer responseStatus;
    @TableField("result")
    private String result;
    @TableField("error_msg")
    private String errorMsg;
    @TableField("duration_ms")
    private Integer durationMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
