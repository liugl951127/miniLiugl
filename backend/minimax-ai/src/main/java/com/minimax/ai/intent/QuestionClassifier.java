package com.minimax.ai.intent;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 问题类型分类器 (V3.5.8 新增)
 *
 * <h2>5W1H + 数据查询类型</h2>
 * 经典 5W1H 问题分类 + 数据查询特有类型, 提升用户意图分析准确度
 *
 * <h2>10 种问题类型</h2>
 * <table border="1">
 *   <tr><th>类型</th><th>示例</th><th>中文触发</th></tr>
 *   <tr><td>WHAT</td><td>什么是 RAG?</td><td>什么是 / 啥 / 解释</td></tr>
 *   <tr><td>HOW</td><td>怎么登录?</td><td>怎么 / 如何 / 步骤</td></tr>
 *   <tr><td>WHY</td><td>为什么失败?</td><td>为什么 / 为何 / 原因</td></tr>
 *   <tr><td>WHEN</td><td>什么时候开始?</td><td>什么时候 / 何时 / 时间</td></tr>
 *   <tr><td>WHERE</td><td>在哪里配置?</td><td>在哪里 / 哪里 / 位置</td></tr>
 *   <tr><td>WHO</td><td>谁负责?</td><td>谁 / 哪个 / 负责人</td></tr>
 *   <tr><td>QUERY</td><td>查询订单状态</td><td>查询 / 列出 / 找出</td></tr>
 *   <tr><td>COMPARE</td><td>对比 A 和 B</td><td>对比 / 区别 / 哪个好</td></tr>
 *   <tr><td>ANALYZE</td><td>分析销售趋势</td><td>分析 / 统计 / 趋势</td></tr>
 *   <tr><td>PREDICT</td><td>预测下月销量</td><td>预测 / 预估 / 未来</td></tr>
 *   <tr><td>RECOMMEND</td><td>推荐方案</td><td>推荐 / 建议 / 哪个</td></tr>
 *   <tr><td>CREATE</td><td>创建图表</td><td>创建 / 画 / 制作 / 生成</td></tr>
 * </table>
 *
 * <h2>复杂度评估</h2>
 * <ul>
 *   <li>simple: 单实体 + 单条件 (e.g. "查询订单")</li>
 *   <li>medium: 多条件 + 时间范围 (e.g. "查询上月的销售")</li>
 *   <li>complex: 多表关联 + 聚合 + 预测 (e.g. "对比上季度各产品的销售趋势")</li>
 * </ul>
 *
 * @author MiniMax
 * @since V3.5.8
 */
public final class QuestionClassifier {

    private QuestionClassifier() {}

    public static final String TYPE_WHAT = "what";
    public static final String TYPE_HOW = "how";
    public static final String TYPE_WHY = "why";
    public static final String TYPE_WHEN = "when";
    public static final String TYPE_WHERE = "where";
    public static final String TYPE_WHO = "who";
    public static final String TYPE_QUERY = "query";
    public static final String TYPE_COMPARE = "compare";
    public static final String TYPE_ANALYZE = "analyze";
    public static final String TYPE_PREDICT = "predict";
    public static final String TYPE_RECOMMEND = "recommend";
    public static final String TYPE_CREATE = "create";

    public static final String COMPLEXITY_SIMPLE = "simple";
    public static final String COMPLEXITY_MEDIUM = "medium";
    public static final String COMPLEXITY_COMPLEX = "complex";

    /** 问题类型触发词 */
    private static final Map<String, List<String>> TYPE_TRIGGERS = new LinkedHashMap<>();
    static {
        TYPE_TRIGGERS.put(TYPE_WHAT, List.of("什么是", "啥是", "啥叫", "解释", "定义", "含义", "what", "which"));
        TYPE_TRIGGERS.put(TYPE_HOW, List.of("怎么", "如何", "怎样", "方法", "步骤", "教程", "how", "how to"));
        TYPE_TRIGGERS.put(TYPE_WHY, List.of("为什么", "为啥", "为何", "原因", "why", "reason"));
        TYPE_TRIGGERS.put(TYPE_WHEN, List.of("什么时候", "何时", "多久", "时间", "when", "what time"));
        TYPE_TRIGGERS.put(TYPE_WHERE, List.of("在哪里", "哪找", "位置", "路径", "where", "位置"));
        TYPE_TRIGGERS.put(TYPE_WHO, List.of("谁", "哪个", "负责人", "who", "whose"));
        TYPE_TRIGGERS.put(TYPE_QUERY, List.of("查询", "查", "看看", "列出", "显示", "找出", "告诉我", "select", "find", "list"));
        TYPE_TRIGGERS.put(TYPE_COMPARE, List.of("对比", "比较", "区别", "差异", "vs", "compare"));
        TYPE_TRIGGERS.put(TYPE_ANALYZE, List.of("分析", "统计", "趋势", "占比", "分布", "analyze", "statistic"));
        TYPE_TRIGGERS.put(TYPE_PREDICT, List.of("预测", "预估", "未来", "走势", "forecast", "predict"));
        TYPE_TRIGGERS.put(TYPE_RECOMMEND, List.of("推荐", "建议", "哪个好", "哪个", "推荐下", "recommend", "suggest"));
        TYPE_TRIGGERS.put(TYPE_CREATE, List.of("创建", "画", "制作", "生成", "新建", "create", "make", "build"));
    }

