package com.minimax.ai.nlp;

// @Slf4j: Lombok 注解,自动生成 log 字段
import lombok.extern.slf4j.Slf4j;
// @Component: Spring 自动注入注解
import org.springframework.stereotype.Component;

// Arrays: 数组工具,转 List/Set 用
import java.util.Arrays;
// HashMap: 普通 Map
import java.util.HashMap;
// HashSet: 普通 Set
import java.util.HashSet;
// List: 列表
import java.util.List;
// Map: 键值对
import java.util.Map;
// Set: 集合
import java.util.Set;
// Pattern: 正则编译 (标点匹配)
import java.util.regex.Pattern;
import java.util.Collections;
import java.util.ArrayList;

/**
 * V6.0 情感分析器 (SentimentAnalyzer)
 *
 * <h2>核心算法: 词典法 + 规则 + 否定 + 程度</h2>
 *
 * <h3>1. 词典法 (Lexicon-based)</h3>
 * 维护正面/负面词词典,统计文本中正负词出现次数,加权求和。
 * 优点: 简单、可解释、零样本 (no training needed)
 * 缺点: 召回有限,无法处理新词
 *
 * <h3>2. 否定词处理 (Negation Handling)</h3>
 * 中文否定词 "不/没/别/非/无/未" 出现在情感词前,反转极性。
 * 例: "好" (+1) vs "不好" (-1) vs "很不好" (-2)
 *
 * <h3>3. 程度副词 (Degree Words)</h3>
 * "很/非常/特别/极" 等强化情感,"稍/略" 弱化。
 * 权重: 很 1.5x / 非常 2.0x / 极 2.5x / 稍 0.5x
 *
 * <h3>4. 表情符号 (Emoji)</h3>
 * 😊👍❤ → +1, 😢😡💔 → -1
 *
 * <h2>输出</h2>
 * - score: -1.0 (最负面) ~ +1.0 (最正面)
 * - label: POSITIVE / NEUTRAL / NEGATIVE
 *
 * <h2>应用场景</h2>
 * - 客服: 自动识别投诉/满意
 * - 评论: 情感倾向分析
 * - 聊天机器人: 调整回复语气
 * - 监控: 用户反馈情感趋势
 */
@Slf4j
/**
 * SentimentAnalyzer (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * NLP 处理 - SentimentAnalyzer.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 SentimentAnalyzer 的业务能力</li>
 *   <li>参与 AI 平台整体架构</li>
 *   <li>支持 Spring 依赖注入</li>
 * </ul>
 *
 * <h2>依赖</h2>
 * <ul>
 *   <li>Spring Framework (自动注入)</li>
 *   <li>Lombok (简化代码)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V6.1
 */
@Component
public class SentimentAnalyzer {

    // ============== 词典 ==============
    /**
     * 正面词词典 (60+ 词)
     * 选取标准: 高频常用、情感强度高、口语化
     */
    private static final Set<String> POSITIVE = new HashSet<>(Arrays.asList(
            "好", "棒", "喜欢", "爱", "开心", "快乐", "高兴", "满意", "感谢", "谢谢",
            "不错", "完美", "优秀", "惊喜", "推荐", "赞", "支持", "成功", "顺利", "强大",
            "漂亮", "美", "帅", "可爱", "好用", "实用", "方便", "快速", "稳定", "流畅",
            "清晰", "专业", "友好", "贴心", "温暖", "舒服", "美好", "精彩", "有趣", "酷",
            "希望", "期待", "信任", "相信", "美好", "幸福", "愉悦", "轻松", "舒服", "良好"
    ));

    /**
     * 负面词词典 (60+ 词)
     * 覆盖: 失望/愤怒/焦虑/恐惧/痛苦
     */
    private static final Set<String> NEGATIVE = new HashSet<>(Arrays.asList(
            "差", "糟", "讨厌", "恨", "生气", "难过", "悲伤", "失败", "错", "坏",
            "烂", "丑", "麻烦", "困难", "烦", "慢", "卡", "崩溃", "挂", "死",
            "bug", "问题", "故障", "错误", "难受", "痛苦", "失望", "绝望", "焦虑", "紧张",
            "压力", "担心", "害怕", "恐惧", "担心", "糟糕", "不行", "没用", "无语", "失望",
            "骗", "坑", "黑", "退", "投诉", "骂", "批评", "质疑", "困惑", "迷茫"
    ));

    /**
     * 否定词词典
     * 出现在情感词前,反转极性 (+1 → -1, -1 → +1)
     */
    private static final Set<String> NEGATIONS = new HashSet<>(Arrays.asList(
            "不", "没", "别", "非", "无", "未", "勿", "莫", "没有", "不是", "不会", "不可"
    ));

    /**
     * 程度副词典 + 权重
     * 强化 (>1.0) 或弱化 (<1.0) 后续情感词
     * 范围: 0.5 (稍) ~ 2.5 (极其)
     */
    private static final Map<String, Double> DEGREE_WORDS = new HashMap<>();
    static {
        // 强化 (1.0 ~ 2.5)
        DEGREE_WORDS.put("很", 1.5);
        DEGREE_WORDS.put("非常", 2.0);
        DEGREE_WORDS.put("特别", 1.8);
        DEGREE_WORDS.put("极", 2.0);
        DEGREE_WORDS.put("极其", 2.5);
        DEGREE_WORDS.put("超", 1.5);
        DEGREE_WORDS.put("超级", 2.0);
        DEGREE_WORDS.put("最", 2.0);
        // 中等 (1.0 ~ 1.5)
        DEGREE_WORDS.put("比较", 1.2);
        DEGREE_WORDS.put("较", 1.2);
        // 弱化 (0.5 ~ 0.7)
        DEGREE_WORDS.put("稍", 0.5);
        DEGREE_WORDS.put("稍微", 0.5);
        DEGREE_WORDS.put("略", 0.5);
        DEGREE_WORDS.put("一点", 0.7);
    }

