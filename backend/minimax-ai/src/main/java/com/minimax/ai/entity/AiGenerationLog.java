package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
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
