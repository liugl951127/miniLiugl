package com.minimax.analytics.service.report;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.analytics.entity.Report;
import com.minimax.analytics.mapper.ReportMapper;
import com.minimax.analytics.service.chart.ChartService;
import com.minimax.analytics.service.report.AnomalyDetector.Anomaly;
import com.minimax.analytics.service.report.TrendAnalyzer.TrendPoint;
import com.minimax.analytics.vo.QueryResult;
import com.minimax.common.exception.BizException;
import com.minimax.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 报告服务实现 (V5.31)
 *
 * Markdown 模板: 标题/问题/SQL/数据预览/趋势/异常/总结
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final ChartService chartService;
    private final TrendAnalyzer trendAnalyzer;
    private final AnomalyDetector anomalyDetector;
    private final ObjectMapper json = new ObjectMapper();

    @Override
    public Report generate(Long userId, Long dataSourceId, String title, String question, String sql, QueryResult result) {
        long t0 = System.currentTimeMillis();
        Report r = new Report();
        r.setUserId(userId);
        r.setReportId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        r.setTitle(title != null ? title : (question != null ? question : "分析报告"));
        r.setQuestion(question);
        r.setSqlText(sql);
        r.setRowCount(result.getRowCount());
        r.setDurationMs(System.currentTimeMillis() - t0);
        r.setFormat("markdown");
        r.setCreatedAt(LocalDateTime.now());

        // 1. 趋势 (V5.31: 简单返回空, 留作扩展)
        List<TrendPoint> trend = Collections.emptyList();
        // 2. 异常
        List<Anomaly> anomalies = anomalyDetector.detect(result);

        // 3. Chart
        Map<String, Object> chartOption = chartService.autoChart(result);
        try { r.setChartOptionsJson(json.writeValueAsString(chartOption)); } catch (Exception ignored) {}

        // 4. Markdown
        String md = buildMarkdown(r, result, trend, anomalies, chartOption);
        r.setMarkdown(md);

        reportMapper.insert(r);
        return r;
    }

    @Override
    public Report getById(Long userId, String reportId) {
        Report r = require(userId, reportId);
        return r;
    }

    @Override
    public String markdown(String reportId) {
        // 简化: 不鉴权 (V5.31)
        Report r = reportMapper.selectOne(new LambdaQueryWrapper<Report>().eq(Report::getReportId, reportId));
        if (r == null) throw new BizException(ResultCode.NOT_FOUND, "报告不存在");
        return r.getMarkdown();
    }

    @Override
    public List<Report> history(Long userId, int page, int size) {
        return reportMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Report>().eq(Report::getUserId, userId)
                        .orderByDesc(Report::getCreatedAt)).getRecords();
    }

    // ---- helpers ----

    private Report require(Long userId, String reportId) {
        Report r = reportMapper.selectOne(new LambdaQueryWrapper<Report>().eq(Report::getReportId, reportId));
        if (r == null) throw new BizException(ResultCode.NOT_FOUND, "报告不存在");
        if (!r.getUserId().equals(userId)) throw new BizException(ResultCode.FORBIDDEN, "无权");
        return r;
    }

    private String buildMarkdown(Report r, QueryResult result, List<TrendPoint> trend, List<Anomaly> anomalies, Map<String, Object> chart) {
        StringBuilder md = new StringBuilder();
        md.append("# ").append(r.getTitle()).append("\n\n");
        if (r.getQuestion() != null) {
            md.append("> **问题**: ").append(r.getQuestion()).append("\n\n");
        }
        md.append("## SQL\n\n```sql\n").append(r.getSqlText()).append("\n```\n\n");
        md.append("## 数据概览\n\n");
        md.append("- 总行数: **").append(result.getRowCount()).append("**\n");
        md.append("- 执行耗时: **").append(result.getDurationMs()).append(" ms**\n");
        md.append("- 列: ").append(result.getColumns()).append("\n\n");
        if (result.getRows() != null && !result.getRows().isEmpty()) {
            md.append("## 数据预览 (前 ").append(Math.min(10, result.getRows().size())).append(" 行)\n\n");
            md.append(tableToMd(result.getColumns(), result.getRows().subList(0, Math.min(10, result.getRows().size()))));
            md.append("\n");
        }
        if (anomalies != null && !anomalies.isEmpty()) {
            md.append("## ⚠️ 异常点 (IQR / z-score)\n\n");
            for (Anomaly a : anomalies) {
                md.append("- ").append(a.field).append(" = ").append(a.value).append(" (index=").append(a.index).append(", score=").append(String.format("%.2f", a.score)).append(")\n");
            }
            md.append("\n");
        }
        if (chart != null) {
            md.append("## 图表\n\n类型: `").append(chart.get("type")).append("` (前端用 ECharts 渲染 option)\n\n");
        }
        md.append("---\n\n*报告生成于 ").append(r.getCreatedAt()).append("*\n");
        return md.toString();
    }

    private String tableToMd(List<String> cols, List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("| ").append(String.join(" | ", cols)).append(" |\n");
        sb.append("|").append(" --- |".repeat(cols.size())).append("\n");
        for (Map<String, Object> row : rows) {
            List<String> vals = new ArrayList<>();
            for (String c : cols) {
                Object v = row.get(c);
                vals.add(v == null ? "" : v.toString().replace("|", "\\|"));
            }
            sb.append("| ").append(String.join(" | ", vals)).append(" |\n");
        }
        return sb.toString();
    }
}
