package com.minimax.analytics.service.report;

import com.minimax.analytics.entity.Report;
import com.minimax.analytics.vo.QueryResult;

import java.util.List;

/**
 * 报告服务接口 (V5.31)
 *
 * SQL + 数据 → Markdown 报告 + ECharts 配置
 */
public interface ReportService {

    /** 生成报告 (含 SQL/数据/趋势/异常) */
    Report generate(Long userId, Long dataSourceId, String title, String question, String sql, QueryResult result);

    /** 报告详情 */
    Report getById(Long userId, String reportId);

    /** Markdown 文本 */
    String markdown(String reportId);

    /** 历史报告 */
    List<Report> history(Long userId, int page, int size);
}
