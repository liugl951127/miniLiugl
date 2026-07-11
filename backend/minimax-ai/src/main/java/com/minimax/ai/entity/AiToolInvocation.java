package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 工具调用记录
 */
@Data
@TableName("ai_tool_invocation")
public class AiToolInvocation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String toolCode;

    private Long userId;

    private String username;

    private String inputJson;

    private String outputJson;

    /** SUCCESS / FAILED / TIMEOUT */
    private String status;

    private String errorMessage;

    private Integer durationMs;

    private String ip;

    private String userAgent;

    private Long dataSourceId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
