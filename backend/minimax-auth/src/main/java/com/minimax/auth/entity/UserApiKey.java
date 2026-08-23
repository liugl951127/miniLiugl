package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户 API Key 实体 (V5.33 Day 18).
 *
 * 存储策略:
 *   - key_hash: SHA-256 明文 Key，不可逆，用于验证
 *   - key_prefix: 前 8 位明文 + "****" 展示，如 "mmx_a1b2****"
 */
@Data
@TableName("user_api_key")
public class UserApiKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID */
    @TableField("user_id")
    private Long userId;

    /** Key 名称 */
    @TableField("name")
    private String name;

    /** SHA-256(Key) 存储 */
    @TableField("key_hash")
    private String keyHash;

    /** 展示前缀 mmx_xxxx */
    @TableField("key_prefix")
    private String keyPrefix;

    /** 权限范围，逗号分隔 */
    @TableField("scopes")
    private String scopes;

    /** 过期时间，NULL 表示永不过期 */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    /** 最后使用时间 */
    @TableField("last_used_at")
    private LocalDateTime lastUsedAt;

    /** 累计调用次数 */
    @TableField("use_count")
    private Long useCount;

    /** 0 禁用 / 1 启用 */
    @TableField("enabled")
    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
