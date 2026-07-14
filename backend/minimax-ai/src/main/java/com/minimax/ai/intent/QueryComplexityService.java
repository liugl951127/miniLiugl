package com.minimax.ai.intent;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 查询复杂度分析器 (Day 29 - 智能化提升).
 *
 * <h3>分析维度</h3>
 * <ol>
 *   <li><b>文本长度</b>: token 估算（中文≈1，英文≈0.25）</li>
 *   <li><b>技术术语密度</b>: SQL/代码/关键词越多越复杂</li>
 *   <li><b>结构复杂度</b>: 数字量、列表、多步骤指示</li>
 *   <li><b>意图类型</b>: CHAT 最简 → ANALYZE_DATA/GENERATE_CODE 最复杂</li>
 *   <li><b>多轮上下文</b>: 引用 "它"/"这个"/"再" 增加复杂度</li>
 * </ol>
 *
 * <h3>评分公式</h3>
 * <pre>
 * score = 0.25*lenScore + 0.30*techScore + 0.20*structScore
 *        + 0.15*intentScore + 0.10*contextScore
 * </pre>
 *
 * <h3>等级划分</h3>
 * <pre>
 * score &lt; 0.30  → COMPLEX  (复杂: 代码生成/多步分析/图表生成)
 * score &lt; 0.60  → MEDIUM    (中等: 数据查询/统计分析)
 * score &lt;= 1.0 → SIMPLE    (简单: 闲聊/快速问答)
 * </pre>
 */
@Slf4j
@Component
public class QueryComplexityService {

    /** 复杂技术术语 */
    private static final Set<String> TECH_TERMS = Set.of(
            "select", "from", "where", "join", "group by", "order by", "having",
            "aggregate", "subquery", "transaction", "index", "partition",
            "spring", "boot", "vue", "react", "flask", "express", "component",
            "api", "rest", "graphql", "websocket", "microservice", "kubernetes",
            "docker", "ci/cd", "pipeline", "deployment", "nginx", "redis",
            "transformer", "attention", "neural", "embedding", "token",
            "html", "css", "javascript", "typescript", "python", "java", "rust",
            "sql", "dml", "ddl", "jdbc", "orm", "mybatis", "hibernate",
            "统计", "聚合", "分组", "排序", "索引", "事务", "并发",
            "神经网络", "注意力机制", "嵌入", "分词", "向量化"
    );

    /** 简单闲聊词汇 */
    private static final Set<String> CASUAL_TERMS = Set.of(
            "你好", "您好", "hi", "hello", "嗨", "hi", "who", "what",
            "你是谁", "叫什么", "介绍一下", "介绍一下", "能做什么",
            "为什么", "怎么", "如何", "请问", "帮我", "帮我", "how",
            "今天", "天气", "怎么样", "好不好", "有趣", "好玩"
    );

    /** 复杂度指示词 */
    private static final Set<String> COMPLEXITY_UP = Set.of(
            "生成", "创建", "实现", "设计", "架构", "完整", "详细",
            "一步步", "逐步", "整个", "从零", "0到1", "全栈", "端到端",
            "生成", "create", "implement", "design", "architect"
    );

    /** 后续指代词 */
    private static final Set<String> FOLLOWUP_TERMS = Set.of(
            "再", "又", "还", "也", "它", "这个", "那个", "上面", "刚才",
            "改了", "换个", "调整", "改成", "重新", "again", "also"
    );

    private static final Pattern CODE_PATTERN = Pattern.compile(
            "[\\{\\}\\(\\);,.\\w]{10,}|" + // 代码符号
            "\\bfunction\\b|\\bdef\\b|\\bclass\\b|\\bimport\\b|\\bexport\\b|\\bpublic\\b|\\bprivate\\b|\\breturn\\b|" + // 代码关键字
            "SELECT|INSERT|UPDATE|DELETE|GROUP|ORDER|FROM|WHERE" // SQL关键字
    );

