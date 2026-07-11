package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
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
