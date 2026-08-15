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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
