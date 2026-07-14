package com.minimax.ai.intent;

import java.util.*;

/**
 * 数据分析意图识别引擎 (V3.5.8 新增)
 *
 * <h2>核心能力</h2>
 * 专门识别"数据分析"场景下的用户意图, 比通用 Intent 分类更精细
 *
 * <h2>6 种数据分析意图</h2>
 * <table border="1">
 *   <tr><th>意图</th><th>说明</th><th>触发词</th></tr>
 *   <tr><td>data_query</td><td>查数据 (单条件)</td><td>查询 / 列出 / 看看 / 多少 / 有几个</td></tr>
 *   <tr><td>data_analyze</td><td>分析 (多维度)</td><td>分析 / 统计 / 占比 / 趋势 / 分布 / 排名</td></tr>
 *   <tr><td>data_compare</td><td>对比 (2+ 实体)</td><td>对比 / 比较 / vs / 区别 / 哪个好 / 哪个多</td></tr>
 *   <tr><td>data_predict</td><td>预测 (未来)</td><td>预测 / 预估 / 未来 / 走势 / 明年 / 下月</td></tr>
 *   <tr><td>data_visualize</td><td>可视化 (图表)</td><td>画 / 图表 / 折线 / 柱状 / 饼图 / 可视化</td></tr>
 *   <tr><td>data_report</td><td>报告 (汇总)</td><td>报告 / 报表 / 汇总 / 周报 / 月报 / 导出</td></tr>
 * </table>
 *
 * <h2>实体抽取</h2>
 * 额外识别数据相关实体:
 * <ul>
 *   <li>metric: 指标 (销售额, 订单数, 用户数...)</li>
 *   <li>dimension: 维度 (地区, 产品, 时间, 渠道...)</li>
 *   <li>timeRange: 时间范围 (今天, 上月, 近7天, 2024年...)</li>
 *   <li>aggregation: 聚合 (总和, 平均, 最大, 最小, 计数...)</li>
 *   <li>dataSource: 数据源 (订单表, 用户表, 销售记录...)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V3.5.8
 */
public final class DataAnalysisEngine {

    private DataAnalysisEngine() {}

    // ============== 6 种数据分析意图 ==============

    public static final String INTENT_DATA_QUERY = "data_query";
    public static final String INTENT_DATA_ANALYZE = "data_analyze";
    public static final String INTENT_DATA_COMPARE = "data_compare";
    public static final String INTENT_DATA_PREDICT = "data_predict";
    public static final String INTENT_DATA_VISUALIZE = "data_visualize";
    public static final String INTENT_DATA_REPORT = "data_report";

    // 触发词 (按权重排序)
    private static final Map<String, List<String>> INTENT_TRIGGERS = new LinkedHashMap<>();
    static {
        INTENT_TRIGGERS.put(INTENT_DATA_QUERY, List.of(
                "查询", "查", "看看", "列出", "显示", "告诉我", "多少", "有几个", "select", "list", "show"
        ));
        INTENT_TRIGGERS.put(INTENT_DATA_ANALYZE, List.of(
                "分析", "统计", "占比", "分布", "排名", "排序", "analyze", "statistic", "rank"
        ));
        INTENT_TRIGGERS.put(INTENT_DATA_COMPARE, List.of(
                "对比", "比较", "区别", "vs", "哪个好", "哪个多", "差异", "compare", "versus"
        ));
        INTENT_TRIGGERS.put(INTENT_DATA_PREDICT, List.of(
                "预测", "预估", "未来", "走势", "预测", "明年", "下月", "下个季度", "forecast", "predict"
        ));
        INTENT_TRIGGERS.put(INTENT_DATA_VISUALIZE, List.of(
                "画", "图表", "折线", "柱状", "饼图", "可视化", "画图", "chart", "graph", "plot", "visualize"
        ));
        INTENT_TRIGGERS.put(INTENT_DATA_REPORT, List.of(
                "报告", "报表", "汇总", "周报", "月报", "季报", "年报", "导出", "export", "report"
        ));
    }

