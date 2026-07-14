package com.minimax.ai.intent;

import java.util.*;

/**
 * 数据分析意图识别引擎 (Data Analysis Engine) (V3.5.8 新增)
 *
 * <h2>为什么独立引擎</h2>
 * 通用 Intent 分类 (greeting/question/complaint...) 难以表达数据查询场景:
 * <ul>
 *   <li>"对比上季度各产品销售" → 不是普通问句, 是数据分析</li>
 *   <li>"预测下月销量" → 不是闲聊, 是 ML 预测</li>
 *   <li>"画个折线图" → 不是文本生成, 是可视化</li>
 * </ul>
 * 独立 DataAnalysisEngine 专门识别 6 种数据分析意图 + 5 类数据实体.
 *
 * <h2>6 种数据分析意图</h2>
 * <table border="1">
 *   <tr><th>意图</th><th>说明</th><th>典型触发词</th></tr>
 *   <tr><td>data_query</td><td>查数据 (单条件)</td><td>查询 / 列出 / 多少 / 几个</td></tr>
 *   <tr><td>data_analyze</td><td>分析 (多维度)</td><td>分析 / 统计 / 占比 / 趋势 / 排名</td></tr>
 *   <tr><td>data_compare</td><td>对比 (2+ 实体)</td><td>对比 / 比较 / 区别 / vs / 哪个好</td></tr>
 *   <tr><td>data_predict</td><td>预测 (未来)</td><td>预测 / 预估 / 未来 / 走势</td></tr>
 *   <tr><td>data_visualize</td><td>可视化 (图表)</td><td>画 / 图表 / 折线 / 柱状 / 饼图</td></tr>
 *   <tr><td>data_report</td><td>报告 (汇总)</td><td>报告 / 报表 / 汇总 / 周报 / 导出</td></tr>
 * </table>
 *
 * <h2>5 类数据实体</h2>
 * 额外识别数据相关实体, 供 SQL 生成器 / Agent 调用:
 * <ul>
 *   <li><b>metric</b>: 业务指标 (销售额/订单数/用户数/转化率/库存)</li>
 *   <li><b>dimension</b>: 维度 (地区/产品/渠道/用户/部门)</li>
 *   <li><b>timeRange</b>: 时间范围 (今天/上周/上月/近 7 天/2024 年)</li>
 *   <li><b>aggregation</b>: 聚合函数 (总数/平均/最大/最小/Top N)</li>
 *   <li><b>dataSource</b>: 数据源 (订单/用户/产品/库存)</li>
 * </ul>
 *
 * <h2>打分算法</h2>
 * <pre>
 *   score(intent) = Σ (trigger.length > 2 ? 0.4 : 0.2)
 *   confidence   = min(1.0, score)
 *   matched      = confidence > 0.15
 * </pre>
 * <p>长度加权: 长词 (≥3 字) 权重 0.4, 短词 0.2.
 * 原因: 长词特异性高 (例: "对比" 比 "比" 更精准).
 *
 * <h2>完整调用链</h2>
 * <pre>
 *   IntentPredictionService.predict()
 *     └─&gt; DataAnalysisEngine.recognize(text)
 *         ├─&gt; 遍历 INTENT_TRIGGERS
 *         ├─&gt; 累计每个 intent 命中分
 *         ├─&gt; 取 top1 作为 best
 *         └─&gt; extractEntities() 抽 5 类实体
 * </pre>
 *
 * @author MiniMax
 * @since V3.5.8
 */
public final class DataAnalysisEngine {

    /** 工具类不允许实例化 */
    private DataAnalysisEngine() {}

    // ═══════════════════════════════════════════════════════════
    // 6 种数据分析意图常量 (供外部引用)
    // ═══════════════════════════════════════════════════════════

    /** 查数据 (单条件) - 简单查询场景 */
    public static final String INTENT_DATA_QUERY = "data_query";
    /** 分析 (多维度) - 统计分析场景 */
    public static final String INTENT_DATA_ANALYZE = "data_analyze";
    /** 对比 (2+ 实体) - 对比分析场景 */
    public static final String INTENT_DATA_COMPARE = "data_compare";
    /** 预测 (未来) - ML 预测场景 */
    public static final String INTENT_DATA_PREDICT = "data_predict";
    /** 可视化 (图表) - 图表生成场景 */
    public static final String INTENT_DATA_VISUALIZE = "data_visualize";
    /** 报告 (汇总) - 报表导出场景 */
    public static final String INTENT_DATA_REPORT = "data_report";

    // ═══════════════════════════════════════════════════════════
    // 触发词配置 (按权重排序, 短语在前单词在后)
    // ═══════════════════════════════════════════════════════════

