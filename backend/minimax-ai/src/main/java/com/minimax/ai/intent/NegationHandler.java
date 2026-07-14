package com.minimax.ai.intent;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 否定处理 (Negation Handler) (V3.5.7 外部化配置版)
 *
 * <h2>为什么需要否定处理</h2>
 * 中文否定词 "不 / 没 / 别 / 莫" 经常反转句子情感:
 * <ul>
 *   <li>"我要退款" → 投诉 (正向命中关键词)</li>
 *   <li>"我不要退款" → 非投诉 (否定 + 关键词)</li>
 *   <li>"不 是 退 款" → 否定作用域 5 字覆盖 "退款"</li>
 *   <li>"我可能不是要退款" → 复杂否定, 转折</li>
 * </ul>
 * 简单关键词匹配会被 "我不要退款" 误判为投诉, 实际是非投诉.
 *
 * <h2>作用域 (Scope) 算法</h2>
 * <ol>
 *   <li>扫描文本, 找出所有否定词位置</li>
 *   <li>每个否定词向后延伸 {@code scope} 字符 (默认 5)</li>
 *   <li>作用域内的关键词命中, 应用 {@code negationPenalty} (默认 0.3)</li>
 * </ol>
 * 例: scope=5, "我不要退款"
 * <ul>
 *   <li>位置 1: "不" 否定词</li>
 *   <li>作用域: [1, 1+1+5) = [1, 7), 覆盖 "不要退款"</li>
 *   <li>位置 3: "退" 关键词, 在作用域内, 分数 × 0.3</li>
 * </ul>
 *
 * <h2>情感翻转 (Sentiment Flip)</h2>
 * 否定作用域内的情感词需要翻转:
 * <ul>
 *   <li>"我 喜欢" → positive</li>
 *   <li>"我 不 喜欢" → negative (翻转)</li>
 * </ul>
 *
 * <h2>线程安全</h2>
 * 使用 {@link AtomicReference} 包装, 支持热更新.
 *
 * @author MiniMax
 * @since V3.5.7
 */
public final class NegationHandler {

    /** 工具类不允许实例化 */
    private NegationHandler() {}

    /**
     * 否定前缀列表 (可热更新).
     * <p>默认: ["不", "没", "别", "莫", "无", "非", "未"]
     * <p>例: 文本 "我不要", 匹配 "不" (位置 1), 启动作用域
     */
    private static final AtomicReference<List<String>> PREFIXES = new AtomicReference<>(List.of());

    /**
     * 否定作用域大小 (可热更新).
     * <p>单位: 字符数 (中文 1 字符 = 1 字, 英文 1 字符 = 1 字母).
     * <p>默认 5: 覆盖 "不要退款" "不会取消订单" 等典型长度.
     * <p>太小: 覆盖不到关键词; 太大: 误判普通句.
     */
    private static final AtomicReference<Integer> SCOPE = new AtomicReference<>(5);

    /**
     * 设置否定前缀列表 (热更新入口).
     *
     * @param prefixes 否定词列表, 例: ["不", "没", "别"] (可空, 表示清空)
     */
    public static void setPrefixes(List<String> prefixes) {
        PREFIXES.set(prefixes != null ? new ArrayList<>(prefixes) : List.of());
    }

    /**
     * 设置否定作用域大小 (热更新入口).
     *
     * @param scope 作用域字符数, 最小 1, 默认 5
     */
    public static void setScope(int scope) {
        SCOPE.set(Math.max(1, scope));
    }

    /**
     * 检测文本中的所有否定作用域.
     * <p>主入口, 通常在 IntentPredictionService.predict() 调用.
     * <p>返回 Map: 起始位置 -> 结束位置, 一个否定词对应一个作用域.
     *
     * @param text 待检测文本 (已归一化)
     * @return 作用域列表, 例: {1=6} 表示位置 1-5 的否定作用域
     */
    public static Map<Integer, Integer> detectNegationScopes(String text) {
        // 入参兜底
        Map<Integer, Integer> scopes = new LinkedHashMap<>();
        if (text == null) return scopes;
        // 转小写 (英文场景)
        String lower = text.toLowerCase(Locale.ROOT);
        // 读取当前作用域大小
        int scope = SCOPE.get();
        // 遍历每个否定词, 找出所有出现位置
        for (String neg : PREFIXES.get()) {
            int from = 0;
            while (true) {
                // 找下一个出现位置
                int idx = lower.indexOf(neg, from);
                if (idx < 0) break;
                // 计算作用域结束位置 = 否定词位置 + 否定词长度 + 作用域
                int scopeEnd = Math.min(lower.length(), idx + neg.length() + scope);
                // 记录作用域: 起始位置 -> 结束位置
                scopes.put(idx, scopeEnd);
                // 继续向后找
                from = idx + neg.length();
            }
        }
        return scopes;
    }

    /**
     * 判断位置 {@code pos} 是否在任意否定作用域内.
     * <p>用于: 关键词命中时, 检查是否被否定.
     *
     * @param pos    关键词位置
     * @param scopes 作用域 Map (从 detectNegationScopes 得到)
     * @return true=被否定, false=正常
     */
    public static boolean isNegated(int pos, Map<Integer, Integer> scopes) {
        // 遍历所有作用域, 检查 pos 是否在其中
        for (var e : scopes.entrySet()) {
            // 作用域是 [起始, 结束) 左闭右开
            if (pos >= e.getKey() && pos < e.getValue()) return true;
        }
        return false;
    }

    /**
     * 否定作用域内的情感翻转.
     * <p>positive ↔ negative, neutral 保持不变.
     *
     * @param sentiment 当前情感 ("positive" / "negative" / "neutral")
     * @param keyword   命中的关键词 (用于日志调试)
     * @param negated   是否被否定
     * @return 翻转后的情感
     */
    public static String flipSentiment(String sentiment, String keyword, boolean negated) {
        // 未否定: 原样返回
        if (!negated) return sentiment;
        // 翻转: positive <-> negative, neutral 保持
        return switch (sentiment) {
            case "positive" -> "negative";
            case "negative" -> "positive";
            default -> sentiment;
        };
    }

    /**
     * 否定惩罚系数.
     * <p>关键词被否定时, 分数乘以 0.3 (削弱 70%).
     * <p>为什么不归零: 避免完全抑制, 保留语义的 '反讽 / 强调' 可能性.
     *
     * @param negated 是否被否定
     * @return 惩罚系数 (0.3 或 1.0)
     */
    public static double negationPenalty(boolean negated) {
        return negated ? 0.3 : 1.0;
    }
}
