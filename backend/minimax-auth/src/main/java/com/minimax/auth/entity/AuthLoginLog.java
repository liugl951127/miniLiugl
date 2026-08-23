package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("auth_login_log")
public class AuthLoginLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("username")
    private String username;
    @TableField("ip")
    private String ip;
    @TableField("user_agent")
    private String userAgent;
    /** 0失败 1成功 */
    @TableField("status")
    private Integer status;
    @TableField("message")
    private String message;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
