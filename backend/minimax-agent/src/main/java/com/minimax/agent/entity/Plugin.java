package com.minimax.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("plugin")
public class Plugin implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String displayName;
    private String description;
    private String version;
    private String author;
    private String category;
    private String scope;
    private Long ownerId;
    private String icon;
    private String entry;
    private String pluginType;
    private String config;
    private Integer enabled;
    private Integer downloads;
    private BigDecimal rating;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
