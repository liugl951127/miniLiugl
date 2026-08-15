package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
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

    private Long userId;
    private String token;
    private LocalDateTime expiresAt;
    private Integer revoked;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
