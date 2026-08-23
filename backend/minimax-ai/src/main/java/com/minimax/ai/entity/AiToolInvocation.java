package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
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

    @TableField("tool_code")
    private String toolCode;

    @TableField("user_id")
    private Long userId;

    @TableField("username")
    private String username;

    @TableField("input_json")
    private String inputJson;

    @TableField("output_json")
    private String outputJson;

    /** SUCCESS / FAILED / TIMEOUT */
    @TableField("status")
    private String status;

    @TableField("error_message")
    private String errorMessage;

    @TableField("duration_ms")
    private Integer durationMs;

    @TableField("ip")
    private String ip;

    @TableField("user_agent")
    private String userAgent;

    @TableField("data_source_id")
    private Long dataSourceId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
