package com.minimax.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField("actor_id")
    private Long actorId;
    @TableField("actor_name")
    private String actorName;
    @TableField("action")
    private String action;
    @TableField("resource_type")
    private String resourceType;
    @TableField("resource_id")
    private String resourceId;
    @TableField("detail")
    private String detail;
    @TableField("result")
    private String result;
    @TableField("error_msg")
    private String errorMsg;
    @TableField("ip")
    private String ip;
    @TableField("user_agent")
    private String userAgent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
