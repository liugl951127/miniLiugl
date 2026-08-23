package com.minimax.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField("name")
    private String name;
    @TableField("display_name")
    private String displayName;
    @TableField("description")
    private String description;
    @TableField("version")
    private String version;
    @TableField("author")
    private String author;
    @TableField("category")
    private String category;
    @TableField("scope")
    private String scope;
    @TableField("owner_id")
    private Long ownerId;
    @TableField("icon")
    private String icon;
    @TableField("entry")
    private String entry;
    @TableField("plugin_type")
    private String pluginType;
    @TableField("config")
    private String config;
    @TableField("enabled")
    private Integer enabled;
    @TableField("downloads")
    private Integer downloads;
    @TableField("rating")
    private BigDecimal rating;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
