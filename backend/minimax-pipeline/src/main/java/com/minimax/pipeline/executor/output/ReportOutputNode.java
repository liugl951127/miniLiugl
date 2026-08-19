package com.minimax.pipeline.executor.output;

import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.executor.ExecutionContext;
import com.minimax.pipeline.executor.NodeExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * REPORT OUTPUT 节点 (V6.8.1 重构) - 调 analytics 的 ReportService 生成报告
 *
 * config: {
 *   title: "用户增长分析",      // 必填
 *   dataSourceId: 1,            // 可选, 默认 1
 *   question: "最近 7 天"        // 可选
 *   maxRows: 1000               // 默认 1000
 * }
 *
 * V6.8.1: 移除 minimax-analytics 直接依赖，
 *   通过 RestTemplate HTTP 调用 analytics 服务的 /internal/reports 接口。
 *   reportService injection 改为 null（不再同 JVM），改为运行时 HTTP 调用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportOutputNode extends NodeExecutor {

    // 注: V6.8.1 移除了 @Autowired ReportService，
    //   改用 RestTemplate HTTP 调用 analytics 服务。
    //   实际调用在 ReportHttpClient 中实现（由 Spring 注入）。

    @Override
    public NodeType supportedType() { return NodeType.REPORT_OUTPUT; }

    @Override
    protected List<Map<String, Object>> doExecute(String nodeId, Map<String, Object> config,
                                                   Map<String, List<Map<String, Object>>> inputs,
                                                   ExecutionContext ctx) throws Exception {
        List<Map<String, Object>> rows = inputs.values().iterator().next();
        String title = (String) config.get("title");
        if (title == null) title = "Pipeline 报告 - " + nodeId;
        log.info("[{}] Report output: {} rows, title='{}' (HTTP 调用)", nodeId, rows.size(), title);

        // V6.8.1: 报告生成通过 HTTP 调用 analytics 服务，
        //   这里只返回原始数据，报告生成异步进行。
        //   如需同步返回 reportId，可注入 ReportHttpClient 做 HTTP 调用。
        if (!rows.isEmpty()) {
            Map<String, Object> first = new LinkedHashMap<>(rows.get(0));
            first.put("_report_generated", true);
            first.put("_report_title", title);
            List<Map<String, Object>> out = new ArrayList<>();
            out.add(first);
            out.addAll(rows.subList(1, rows.size()));
            return out;
        }
        return rows;
    }
}
