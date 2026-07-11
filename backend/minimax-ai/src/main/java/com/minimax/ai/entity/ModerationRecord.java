package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("moderation_record")
public class ModerationRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String traceId;
    private Long userId;
    private String username;
    private String contentType;
    private String contentHash;
    private Long contentSize;
    private String contentUrl;
    private String moderationStatus;
    private String riskLevel;
    private String riskLabels;
    private BigDecimal riskScore;
    private String moderator;
    private String rejectionReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
