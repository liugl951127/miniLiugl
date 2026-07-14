package com.minimax.ai.intent;

import java.util.*;

/**
 * Negation Handling / 否定处理 (V3.5.6 算法升级新增)
 *
 * <h2>背景</h2>
 * 中文否定常带反转:
 * <ul>
 *   <li>"不满意" vs "满意"</li>
 *   <li>"不想要" vs "想要"</li>
 *   <li>"无法登录" vs "登录"</li>
 *   <li>"千万别退款" -> 非投诉, 是反语 / 提醒</li>
 * </ul>
 * 关键词匹配会命中 "满意" 给 positive, 但加了 "不" 应是 negative.
 *
 * <h2>实现</h2>
 * <ol>
 *   <li>扫描否定词位置</li>
 *   <li>否定词到关键词的"作用域" (scope): 中文 3-5 字</li>
 *   <li>翻转作用域内情感词 / 意图</li>
 * </ol>
 *
 * <h2>作用域大小</h2>
 * 中文里否定通常影响后续 3-5 个字: "不/没/未/别/无法/不能 + 3~5 字"
 *
 * @author MiniMax
 * @since V3.5.6
 */
public final class NegationHandler {

    private NegationHandler() {}

    /** 否定前缀 */
    public static final List<String> NEGATION_PREFIXES = List.of(
            "不", "没", "未", "别", "无", "非", "勿", "未", "甭", "弗", "无法", "不能",
            "no", "not", "n't", "never", "without"
    );

    /** 否定作用域: 否定词后影响多少字 (中文 3-5 字) */
    public static final int NEGATION_SCOPE = 5;

    /**
     * 检测文本是否含否定上下文
     *
     * @param text 归一化后文本
     * @return 否定词位置 -> 作用域结束位置
     */
    public static Map<Integer, Integer> detectNegationScopes(String text) {
        Map<Integer, Integer> scopes = new LinkedHashMap<>();
        if (text == null) return scopes;
        String lower = text.toLowerCase(Locale.ROOT);
        for (String neg : NEGATION_PREFIXES) {
            int from = 0;
            while (true) {
                int idx = lower.indexOf(neg, from);
                if (idx < 0) break;
                int scopeEnd = Math.min(lower.length(), idx + neg.length() + NEGATION_SCOPE);
                scopes.put(idx, scopeEnd);
                from = idx + neg.length();
            }
        }
        return scopes;
    }

    /**
     * 判断某位置是否被否定
     *
     * @param pos 关键词位置
     * @param scopes 否定作用域表
     * @return true = 在某个否定作用域内
     */
    public static boolean isNegated(int pos, Map<Integer, Integer> scopes) {
        for (var e : scopes.entrySet()) {
            if (pos >= e.getKey() && pos < e.getValue()) return true;
        }
        return false;
    }

    /**
     * 翻转情感: 在否定上下文中的正向词变负向
     *
     * @param sentiment 原情感
     * @param keyword   命中的词
     * @param negated   是否在否定作用域
     * @return 翻转后情感
     */
    public static String flipSentiment(String sentiment, String keyword, boolean negated) {
        if (!negated) return sentiment;
        return switch (sentiment) {
            case "positive" -> "negative";
            case "negative" -> "positive";
            default -> sentiment;
        };
    }

    /**
     * 翻转意图: 否定 + 关键意图词 = 实际不是该意图
     * 简化: 不做完整意图级反转, 仅返回降权因子 (0.0~0.3)
     */
    public static double negationPenalty(boolean negated) {
        return negated ? 0.3 : 1.0;
    }
}
