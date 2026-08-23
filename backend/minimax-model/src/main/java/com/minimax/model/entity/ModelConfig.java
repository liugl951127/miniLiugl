package com.minimax.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("model_config")
public class ModelConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("provider_id")
    private Long providerId;
    @TableField("model_code")
    private String modelCode;
    @TableField("display_name")
    private String displayName;
    @TableField("max_context")
    private Integer maxContext;
    @TableField("max_output")
    private Integer maxOutput;
    @TableField("input_price")
    private BigDecimal inputPrice;
    @TableField("output_price")
    private BigDecimal outputPrice;
    @TableField("supports_vision")
    private Integer supportsVision;
    @TableField("supports_tools")
    private Integer supportsTools;
    @TableField("supports_stream")
    private Integer supportsStream;
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
