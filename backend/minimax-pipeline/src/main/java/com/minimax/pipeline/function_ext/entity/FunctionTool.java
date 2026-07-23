package com.minimax.pipeline.function_ext.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("function_tool")
public class FunctionTool implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String displayName;
    private String description;
    private String category;
    private String scope;
    private Long ownerId;
    private String parameters;   // JSON Schema string
    private String endpoint;     // FQN or HTTP URL
    private String httpMethod;
    private Integer enabled;
    private String tags;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
