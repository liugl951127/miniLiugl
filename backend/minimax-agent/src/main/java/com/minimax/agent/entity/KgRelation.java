package com.minimax.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("kg_relation")
public class KgRelation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("from_entity")
    private Long fromEntity;
    @TableField("to_entity")
    private Long toEntity;
    @TableField("relation_type")
    private String relationType;
    @TableField("description")
    private String description;
    @TableField("weight")
    private BigDecimal weight;
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
