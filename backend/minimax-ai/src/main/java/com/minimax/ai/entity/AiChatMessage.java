package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * AiChatMessage (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * 数据库实体 - AiChatMessage.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 AiChatMessage 的业务能力</li>
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
@TableName("ai_chat_message")
public class AiChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private String role;

    private String content;

    private String toolCode;

    private String toolInput;

    private String toolOutput;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
