package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * AiChatSession (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * 数据库实体 - AiChatSession.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 AiChatSession 的业务能力</li>
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
@TableName("ai_chat_session")
public class AiChatSession {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private Long userId;

    private String username;

    private String title;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * V6.8.1: 会话状态 (0=inactive, 1=active, 2=archived)
     */
    private Integer status = 1;

    /**
     * V6.8.1: 关联意图 (来自 IntentService.RecognitionResult)
     */
    private String intent;

    /**
     * V6.8.1: 意图置信度
     */
    private Double confidence;

    /**
     * V6.8.1: 备选意图列表 (JSON 格式)
     */
    private String alternatives;

    /**
     * V6.8.1: 关联模型
     */
    private String model;

    /**
     * V7.0: 关联知识库 ID (RAG 用)
     */
    private Long kbId;

    /**
     * V7.0: 知识库名称 (冗余展示用)
     */
    private String kbName;

    /**
     * V7.0: 关联 Agent ID (委托执行)
     */
    private String agentId;

    /**
     * V7.0: Agent 名称 (冗余展示用)
     */
    private String agentName;

    @TableLogic
    private Integer deleted;
}
