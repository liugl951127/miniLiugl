package com.minimax.ai.intent;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Negation Handling / 否定处理 (V3.5.7 外部化配置版)
 *
 * @author MiniMax
 * @since V3.5.7
 */
public final class NegationHandler {

    private NegationHandler() {}

    /** 否定前缀 (可热更新) */
    private static final AtomicReference<List<String>> PREFIXES = new AtomicReference<>(List.of());
    /** 否定作用域大小 (可热更新) */
    private static final AtomicReference<Integer> SCOPE = new AtomicReference<>(5);

    /** 设置否定前缀 */
    public static void setPrefixes(List<String> prefixes) {
        PREFIXES.set(prefixes != null ? new ArrayList<>(prefixes) : List.of());
    }

    /** 设置作用域 */
    public static void setScope(int scope) {
        SCOPE.set(Math.max(1, scope));
    }

    /**
     * 检测文本是否含否定上下文
     */
    public static Map<Integer, Integer> detectNegationScopes(String text) {
        Map<Integer, Integer> scopes = new LinkedHashMap<>();
        if (text == null) return scopes;
        String lower = text.toLowerCase(Locale.ROOT);
        int scope = SCOPE.get();
        for (String neg : PREFIXES.get()) {
            int from = 0;
            while (true) {
                int idx = lower.indexOf(neg, from);
                if (idx < 0) break;
                int scopeEnd = Math.min(lower.length(), idx + neg.length() + scope);
                scopes.put(idx, scopeEnd);
                from = idx + neg.length();
            }
        }
        return scopes;
    }

    public static boolean isNegated(int pos, Map<Integer, Integer> scopes) {
        for (var e : scopes.entrySet()) {
            if (pos >= e.getKey() && pos < e.getValue()) return true;
        }
        return false;
    }

    public static String flipSentiment(String sentiment, String keyword, boolean negated) {
        if (!negated) return sentiment;
        return switch (sentiment) {
            case "positive" -> "negative";
            case "negative" -> "positive";
            default -> sentiment;
        };
    }

    public static double negationPenalty(boolean negated) {
        return negated ? 0.3 : 1.0;
    }
}
