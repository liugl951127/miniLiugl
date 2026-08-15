package com.minimax.model.entity;

import com.baomidou.mybatisplus.annotation.*;
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
    private Long providerId;
    private String modelCode;
    private String displayName;
    private Integer maxContext;
    private Integer maxOutput;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private Integer supportsVision;
    private Integer supportsTools;
    private Integer supportsStream;
    private Integer enabled;
    private Integer sort;
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
