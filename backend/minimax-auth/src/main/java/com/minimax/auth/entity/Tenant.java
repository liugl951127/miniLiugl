package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("tenant")
public class Tenant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String plan;
    private Integer status;
    private Integer maxUsers;
    private Integer maxModels;
    private Integer qpsLimit;
    private Long monthlyQuota;
    private Long usedQuota;
    private LocalDateTime expireAt;
    private String contactEmail;
    private String contactPhone;
    private String remark;
    /** 数据隔离标记：true=隔离，false/null=共享（V6.9 后端支持） */
    private Boolean dataIsolation;
    /** IP 白名单，多个逗号分隔（V6.9 后端支持） */
    private String ipWhitelist;
    /** 是否默认租户：1=默认，不可删除 */
    private Integer isDefault;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
