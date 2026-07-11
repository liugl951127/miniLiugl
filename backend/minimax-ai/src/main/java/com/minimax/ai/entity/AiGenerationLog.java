package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_generation_log")
public class AiGenerationLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String generationId;
    private Long userId;
    private String username;
    private String userIp;
    private String modality;
    private String modelName;
    private String modelVersion;
    private String prompt;
    private String negativePrompt;
    private String parameters;
    private String outputUrl;
    private Long outputSize;
    private String outputHash;
    private Integer watermarked;
    private String watermarkText;
    private Integer durationMs;
    private String status;
    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
