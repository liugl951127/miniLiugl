package com.minimax.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("model_provider")
public class ModelProvider implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("code")
    private String code;
    @TableField("name")
    private String name;
    @TableField("base_url")
    private String baseUrl;
    @TableField("api_key")
    private String apiKey;
    /** openai / anthropic / ollama */
    @TableField("protocol")
    private String protocol;
    @TableField("enabled")
    private Integer enabled;
    @TableField("sort")
    private Integer sort;
    @TableField("description")
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
