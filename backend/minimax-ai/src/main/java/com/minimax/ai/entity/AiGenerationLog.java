package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * AiGenerationLog (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * 数据库实体 - AiGenerationLog.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 AiGenerationLog 的业务能力</li>
 *   <li>参与 AI 平台整体架构</li>
 *   <li>支持 Spring 依赖注入</li>
 * </ul>
 *
 * <h2>依赖</h2>
 * <ul>
 *   <li>Spring Framework (自动注入)</li>
 *   <li>Lombok (简化代码)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V6.1
 */
@TableName("ai_generation_log")
public class AiGenerationLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("generation_id")
    private String generationId;
    @TableField("user_id")
    private Long userId;
    @TableField("username")
    private String username;
    @TableField("user_ip")
    private String userIp;
    @TableField("modality")
    private String modality;
    @TableField("model_name")
    private String modelName;
    @TableField("model_version")
    private String modelVersion;
    @TableField("prompt")
    private String prompt;
    @TableField("negative_prompt")
    private String negativePrompt;
    @TableField("parameters")
    private String parameters;
    @TableField("output_url")
    private String outputUrl;
    @TableField("output_size")
    private Long outputSize;
    @TableField("output_hash")
    private String outputHash;
    @TableField("watermarked")
    private Integer watermarked;
    @TableField("watermark_text")
    private String watermarkText;
    @TableField("duration_ms")
    private Integer durationMs;
    @TableField("status")
    private String status;
    @TableField("error_msg")
    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
