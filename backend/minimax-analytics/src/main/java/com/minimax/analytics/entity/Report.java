package com.minimax.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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

    @TableField("user_id")
    private Long userId;
    @TableField("report_id")
    private String reportId;          // UUID
    @TableField("title")
    private String title;             // 用户给/自动生成的标题
    @TableField("question")
    private String question;          // 原始问题 (NL2SQL)
    @TableField("sql_text")
    private String sqlText;           // 实际执行的 SQL
    @TableField("markdown")
    private String markdown;          // 渲染好的 markdown
    @TableField("chart_options_json")
    private String chartOptionsJson;  // ECharts option
    @TableField("row_count")
    private Long rowCount;            // 结果行数
    @TableField("duration_ms")
    private Long durationMs;
    @TableField("format")
    private String format;            // markdown / html

    @TableField("created_at")
    private LocalDateTime createdAt;
}
