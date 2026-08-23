package com.minimax.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * NL2SQL 调用历史 (V5.31)
 *
 * 记录用户自然语言问题 + LLM 生成的 SQL + 反馈 (用于训练样本)
 */
@Data
@TableName("analytics_nlsql_history")
public class Nl2SqlHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("data_source_id")
    private Long dataSourceId;       // 哪个数据源
    @TableField("question")
    private String question;          // 用户自然语言问题
    @TableField("generated_sql")
    private String generatedSql;      // LLM 生成的 SQL
    @TableField("corrected_sql")
    private String correctedSql;      // 用户修改后的 SQL (有则记录)
    @TableField("model")
    private String model;             // 用的模型
    @TableField("prompt_tokens")
    private Integer promptTokens;
    @TableField("completion_tokens")
    private Integer completionTokens;
    @TableField("duration_ms")
    private Long durationMs;
    @TableField("success")
    private Boolean success;          // 是否执行成功
    @TableField("error_message")
    private String errorMessage;
    @TableField("feedback_rating")
    private Integer feedbackRating;   // 1-5 星

    @TableField("created_at")
    private LocalDateTime createdAt;
}