    /** 趋势词 */
    private static final Set<String> TREND_TRIGGERS = Set.of(
            "趋势", "走势", "变化", "增长", "下降", "涨幅", "跌幅", "同比", "环比"
    );
    /** 指标词 */
    private static final Set<String> METRIC_TRIGGERS = Set.of(
            "销售额", "营收", "收入", "利润", "成本", "毛利",
            "订单数", "订单量", "访问量", "点击量", "pv", "uv",
            "用户数", "客户数", "会员数", "注册数", "活跃用户",
            "转化率", "留存率", "复购率", "退款率",
            "库存", "销量", "产量", "报废率"
    );
    /** 维度词 */
    private static final Set<String> DIMENSION_TRIGGERS = Set.of(
            "地区", "城市", "省份", "国家", "区域",
            "产品", "品类", "商品", "服务",
            "渠道", "来源", "平台", "店铺",
            "用户", "客户", "会员", "员工",
            "部门", "团队", "小组"
    );
    /** 时间词 (扩展 QuestionClassifier) */
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
    /** 聚合词 */
    private static final Set<String> AGG_TRIGGERS = Set.of(
            "总数", "总和", "求和", "合计", "累计",
            "平均", "均值", "中位数",
            "最大", "最小", "最高", "最低",
            "Top", "前10", "前5", "前3",
            "sum", "avg", "count", "max", "min"
    );
    /** 数据源关键词 */
    private static final Set<String> SOURCE_TRIGGERS = Set.of(
            "订单", "用户", "客户", "产品", "商品", "库存",
            "支付", "退款", "物流",
            "订单表", "用户表", "产品表",
            "订单记录", "销售记录", "访问日志"
    );

    /**
     * 是否属于数据分析场景
     */
    public static boolean isDataAnalysis(String text) {
        if (text == null || text.isBlank()) return false;
        // 含数据指标
        for (String m : METRIC_TRIGGERS) {
            if (text.contains(m)) return true;
        }
        // 含分析触发词
        for (var e : INTENT_TRIGGERS.entrySet()) {
            for (String trigger : e.getValue()) {
                if (text.contains(trigger)) {
                    // 排除明显的非数据分析场景
                    if (isNonAnalysis(text, trigger)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    /** 排除明显非数据分析 */
    private static boolean isNonAnalysis(String text, String trigger) {
        // "查询订单状态" -> 可能是订单客服, 非分析
        if ("查询".equals(trigger) && (text.contains("状态") || text.contains("物流"))) {
            return true;
        }
        return false;
    }

    /**
     * 识别数据分析意图
     *
     * @param text 已归一化文本
     * @return 意图 + 置信度, 命中最高分
     */
    public static Result recognize(String text) {
        if (text == null || text.isBlank()) {
            return new Result(null, 0.0, List.of());
        }

        Map<String, Double> scores = new LinkedHashMap<>();
        Map<String, List<String>> hits = new LinkedHashMap<>();

        for (var e : INTENT_TRIGGERS.entrySet()) {
            double score = 0.0;
            List<String> matched = new ArrayList<>();
            for (String trigger : e.getValue()) {
                if (text.contains(trigger)) {
                    // 长词权重更高 (短语 2x, 单词 1x)
                    score += trigger.length() > 2 ? 0.4 : 0.2;
                    matched.add(trigger);
                }
            }
            if (score > 0) {
                scores.put(e.getKey(), score);
                hits.put(e.getKey(), matched);
            }
        }

        if (scores.isEmpty()) {
            return new Result(null, 0.0, List.of());
        }

        // 排序
        var sorted = scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .toList();

        String best = sorted.get(0).getKey();
        double bestScore = sorted.get(0).getValue();
        // 归一化到 0-1
        double conf = Math.min(1.0, bestScore);

        // 组合实体
        Map<String, Set<String>> entities = extractEntities(text);
        return new Result(best, conf, List.of(
                new EntityBag(entities, hits.get(best))
        ));
    }

    /**
     * 抽取数据相关实体
     */
    public static Map<String, Set<String>> extractEntities(String text) {
        Map<String, Set<String>> entities = new LinkedHashMap<>();
        entities.put("metric", matchSet(text, METRIC_TRIGGERS));
        entities.put("dimension", matchSet(text, DIMENSION_TRIGGERS));
        entities.put("timeRange", matchSet(text, TIME_TRIGGERS));
        entities.put("aggregation", matchSet(text, AGG_TRIGGERS));
        entities.put("dataSource", matchSet(text, SOURCE_TRIGGERS));
        entities.put("trend", matchSet(text, TREND_TRIGGERS));
        return entities;
    }

    private static Set<String> matchSet(String text, Set<String> triggers) {
        Set<String> matched = new LinkedHashSet<>();
        for (String t : triggers) {
            if (text.contains(t)) matched.add(t);
        }
        return matched;
    }

    /**
     * 识别结果
     */
    public record Result(String intent, double confidence, List<EntityBag> entities) {
        public boolean matched() { return intent != null && confidence > 0.15; }
    }

    /**
     * 实体包
     */
    public record EntityBag(Map<String, Set<String>> entities, List<String> matchedTriggers) {}
}
