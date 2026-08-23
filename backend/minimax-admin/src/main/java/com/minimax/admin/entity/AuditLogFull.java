package com.minimax.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_log_full")
public class AuditLogFull {
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
