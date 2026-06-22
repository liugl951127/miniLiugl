package com.minimax.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分析报告 (V5.31)
 *
 * SQL 执行后, 自动生成 markdown 报告 + ECharts chart 配置
 */
@Data
@TableName("analytics_report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String reportId;          // UUID
    private String title;             // 用户给/自动生成的标题
    private String question;          // 原始问题 (NL2SQL)
    private String sqlText;           // 实际执行的 SQL
    private String markdown;          // 渲染好的 markdown
    private String chartOptionsJson;  // ECharts option
    private Long rowCount;            // 结果行数
    private Long durationMs;
    private String format;            // markdown / html

    private LocalDateTime createdAt;
}
