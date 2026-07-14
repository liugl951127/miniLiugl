package com.minimax.ai.intent;

import java.util.*;

/**
 * 智能推荐引擎 (V3.5.8 新增)
 *
 * <h2>核心能力</h2>
 * 1. 根据用户意图推荐处理 Agent (chat / rag / analytics / function / multimodal)
 * 2. 推荐具体工具 (web_search / sql_query / chart_gen / export)
 * 3. 推荐数据源 (哪个 knowledge base / data source)
 * 4. 推荐处理流程 (single_turn / multi_turn / pipeline)
 * 5. 推荐下一轮追问 (提升对话深度)
 *
 * <h2>路由策略</h2>
 * <pre>
 *   greeting         → Agent.chat
 *   question (5W1H)  → Agent.rag (知识问答)
 *   complaint        → Agent.chat + 人工升级建议
 *   data_query       → Agent.analytics.sql_query
 *   data_analyze     → Agent.analytics.analyze + chart_gen
 *   data_compare     → Agent.analytics.compare
 *   data_predict     → Agent.analytics.predict (ML 推理)
 *   data_visualize   → Agent.analytics.chart
 *   data_report      → Agent.analytics.report_export
 *   function_call    → Agent.function (工具执行)
 *   rag_search       → Agent.rag.search
 * </pre>
 *
 * @author MiniMax
 * @since V3.5.8
 */
public final class SuggestionEngine {

    private SuggestionEngine() {}

    /** 意图 → Agent 路由 */
    private static final Map<String, String> AGENT_ROUTING = new HashMap<>();
    static {
        AGENT_ROUTING.put("greeting", "chat");
        AGENT_ROUTING.put("goodbye", "chat");
        AGENT_ROUTING.put("thanks", "chat");
        AGENT_ROUTING.put("question", "rag");
        AGENT_ROUTING.put("complaint", "chat");
        AGENT_ROUTING.put("consult", "rag");
        AGENT_ROUTING.put("order", "function");
        AGENT_ROUTING.put("complaint", "chat");
        // 数据分析
        AGENT_ROUTING.put("data_query", "analytics");
        AGENT_ROUTING.put("data_analyze", "analytics");
        AGENT_ROUTING.put("data_compare", "analytics");
        AGENT_ROUTING.put("data_predict", "analytics");
        AGENT_ROUTING.put("data_visualize", "analytics");
        AGENT_ROUTING.put("data_report", "analytics");
    }

    /** 意图 → 工具推荐 */
    private static final Map<String, List<String>> TOOL_RECOMMEND = new HashMap<>();
    static {
        TOOL_RECOMMEND.put("greeting", List.of("welcome_template"));
        TOOL_RECOMMEND.put("question", List.of("rag_search", "web_search"));
        TOOL_RECOMMEND.put("consult", List.of("rag_search"));
        TOOL_RECOMMEND.put("order", List.of("order_query", "order_cancel"));
        TOOL_RECOMMEND.put("complaint", List.of("complaint_create", "human_handoff"));
        TOOL_RECOMMEND.put("data_query", List.of("sql_query", "data_export"));
        TOOL_RECOMMEND.put("data_analyze", List.of("data_query", "statistic", "trend_detect"));
        TOOL_RECOMMEND.put("data_compare", List.of("data_query", "diff_calc"));
        TOOL_RECOMMEND.put("data_predict", List.of("data_query", "ml_forecast", "model_inference"));
        TOOL_RECOMMEND.put("data_visualize", List.of("data_query", "chart_generate"));
        TOOL_RECOMMEND.put("data_report", List.of("data_query", "report_template", "pdf_export"));
    }

    /** 问题类型 → 追问模板 */
    private static final Map<String, List<String>> FOLLOWUP_TEMPLATES = new HashMap<>();
    static {
        FOLLOWUP_TEMPLATES.put(QuestionClassifier.TYPE_WHAT, List.of(
                "想了解哪个方面?",
                "需要更详细的解释吗?",
                "需要举例说明吗?"
        ));
        FOLLOWUP_TEMPLATES.put(QuestionClassifier.TYPE_HOW, List.of(
                "需要具体步骤吗?",
                "需要代码示例吗?",
                "还有其他疑问吗?"
        ));
        FOLLOWUP_TEMPLATES.put(QuestionClassifier.TYPE_WHY, List.of(
                "想了解根本原因吗?",
                "需要具体例子吗?",
                "可以帮你分析具体情况"
        ));
        FOLLOWUP_TEMPLATES.put(QuestionClassifier.TYPE_QUERY, List.of(
                "需要过滤特定条件吗?",
                "需要按时间范围筛选吗?",
                "需要导出结果吗?"
        ));
        FOLLOWUP_TEMPLATES.put(QuestionClassifier.TYPE_COMPARE, List.of(
                "需要按什么维度对比?",
                "需要看趋势变化吗?",
                "需要生成对比图吗?"
        ));
        FOLLOWUP_TEMPLATES.put(QuestionClassifier.TYPE_ANALYZE, List.of(
                "需要细分到哪个维度?",
                "需要时间趋势分析吗?",
                "需要生成可视化吗?"
        ));
        FOLLOWUP_TEMPLATES.put(QuestionClassifier.TYPE_PREDICT, List.of(
                "需要预测多长时间?",
                "需要考虑哪些变量?",
                "需要置信区间吗?"
        ));
        FOLLOWUP_TEMPLATES.put(QuestionClassifier.TYPE_RECOMMEND, List.of(
                "有什么具体需求?",
                "更看重哪些方面?",
                "需要列出备选方案吗?"
        ));
        FOLLOWUP_TEMPLATES.put(QuestionClassifier.TYPE_CREATE, List.of(
                "需要什么样式?",
                "需要包含哪些字段?",
                "需要保存到哪?"
        ));
    }

    /**
     * 推荐路由结果
     */
    public record Suggestion(
            String agent,                  // 推荐的 Agent
            List<String> tools,            // 推荐的工具
            String process,                // 处理方式
            String chartType,              // 图表类型
            List<String> followups,        // 推荐追问
            double confidence,             // 推荐置信度
            String reason                  // 推荐理由
    ) {}

    /**
     * 生成推荐
     *
     * @param intentLabel      意图分类 (e.g. "data_analyze")
     * @param questionType     问题类型 (e.g. "query")
     * @param complexity       复杂度
     * @param intentConfidence 意图置信度
     * @return 推荐结果
     */
    public static Suggestion suggest(
            String intentLabel,
            String questionType,
            QuestionClassifier.Complexity complexity,
            double intentConfidence
    ) {
        // 1. 选 Agent
        String agent = AGENT_ROUTING.getOrDefault(intentLabel, "chat");

        // 2. 选工具
        List<String> tools = TOOL_RECOMMEND.getOrDefault(intentLabel, List.of());

        // 3. 选图表
        String chart = complexity != null ? complexity.suggestChartType() : "table";

        // 4. 选追问
        List<String> followups = FOLLOWUP_TEMPLATES.getOrDefault(questionType, List.of(
                "请告诉我更多细节"
        ));

        // 5. 选处理流程
        String process = complexity != null ? complexity.suggestProcess() : "DIRECT_QUERY";

        // 6. 置信度
        double conf = intentConfidence * 0.7 + 0.3;

        // 7. 理由
        String reason = String.format("基于意图 %s (类型 %s, 复杂度 %s) 推荐 %s 代理",
                intentLabel, questionType, complexity != null ? complexity.level() : "simple", agent);

        return new Suggestion(agent, tools, process, chart, followups, conf, reason);
    }
}
