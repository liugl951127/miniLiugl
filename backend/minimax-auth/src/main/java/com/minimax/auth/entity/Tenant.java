package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField("code")
    private String code;
    @TableField("name")
    private String name;
    @TableField("plan")
    private String plan;
    @TableField("status")
    private Integer status;
    @TableField("max_users")
    private Integer maxUsers;
    @TableField("max_models")
    private Integer maxModels;
    @TableField("qps_limit")
    private Integer qpsLimit;
    @TableField("monthly_quota")
    private Long monthlyQuota;
    @TableField("used_quota")
    private Long usedQuota;
    @TableField("expire_at")
    private LocalDateTime expireAt;
    @TableField("contact_email")
    private String contactEmail;
    @TableField("contact_phone")
    private String contactPhone;
    @TableField("remark")
    private String remark;
    /** 数据隔离标记：true=隔离，false/null=共享（V6.9 后端支持） */
    @TableField("data_isolation")
    private Boolean dataIsolation;
    /** IP 白名单，多个逗号分隔（V6.9 后端支持） */
    @TableField("ip_whitelist")
    private String ipWhitelist;
    /** 是否默认租户：1=默认，不可删除 */
    @TableField("is_default")
    private Integer isDefault;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
