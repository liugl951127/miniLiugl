package com.minimax.ai.nlp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * V6.0 情感分析器 (SentimentAnalyzer)
 *
 * 基于词典 + 规则 + 否定词:
 *   - 正面词典: 好/棒/喜欢/开心/满意/感谢/...
 *   - 负面词典: 差/糟/讨厌/生气/难过/失败/...
 *   - 否定词: 不/没/别/非/无/未
 *   - 程度副词: 很/非常/特别/极/稍/略 (× 2.0 / 1.5 / 1.3 / 0.5)
 *
 * 输出: -1.0 (最负面) ~ +1.0 (最正面)
 *
 * 应用:
 *   - 客服: 自动识别投诉/满意
 *   - 评论: 情感倾向
 *   - 聊天: 调整回复语气
 */
@Slf4j
@Component
public class SentimentAnalyzer {

    private static final Set<String> POSITIVE = new HashSet<>(Arrays.asList(
            "好", "棒", "喜欢", "爱", "开心", "快乐", "高兴", "满意", "感谢", "谢谢",
            "不错", "完美", "优秀", "惊喜", "推荐", "赞", "支持", "成功", "顺利", "强大",
            "漂亮", "美", "帅", "可爱", "好用", "实用", "方便", "快速", "稳定", "流畅",
            "清晰", "专业", "友好", "贴心", "温暖", "舒服", "美好", "精彩", "有趣", "酷",
            "希望", "期待", "信任", "相信", "美好", "幸福", "愉悦", "轻松", "舒服", "良好"
    ));

    private static final Set<String> NEGATIVE = new HashSet<>(Arrays.asList(
            "差", "糟", "讨厌", "恨", "生气", "难过", "悲伤", "失败", "错", "坏",
            "烂", "丑", "麻烦", "困难", "烦", "慢", "卡", "崩溃", "挂", "死",
            "bug", "问题", "故障", "错误", "难受", "痛苦", "失望", "绝望", "焦虑", "紧张",
            "压力", "担心", "害怕", "恐惧", "担心", "糟糕", "不行", "没用", "无语", "失望",
            "骗", "坑", "黑", "退", "投诉", "骂", "批评", "质疑", "困惑", "迷茫"
    ));

    private static final Set<String> NEGATIONS = new HashSet<>(Arrays.asList(
            "不", "没", "别", "非", "无", "未", "勿", "莫", "没有", "不是", "不会", "不可"
    ));

    private static final Map<String, Double> DEGREE_WORDS = new HashMap<>();
    static {
        // 程度副词权重
        DEGREE_WORDS.put("很", 1.5);
        DEGREE_WORDS.put("非常", 2.0);
        DEGREE_WORDS.put("特别", 1.8);
        DEGREE_WORDS.put("极", 2.0);
        DEGREE_WORDS.put("极其", 2.5);
        DEGREE_WORDS.put("超", 1.5);
        DEGREE_WORDS.put("超级", 2.0);
        DEGREE_WORDS.put("最", 2.0);
        DEGREE_WORDS.put("比较", 1.2);
        DEGREE_WORDS.put("较", 1.2);
        DEGREE_WORDS.put("稍", 0.5);
        DEGREE_WORDS.put("稍微", 0.5);
        DEGREE_WORDS.put("略", 0.5);
        DEGREE_WORDS.put("一点", 0.7);
    }

    private static final Pattern PUNCT = Pattern.compile("[\\p{Punct}\\p{S}]+");

    public SentimentResult analyze(String text) {
        if (text == null || text.isBlank()) {
            return new SentimentResult(0.0, SentimentLabel.NEUTRAL, Collections.emptyList());
        }

        String cleaned = PUNCT.matcher(text).replaceAll(" ");
        // 中文字符按字拆, 英文/数字保留整词
        List<String> tokensList = new java.util.ArrayList<>();
        StringBuilder word = new StringBuilder();
        for (char c : cleaned.toCharArray()) {
            if (Character.isWhitespace(c)) {
                if (word.length() > 0) { tokensList.add(word.toString()); word.setLength(0); }
            } else if (c >= 0x4E00 && c <= 0x9FFF) {
                // 中文字符, 单字一个 token
                if (word.length() > 0) { tokensList.add(word.toString()); word.setLength(0); }
                tokensList.add(String.valueOf(c));
            } else {
                word.append(c);
            }
        }
        if (word.length() > 0) tokensList.add(word.toString());
        String[] tokens = tokensList.toArray(new String[0]);

        double score = 0.0;
        List<String> hits = new ArrayList<>();
        boolean prevNeg = false;
        Double prevDegree = null;

        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            if (t.isEmpty()) continue;

            if (NEGATIONS.contains(t)) {
                prevNeg = !prevNeg;
                continue;
            }

            Double deg = DEGREE_WORDS.get(t);
            if (deg != null) {
                prevDegree = deg;
                continue;
            }

            double w = prevDegree != null ? prevDegree : 1.0;
            prevDegree = null;

            if (POSITIVE.contains(t)) {
                score += prevNeg ? -w : w;
                hits.add((prevNeg ? "−" : "+") + t);
            } else if (NEGATIVE.contains(t)) {
                score += prevNeg ? w : -w;
                hits.add((prevNeg ? "−−" : "−") + t);
            }
            // 表情符号
            if (t.contains("😊") || t.contains("😄") || t.contains("👍") || t.contains("❤") || t.contains("🎉")) {
                score += 1.0;
                hits.add("😊");
            }
            if (t.contains("😢") || t.contains("😭") || t.contains("😡") || t.contains("😞") || t.contains("💔")) {
                score -= 1.0;
                hits.add("😢");
            }
        }

        // 归一化到 [-1, 1]
        double normalized = Math.max(-1.0, Math.min(1.0, score / Math.max(3.0, hits.size())));

        SentimentLabel label;
        if (normalized > 0.2) label = SentimentLabel.POSITIVE;
        else if (normalized < -0.2) label = SentimentLabel.NEGATIVE;
        else label = SentimentLabel.NEUTRAL;

        return new SentimentResult(normalized, label, hits);
    }

    public enum SentimentLabel { POSITIVE, NEUTRAL, NEGATIVE }

    public static class SentimentResult {
        public final double score;     // -1.0 ~ +1.0
        public final SentimentLabel label;
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
