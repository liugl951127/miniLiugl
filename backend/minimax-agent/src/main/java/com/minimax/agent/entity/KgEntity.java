package com.minimax.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("kg_entity")
public class KgEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("name")
    private String name;
    @TableField("entity_type")
    private String entityType;
    @TableField("description")
    private String description;
    @TableField("aliases")
    private String aliases;
    @TableField("importance")
    private Integer importance;
    @TableField("source")
    private String source;
    @TableField("ref_count")
    private Integer refCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
