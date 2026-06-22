package com.minimax.analytics.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NL2SQL 结果 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Nl2SqlResult {
    private String question;           // 用户问题
    private String generatedSql;       // LLM 生成的 SQL
    private String explanation;        // LLM 对 SQL 的解释
    private Long durationMs;
    private String model;              // 用的模型
    private Integer promptTokens;
    private Integer completionTokens;

    // V5.31 扩展: 自动执行结果
    private Boolean executed;
    private QueryResult queryResult;   // 如果 autoExecute=true

    // V5.31 扩展: 报告 (如生成)
    private String reportId;           // 关联的 report id
    private String markdown;           // 简版报告
}
