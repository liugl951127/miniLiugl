package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
/**
 * ModerationRecord (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * 数据库实体 - ModerationRecord.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 ModerationRecord 的业务能力</li>
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
@TableName("moderation_record")
public class ModerationRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("trace_id")
    private String traceId;
    @TableField("user_id")
    private Long userId;
    @TableField("username")
    private String username;
    @TableField("content_type")
    private String contentType;
    @TableField("content_hash")
    private String contentHash;
    @TableField("content_size")
    private Long contentSize;
    @TableField("content_url")
    private String contentUrl;
    @TableField("moderation_status")
    private String moderationStatus;
    @TableField("risk_level")
    private String riskLevel;
    @TableField("risk_labels")
    private String riskLabels;
    @TableField("risk_score")
    private BigDecimal riskScore;
    @TableField("moderator")
    private String moderator;
    @TableField("rejection_reason")
    private String rejectionReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