    /** 时间范围触发 */
    private static final Set<String> TIME_RANGE_TRIGGERS = Set.of(
            "今天", "昨天", "明天", "本周", "上周", "本月", "上月", "本季度", "上季度",
            "今年", "去年", "明年", "近7天", "近30天", "近一年", "近一月"
    );
    /** 聚合函数触发 */
    private static final Set<String> AGGREGATION_TRIGGERS = Set.of(
            "总数", "平均", "最大", "最小", "求和", "总和", "汇总",
            "sum", "avg", "count", "max", "min", "group by"
    );
    /** 维度触发 */
    private static final Set<String> DIMENSION_TRIGGERS = Set.of(
            "按", "每个", "各个", "分", "group", "by"
    );

    /**
     * 分类问题类型
     *
     * @param text 已归一化文本
     * @return (type, confidence) 列表, 按 confidence 降序
     */
    public static List<Match> classify(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<Match> matches = new ArrayList<>();
        for (var e : TYPE_TRIGGERS.entrySet()) {
            int hits = 0;
            int totalScore = 0;
            for (String trigger : e.getValue()) {
                if (text.contains(trigger)) {
                    hits++;
                    // 长度越长权重越高
                    totalScore += trigger.length() * 10;
                }
            }
            if (hits > 0) {
                // 归一化到 0-1
                double conf = Math.min(1.0, totalScore / 30.0);
                matches.add(new Match(e.getKey(), conf, hits));
            }
        }
        // 按 conf 降序
        matches.sort((a, b) -> Double.compare(b.confidence, a.confidence));
        return matches;
    }

    /**
     * 评估查询复杂度
     */
    public static Complexity assess(String text) {
        if (text == null) return new Complexity(COMPLEXITY_SIMPLE, 0, 0, 0);
        int timeRangeCount = 0;
        for (String t : TIME_RANGE_TRIGGERS) {
            if (text.contains(t)) timeRangeCount++;
        }
        int aggCount = 0;
        for (String t : AGGREGATION_TRIGGERS) {
            if (text.contains(t)) aggCount++;
        }
        int dimCount = 0;
        for (String t : DIMENSION_TRIGGERS) {
            if (text.contains(t)) dimCount++;
        }
        // 总条件数
        int total = timeRangeCount + aggCount + dimCount;
        // 关联指标 (>2 个数字/百分比/金额)
        Pattern p = Pattern.compile("(\\d+%|\\d+\\.\\d+|\\d{2,})");
        int numbers = p.matcher(text).results().limit(5).toList().size();
        total += Math.min(numbers, 3);

        String level;
        if (total <= 1) level = COMPLEXITY_SIMPLE;
        else if (total <= 3) level = COMPLEXITY_MEDIUM;
        else level = COMPLEXITY_COMPLEX;
        return new Complexity(level, timeRangeCount, aggCount, dimCount);
    }

    /** 类型匹配结果 */
    public record Match(String type, double confidence, int hits) {}

    /** 复杂度评估结果 */
    public record Complexity(String level, int timeRanges, int aggregations, int dimensions) {
        /** 推荐的图表类型 */
        public String suggestChartType() {
            if (timeRanges > 0 && aggregations > 0) return "line";   // 折线图: 时间趋势
            if (dimensions > 1 && aggregations > 0) return "bar";    // 柱状图: 分类对比
            if (aggregations > 0) return "pie";                       // 饼图: 占比
            if (timeRanges > 0) return "area";                       // 面积图
            return "table";                                           // 表格
        }
        /** 推荐的处理方式 */
        public String suggestProcess() {
            return switch (level) {
                case "simple"   -> "DIRECT_QUERY";   // 直接 SQL 查询
                case "medium"   -> "FILTER_AGG";     // 过滤 + 聚合
                case "complex"  -> "MULTI_JOIN";     // 多表关联 + 复杂聚合
                default -> "DIRECT_QUERY";
            };
        }
    }
}
