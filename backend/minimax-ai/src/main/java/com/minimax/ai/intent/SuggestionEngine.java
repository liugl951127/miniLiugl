package com.minimax.ai.intent;

import java.util.*;

/**
 * 智能推荐引擎 (Suggestion Engine) (V3.5.8 新增)
 *
 * <h2>为什么需要推荐引擎</h2>
 * 意图识别后, 还需要决定:
 * <ul>
 *   <li>路由到哪个 Agent 处理 (chat/rag/analytics/function)</li>
 *   <li>调用哪些工具 (sql_query/chart_gen/rag_search)</li>
 *   <li>推荐什么数据源 (哪个 KB / 哪个表)</li>
 *   <li>推荐什么处理流程 (single_turn/multi_turn/pipeline)</li>
 *   <li>推荐下一轮追问 (引导用户说出更多)</li>
 * </ul>
 * 减少用户在多轮对话中反复表达的负担, 提升问题解决率.
 *
 * <h2>4 维推荐</h2>
 * <ol>
 *   <li><b>Agent 路由</b>: 11 意图 → 6 Agent</li>
 *   <li><b>工具推荐</b>: 11 意图 → 25 工具</li>
 *   <li><b>追问模板</b>: 12 问题类型 → 36 追问</li>
 *   <li><b>流程推荐</b>: 复杂度 → DIRECT_QUERY / FILTER_AGG / MULTI_JOIN</li>
 * </ol>
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
 * <h2>完整调用链</h2>
 * <pre>
 *   IntentPredictionService.predict()
 *     └─&gt; SuggestionEngine.suggest(intent, type, complexity, conf)
 *         ├─&gt; 查 AGENT_ROUTING 选 Agent
 *         ├─&gt; 查 TOOL_RECOMMEND 选工具
 *         ├─&gt; 查 FOLLOWUP_TEMPLATES 选追问
 *         └─&gt; 调 Complexity.suggestProcess() 选流程
 * </pre>
 *
 * @author MiniMax
 * @since V3.5.8
 */
public final class SuggestionEngine {

    /** 工具类不允许实例化 */
    private SuggestionEngine() {}

    // ═══════════════════════════════════════════════════════════
    // 配置: 11 意图 → 6 Agent 路由
    // ═══════════════════════════════════════════════════════════

    /**
     * 意图 → Agent 路由表.
     * <p>键: IntentPredictionService 输出的 intent 标签.
     * <p>值: Agent 名称 (chat / rag / analytics / function / multimodal).
     */
    private static final Map<String, String> AGENT_ROUTING = new HashMap<>();
    static {
        // 通用对话
        AGENT_ROUTING.put("greeting", "chat");
        AGENT_ROUTING.put("goodbye", "chat");
        AGENT_ROUTING.put("thanks", "chat");
        // 知识问答
        AGENT_ROUTING.put("question", "rag");
        AGENT_ROUTING.put("consult", "rag");
        // 业务执行
        AGENT_ROUTING.put("order", "function");
        AGENT_ROUTING.put("complaint", "chat");
        // 数据分析 (6 类)
        AGENT_ROUTING.put("data_query", "analytics");
        AGENT_ROUTING.put("data_analyze", "analytics");
        AGENT_ROUTING.put("data_compare", "analytics");
        AGENT_ROUTING.put("data_predict", "analytics");
        AGENT_ROUTING.put("data_visualize", "analytics");
        AGENT_ROUTING.put("data_report", "analytics");
    }

    // ═══════════════════════════════════════════════════════════
    // 配置: 11 意图 → 25 工具
    // ═══════════════════════════════════════════════════════════

    /**
     * 意图 → 推荐工具列表.
     * <p>键: 意图标签.
     * <p>值: 工具名称列表 (按优先级排序).
     */
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

    // ═══════════════════════════════════════════════════════════
    // 配置: 12 问题类型 → 36 追问模板
    // ═══════════════════════════════════════════════════════════

    /**
     * 问题类型 → 推荐追问模板.
     * <p>键: QuestionClassifier 输出 type.
     * <p>值: 3 个追问模板 (供 UI 渲染为快捷问题).
     */
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
     * 推荐结果.
     * <p>含 5 维推荐: agent / tools / process / chart / followups + 置信 + 理由.
     */
    public record Suggestion(
            String agent,                  // 推荐的 Agent
            List<String> tools,            // 推荐的工具列表
            String process,                // 处理方式
            String chartType,              // 图表类型
            List<String> followups,        // 推荐追问
            double confidence,             // 推荐置信度
            String reason                  // 推荐理由
    ) {}

    /**
     * 生成推荐 (主入口).
     * <p>输入: 意图 + 问题类型 + 复杂度 + 置信度, 输出 5 维推荐.
     *
     * @param intentLabel      意图分类 (e.g. "data_analyze")
     * @param questionType     问题类型 (e.g. "query")
     * @param complexity       复杂度 (从 QuestionClassifier.assess 得到)
     * @param intentConfidence 意图置信度 (用作推荐置信度基础)
     * @return 推荐结果 (含 agent/tools/process/chart/followups/confidence/reason)
     */
    public static Suggestion suggest(
            String intentLabel,
            String questionType,
            QuestionClassifier.Complexity complexity,
            double intentConfidence
    ) {
        // 步骤 1: 选 Agent (查 AGENT_ROUTING, 默认 chat)
        String agent = AGENT_ROUTING.getOrDefault(intentLabel, "chat");

        // 步骤 2: 选工具 (查 TOOL_RECOMMEND, 默认空)
        List<String> tools = TOOL_RECOMMEND.getOrDefault(intentLabel, List.of());

        // 步骤 3: 选图表 (调 Complexity.suggestChartType, 默认 table)
        String chart = complexity != null ? complexity.suggestChartType() : "table";

        // 步骤 4: 选追问 (查 FOLLOWUP_TEMPLATES, 默认通用追问)
        List<String> followups = FOLLOWUP_TEMPLATES.getOrDefault(questionType, List.of(
                "请告诉我更多细节"
        ));

        // 步骤 5: 选处理流程 (调 Complexity.suggestProcess)
        String process = complexity != null ? complexity.suggestProcess() : "DIRECT_QUERY";

        // 步骤 6: 计算推荐置信度 (意图置信度 × 0.7 + 0.3 兜底)
        //   0.3 兜底: 即使意图置信度低, 推荐也有基础可信度
        double conf = intentConfidence * 0.7 + 0.3;

        // 步骤 7: 生成推荐理由 (供调试 / UI 展示)
        String reason = String.format("基于意图 %s (类型 %s, 复杂度 %s) 推荐 %s 代理",
                intentLabel, questionType,
                complexity != null ? complexity.level() : "simple", agent);

        return new Suggestion(agent, tools, process, chart, followups, conf, reason);
    }
}
