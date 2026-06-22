package com.minimax.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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

    private Long userId;
    private Long dataSourceId;       // 哪个数据源
    private String question;          // 用户自然语言问题
    private String generatedSql;      // LLM 生成的 SQL
    private String correctedSql;      // 用户修改后的 SQL (有则记录)
    private String model;             // 用的模型
    private Integer promptTokens;
    private Integer completionTokens;
    private Long durationMs;
    private Boolean success;          // 是否执行成功
    private String errorMessage;
    private Integer feedbackRating;   // 1-5 星

    private LocalDateTime createdAt;
}