    /**
     * 分析查询复杂度。
     *
     * @param text        用户输入
     * @param intentHint  意图提示（可为 null）
     * @return 复杂度分析结果
     */
    public ComplexityResult analyze(String text, String intentHint) {
        if (text == null || text.isBlank()) {
            return new ComplexityResult(ComplexityLevel.SIMPLE, 0.0, "空输入");
        }

        String lower = text.toLowerCase();
        int len = text.length();

        // 1. 长度得分 (0-1)
        double lenScore = Math.min(len / 500.0, 1.0);

        // 2. 技术术语密度 (0-1)
        int techHits = 0;
        for (String term : TECH_TERMS) {
            if (lower.contains(term)) techHits++;
        }
        double techScore = Math.min(techHits / 8.0, 1.0);

        // 3. 结构复杂度 (数字量、多步骤指示) (0-1)
        int numCount = (int) text.chars().filter(Character::isDigit).count();
        int stepWords = countWords(lower, List.of("首先", "第一步", "然后", "接着", "最后", "总结",
                "first", "then", "next", "finally", "lastly", "step"));
        double structScore = Math.min((numCount / 30.0 + stepWords / 5.0), 1.0);

        // 4. 意图类型复杂度 (0-1)
        double intentScore = intentComplexityScore(intentHint, lower);

        // 5. 上下文复杂度 (多轮指代) (0-1)
        int followupHits = countWords(lower, FOLLOWUP_TERMS.stream().toList());
        double contextScore = Math.min(followupHits / 2.0, 1.0);

        // 综合评分
        double score = 0.25 * lenScore + 0.30 * techScore
                + 0.20 * structScore + 0.15 * intentScore + 0.10 * contextScore;

        // 代码片段检测直接跳复杂
        if (CODE_PATTERN.matcher(text).find()) {
            score = Math.max(score, 0.70);
        }

        ComplexityLevel level = score < 0.30 ? ComplexityLevel.SIMPLE
                : score < 0.60 ? ComplexityLevel.MEDIUM
                : ComplexityLevel.COMPLEX;

        String reason = String.format(
                "len=%.0f tech=%.2f struct=%.2f intent=%.2f context=%.2f",
                lenScore, techScore, structScore, intentScore, contextScore);

        log.debug("[Complexity] text='{}' → score={:.2f} level={} reason={}",
                text.length() > 50 ? text.substring(0, 50) + "..." : text,
                score, level, reason);

        return new ComplexityResult(level, score, reason);
    }

    /** 根据意图名称估算复杂度分 */
    private double intentComplexityScore(String intent, String lower) {
        if (intent == null) return 0.3;
        return switch (intent.toUpperCase()) {
            case "CHAT", "TTS", "STT" -> 0.05;
            case "QUERY_DATA", "IMAGE_ANALYZE", "AUDIO_ANALYZE" -> 0.30;
            case "ANALYZE_DATA", "GENERATE_CHART", "VIDEO_ANALYZE" -> 0.55;
            case "GENERATE_CODE", "GENERATE_MUSIC", "GENERATE_ANIMATION" -> 0.80;
            default -> 0.30;
        };
    }

    private int countWords(String text, List<String> words) {
        int count = 0;
        for (String w : words) {
            if (text.contains(w)) count++;
        }
        return count;
    }

    // ============== 结果 ==============

    @Getter
    public static class ComplexityResult {
        /** 复杂度等级 */
        private final ComplexityLevel level;
        /** 综合评分 [0, 1] */
        private final double score;
        /** 分析原因 */
        private final String reason;

        public ComplexityResult(ComplexityLevel level, double score, String reason) {
            this.level = level;
            this.score = Math.round(score * 100.0) / 100.0;
            this.reason = reason;
        }
    }

    public enum ComplexityLevel {
        /** 简单: 闲聊、快速问答 */
        SIMPLE,
        /** 中等: 数据查询、统计分析 */
        MEDIUM,
        /** 复杂: 代码生成、多步分析、图表生成 */
        COMPLEX
    }
}
