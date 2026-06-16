package com.minimax.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("admin_audit_log")
public class AdminAuditLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long actorId;
    private String actorName;
    private String action;
    private String resourceType;
    private String resourceId;
    private String detail;
    private String result;
    private String errorMsg;
    private String ip;
    private String userAgent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
