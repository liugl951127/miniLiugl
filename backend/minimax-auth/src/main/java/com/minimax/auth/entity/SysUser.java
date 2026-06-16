package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户实体。
 * 字段命名严格对齐 sys_user 表，避免歧义。
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 哈希，禁止明文落库。 */
    private String password;

    private String nickname;
    private String email;
    private String phone;
    private String avatar;

    /** 0未知 1男 2女 */
    private Integer gender;

    /** 0禁用 1正常 */
    private Integer status;

    private String lastLoginIp;
    private LocalDateTime lastLoginAt;
    private Long tenantId;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