    /**
     * 6 意图的触发词字典.
     * <p>LinkedHashMap 保序: 长词在前 (权重 0.4), 短词在后 (权重 0.2).
     */
    private static final Map<String, List<String>> INTENT_TRIGGERS = new LinkedHashMap<>();
    static {
        // data_query: 简单查询
        INTENT_TRIGGERS.put(INTENT_DATA_QUERY, List.of(
                "查询", "查", "看看", "列出", "显示", "告诉我", "多少", "有几个"
        ));
        // data_analyze: 统计分析
        INTENT_TRIGGERS.put(INTENT_DATA_ANALYZE, List.of(
                "分析", "统计", "占比", "分布", "排名", "排序", "analyze", "statistic", "rank"
        ));
        // data_compare: 对比分析
        INTENT_TRIGGERS.put(INTENT_DATA_COMPARE, List.of(
                "对比", "比较", "区别", "vs", "哪个好", "哪个多", "差异", "compare", "versus"
        ));
        // data_predict: ML 预测
        INTENT_TRIGGERS.put(INTENT_DATA_PREDICT, List.of(
                "预测", "预估", "未来", "走势", "明年", "下月", "下个季度", "forecast", "predict"
        ));
        // data_visualize: 图表可视化
        INTENT_TRIGGERS.put(INTENT_DATA_VISUALIZE, List.of(
                "画", "图表", "折线", "柱状", "饼图", "可视化", "画图", "chart", "graph", "plot", "visualize"
        ));
        // data_report: 报表导出
        INTENT_TRIGGERS.put(INTENT_DATA_REPORT, List.of(
                "报告", "报表", "汇总", "周报", "月报", "季报", "年报", "导出", "export", "report"
        ));
    }

    // ═══════════════════════════════════════════════════════════
    // 5 类实体触发词
    // ═══════════════════════════════════════════════════════════

    /** 趋势词 (上升/下降/同比/环比) */
    private static final Set<String> TREND_TRIGGERS = Set.of(
            "趋势", "走势", "变化", "增长", "下降", "涨幅", "跌幅", "同比", "环比"
    );

    /** 业务指标: 销售额/订单数/用户数/转化率/库存 等 */
    private static final Set<String> METRIC_TRIGGERS = Set.of(
            "销售额", "营收", "收入", "利润", "成本", "毛利",
            "订单数", "订单量", "访问量", "点击量", "pv", "uv",
            "用户数", "客户数", "会员数", "注册数", "活跃用户",
            "转化率", "留存率", "复购率", "退款率",
            "库存", "销量", "产量", "报废率"
    );

    /** 维度: 地区/产品/渠道/用户/部门 */
    private static final Set<String> DIMENSION_TRIGGERS = Set.of(
            "地区", "城市", "省份", "国家", "区域",
            "产品", "品类", "商品", "服务",
            "渠道", "来源", "平台", "店铺",
            "用户", "客户", "会员", "员工",
            "部门", "团队", "小组"
    );

    /** 时间范围: 今天/上周/上月/近 7 天/2024 年 等 */
    private static final Set<String> TIME_TRIGGERS = Set.of(
            "今天", "昨天", "明天", "前天", "后天",
            "本周", "上周", "下周",
            "本月", "上月", "下月",
            "本季度", "上季度", "下季度",
            "今年", "去年", "明年",
            "近7天", "近30天", "近一年", "近一月",
            "2020", "2021", "2022", "2023", "2024", "2025", "2026",
            "年初", "年末", "季度", "月度", "周", "日"
    );

    /** 聚合函数: 总数/平均/最大/最小/Top N */
    private static final Set<String> AGG_TRIGGERS = Set.of(
            "总数", "总和", "求和", "合计", "累计",
            "平均", "均值", "中位数",
            "最大", "最小", "最高", "最低",
            "Top", "前10", "前5", "前3",
            "sum", "avg", "count", "max", "min"
    );

    /** 数据源关键词: 订单/用户/产品/库存 */
    private static final Set<String> SOURCE_TRIGGERS = Set.of(
            "订单", "用户", "客户", "产品", "商品", "库存",
            "支付", "退款", "物流",
            "订单表", "用户表", "产品表",
            "订单记录", "销售记录", "访问日志"
    );