    // ============== 标点正则 ==============
    /**
     * 标点/符号匹配: [\\p{Punct}\\p{S}]+
     * \\p{Punct} = !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~
     * \\p{S} = 数学符号/货币符号等
     */
    private static final Pattern PUNCT = Pattern.compile("[\\p{Punct}\\p{S}]+");

    // ============== 核心 API ==============
    /**
     * 分析文本情感
     *
     * <h2>算法</h2>
     * <ol>
     *   <li>清洗: 标点 → 空格</li>
     *   <li>分字: 中文字符按字拆,英文/数字保留整词</li>
     *   <li>遍历: 维护 prevNeg(上词是否否定) + prevDegree(上词程度)</li>
     *   <li>情感词: score += (prevNeg?-1:1) * prevDegree * 1</li>
     *   <li>否定词: prevNeg = !prevNeg</li>
     *   <li>程度词: prevDegree = weight</li>
     *   <li>归一化: score / max(3, hits) ∈ [-1, 1]</li>
     * </ol>
     *
     * @param text 输入文本
     * @return SentimentResult (score, label, hits)
     */
    public SentimentResult analyze(String text) {
        // 空文本: 中性
        if (text == null || text.isBlank()) {
            return new SentimentResult(0.0, SentimentLabel.NEUTRAL, Collections.emptyList());
        }

        // 1. 清洗: 标点替换为空格 (避免影响分词)
        String cleaned = PUNCT.matcher(text).replaceAll(" ");

        // 2. 分字: 中文字符单独成 token, 英文/数字保留整词
        // 例: "这个产品很好用" → ["这", "个", "产", "品", "很", "好", "用"]
        List<String> tokensList = new java.util.ArrayList<>();
        StringBuilder word = new StringBuilder();
        for (char c : cleaned.toCharArray()) {
            if (Character.isWhitespace(c)) {
                // 空白: 结束当前英文/数字词
                if (word.length() > 0) { tokensList.add(word.toString()); word.setLength(0); }
            } else if (c >= 0x4E00 && c <= 0x9FFF) {
                // 中文字符 (CJK 基本平面): 单字成 token
                if (word.length() > 0) { tokensList.add(word.toString()); word.setLength(0); }
                tokensList.add(String.valueOf(c));
            } else {
                // 英文字母/数字: 累积
                word.append(c);
            }
        }
        if (word.length() > 0) tokensList.add(word.toString());
        String[] tokens = tokensList.toArray(new String[0]);

        // 3. 遍历 token, 累加 score
        double score = 0.0;
        List<String> hits = new ArrayList<>();
        // 状态变量: 上一词是否否定 (反转下次情感)
        boolean prevNeg = false;
        // 状态变量: 上一词程度 (下次情感用)
        Double prevDegree = null;

        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            if (t.isEmpty()) continue;

            // 否定词: 翻转标志 (持续到下一个情感词)
            if (NEGATIONS.contains(t)) {
                prevNeg = !prevNeg;
                continue;
            }

            // 程度副词: 记录权重 (持续到下一个情感词)
            Double deg = DEGREE_WORDS.get(t);
            if (deg != null) {
                prevDegree = deg;
                continue;
            }

            // 当前情感词的权重 (默认 1.0, 有 prevDegree 则用)
            double w = prevDegree != null ? prevDegree : 1.0;
            prevDegree = null;  // 重置 (只影响最近一个情感词)

            // 正面词: 累加 ±w
            if (POSITIVE.contains(t)) {
                // 否定: 反转极性
                score += prevNeg ? -w : w;
                hits.add((prevNeg ? "−" : "+") + t);
            } else if (NEGATIVE.contains(t)) {
                // 负面词: 累加 (默认负, 否定则正)
                score += prevNeg ? w : -w;
                hits.add((prevNeg ? "−−" : "−") + t);
            }

            // 表情符号处理
            if (t.contains("😊") || t.contains("😄") || t.contains("👍") || t.contains("❤") || t.contains("🎉")) {
                score += 1.0;
                hits.add("😊");
            }
            if (t.contains("😢") || t.contains("😭") || t.contains("😡") || t.contains("😞") || t.contains("💔")) {
                score -= 1.0;
                hits.add("😢");
            }
        }

        // 4. 归一化到 [-1, 1]
        //    分母: max(3, hits.size()) 防止 hits 极少时分数爆炸
        double normalized = Math.max(-1.0, Math.min(1.0, score / Math.max(3.0, hits.size())));

        // 5. 判定 label
        SentimentLabel label;
        if (normalized > 0.2) label = SentimentLabel.POSITIVE;       // 显著正面
        else if (normalized < -0.2) label = SentimentLabel.NEGATIVE; // 显著负面
        else label = SentimentLabel.NEUTRAL;                          // 中性

        return new SentimentResult(normalized, label, hits);
    }

    // ============== 数据类 ==============
    /**
     * 情感标签枚举
     */
    public enum SentimentLabel { POSITIVE, NEUTRAL, NEGATIVE }

    /**
     * 情感分析结果
     */
    public static class SentimentResult {
        // 归一化分数 [-1, 1]
        public final double score;
        // 标签
        public final SentimentLabel label;
        // 命中的情感词列表 (用于 debug)
        public final List<String> hits;
        public SentimentResult(double s, SentimentLabel l, List<String> h) {
            this.score = s;
            this.label = l;
            this.hits = h;
        }
        @Override
        public String toString() {
            return String.format("Sentiment[%.2f, %s, hits=%d]", score, label, hits.size());
        }
    }
}
