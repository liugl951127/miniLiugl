package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库权限 (V3.4.0 自研知识库)
 *
 * <p>user/kb 关联, 控制读/写/管理
 */
@Data
@TableName("kb_permission")
public class KbPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联知识库 kbId */
    @TableField("kb_id")
    private String kbId;
    /** 主体类型: USER / ROLE / ORG */
    @TableField("subject_type")
    private String subjectType;
    /** 主体 ID (userId / roleId / orgId) */
    @TableField("subject_id")
    private Long subjectId;
    /** 权限: READ / WRITE / ADMIN */
    @TableField("permission")
    private String permission;
    /** 授权人 */
    @TableField("grant_by")
    private Long grantBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