    /**
     * 是否属于数据分析场景 (粗筛).
     * <p>用于: 在 IntentPredictionService.predict() 决策是否走数据分析路径.
     *
     * @param text 已归一化文本
     * @return true=含数据分析特征, false=普通对话
     */
    public static boolean isDataAnalysis(String text) {
        if (text == null || text.isBlank()) return false;
        // 路径 1: 含数据指标词 (强信号)
        for (String m : METRIC_TRIGGERS) {
            if (text.contains(m)) return true;
        }
        // 路径 2: 含分析触发词
        for (var e : INTENT_TRIGGERS.entrySet()) {
            for (String trigger : e.getValue()) {
                if (text.contains(trigger)) {
                    // 排除明显的非数据分析场景 (例: "查询订单状态" 是客服, 不是分析)
                    if (isNonAnalysis(text, trigger)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 排除明显非数据分析场景.
     * <p>例: "查询订单状态" - "查询" 触发但 "状态" 表明是客服意图.
     *
     * @param text    文本
     * @param trigger 当前命中的触发词
     * @return true=应该排除
     */
    private static boolean isNonAnalysis(String text, String trigger) {
        // "查询" 触发但文本含 "状态" / "物流" → 客服意图, 非分析
        if ("查询".equals(trigger) && (text.contains("状态") || text.contains("物流"))) {
            return true;
        }
        return false;
    }

    /**
     * 识别数据分析意图 (主入口).
     * <p>输出 top1 意图 + 置信度 + 实体集合.
     * <p>完整调用链: IntentPredictionService.predict() -> 本方法.
     *
     * @param text 已归一化文本
     * @return 识别结果: intent + confidence + 实体包
     */
    public static Result recognize(String text) {
        // 空入参兜底
        if (text == null || text.isBlank()) {
            return new Result(null, 0.0, List.of());
        }

        // 步骤 1: 累计每个 intent 的命中分
        Map<String, Double> scores = new LinkedHashMap<>();
        Map<String, List<String>> hits = new LinkedHashMap<>();
        for (var e : INTENT_TRIGGERS.entrySet()) {
            double score = 0.0;
            List<String> matched = new ArrayList<>();
            for (String trigger : e.getValue()) {
                if (text.contains(trigger)) {
                    // 长度加权: 长词 (≥3 字) 0.4, 短词 0.2
                    score += trigger.length() > 2 ? 0.4 : 0.2;
                    matched.add(trigger);
                }
            }
            if (score > 0) {
                scores.put(e.getKey(), score);
                hits.put(e.getKey(), matched);
            }
        }

        // 步骤 2: 无命中, 返回空结果
        if (scores.isEmpty()) {
            return new Result(null, 0.0, List.of());
        }

        // 步骤 3: 排序取 top1
        var sorted = scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .toList();
        String best = sorted.get(0).getKey();
        double bestScore = sorted.get(0).getValue();
        // 步骤 4: 归一化到 0-1
        double conf = Math.min(1.0, bestScore);

        // 步骤 5: 抽取 5 类数据实体
        Map<String, Set<String>> entities = extractEntities(text);
        return new Result(best, conf, List.of(
                new EntityBag(entities, hits.get(best))
        ));
    }

    /**
     * 抽取数据相关实体 (5 类 + 1 趋势).
     * <p>主入口, 也在 IntentPredictionService.predict() 直接调用作为备用.
     *
     * @param text 已归一化文本
     * @return 实体字典: 类别 -> 命中词集合
     */
    public static Map<String, Set<String>> extractEntities(String text) {
        Map<String, Set<String>> entities = new LinkedHashMap<>();
        entities.put("metric",      matchSet(text, METRIC_TRIGGERS));     // 指标
        entities.put("dimension",   matchSet(text, DIMENSION_TRIGGERS));  // 维度
        entities.put("timeRange",   matchSet(text, TIME_TRIGGERS));      // 时间范围
        entities.put("aggregation", matchSet(text, AGG_TRIGGERS));       // 聚合
        entities.put("dataSource",  matchSet(text, SOURCE_TRIGGERS));    // 数据源
        entities.put("trend",       matchSet(text, TREND_TRIGGERS));     // 趋势
        return entities;
    }

    /**
     * 内部方法: 匹配触发词集合.
     *
     * @param text     文本
     * @param triggers 触发词集合
     * @return 命中词集合 (LinkedHashSet 去重保序)
     */
    private static Set<String> matchSet(String text, Set<String> triggers) {
        Set<String> matched = new LinkedHashSet<>();
        for (String t : triggers) {
            if (text.contains(t)) matched.add(t);
        }
        return matched;
    }

    /**
     * 识别结果记录.
     *
     * @param intent     识别的意图 (null=未识别)
     * @param confidence 置信度 0-1
     * @param entities   实体包列表 (通常 1 个)
     */
    public record Result(String intent, double confidence, List<EntityBag> entities) {
        /**
         * 是否有效识别 (意图非空且置信度 &gt; 阈值).
         * <p>阈值 0.15: 触发 1 个 2 字词 = 0.2 > 0.15 → matched.
         */
        public boolean matched() { return intent != null && confidence > 0.15; }
    }

    /**
     * 实体包: 一组实体 + 触发的关键词列表.
     *
     * @param entities        5 类实体字典
     * @param matchedTriggers 触发的具体词 (调试用)
     */
    public record EntityBag(Map<String, Set<String>> entities, List<String> matchedTriggers) {}
}
