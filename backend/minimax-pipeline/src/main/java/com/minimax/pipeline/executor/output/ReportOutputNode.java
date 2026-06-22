package com.minimax.pipeline.executor.output;

import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.executor.ExecutionContext;
import com.minimax.pipeline.executor.NodeExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * REPORT OUTPUT 节点 (V5.32) - 调 analytics 的 ReportService 生成报告
 *
 * config: {
 *   title: "用户增长分析",      // 必填
 *   dataSourceId: 1,            // 可选, 默认 1
 *   question: "最近 7 天"        // 可选
 *   maxRows: 1000               // 默认 1000
 * }
 *
 * V5.32 简化: 同模块直接注入, 不走 Feign
 *   (analytics 已经在同 jvm, 因为 minimax-pipeline 依赖 minimax-analytics)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportOutputNode extends NodeExecutor {

    @Autowired(required = false)
    private com.minimax.analytics.service.report.ReportService reportService;

    @Override
    public NodeType supportedType() { return NodeType.REPORT_OUTPUT; }

    @Override
    protected List<Map<String, Object>> doExecute(String nodeId, Map<String, Object> config,
                                                   Map<String, List<Map<String, Object>>> inputs,
                                                   ExecutionContext ctx) throws Exception {
        List<Map<String, Object>> rows = inputs.values().iterator().next();
        String title = (String) config.get("title");
        if (title == null) title = "Pipeline 报告 - " + nodeId;
        log.info("[{}] Report output: {} rows, title='{}'", nodeId, rows.size(), title);

        if (reportService == null) {
            log.warn("[{}] ReportService 未注入, 跳过实际生成 (V5.32 简化)", nodeId);
            return rows;
        }

        Long dataSourceId = config.get("dataSourceId") == null ? 1L : ((Number) config.get("dataSourceId")).longValue();
        String question = (String) config.get("question");
        String sql = "/* Pipeline 虚拟 SQL */ SELECT 1";  // V5.32 简化: 不重跑, 直接拿数据
        // V5.32 简化: 用 QueryResult 包装, ReportService.generate
        com.minimax.analytics.vo.QueryResult qr = com.minimax.analytics.vo.QueryResult.builder()
                .columns(rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet()))
                .rows(rows)
                .rowCount((long) rows.size())
                .durationMs(0L)
                .build();
        com.minimax.analytics.entity.Report report = reportService.generate(ctx.getUserId() == null ? 0L : ctx.getUserId(),
                dataSourceId, title, question, sql, qr);
        log.info("[{}] Report created: id={}, reportId={}", nodeId, report.getId(), report.getReportId());

        if (!rows.isEmpty()) {
            Map<String, Object> first = new LinkedHashMap<>(rows.get(0));
            first.put("_report_id", report.getReportId());
            List<Map<String, Object>> out = new ArrayList<>();
            out.add(first);
            out.addAll(rows.subList(1, rows.size()));
            return out;
        }
        return rows;
    }
}
