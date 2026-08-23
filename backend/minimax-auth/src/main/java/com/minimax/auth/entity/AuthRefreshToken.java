package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("auth_refresh_token")
public class AuthRefreshToken implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("token")
    private String token;
    @TableField("expires_at")
    private LocalDateTime expiresAt;
    @TableField("revoked")
    private Integer revoked;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
