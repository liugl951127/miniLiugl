package com.minimax.ai.intent;

import java.util.*;

/**
 * N-gram 提取与评分 (V3.5.6 算法升级新增)
 *
 * <h2>背景</h2>
 * 关键词匹配只看单个词, 漏掉关键短语:
 * <ul>
 *   <li>"我要退款" (4 字) -> 退款意图强</li>
 *   <li>"我要买" (3 字) -> 下单意图强</li>
 *   <li>"我可能不是要退款, 谢谢" -> 否定 + 转折</li>
 * </ul>
 * 单字 "退款" 单独看是投诉, 但 "不是退款" 是非投诉。
 *
 * <h2>实现</h2>
 * <ol>
 *   <li>分词 (按字符 / 词)</li>
 *   <li>滑动窗口生成 bigram (2 字) + trigram (3 字)</li>
 *   <li>跟候选 phrase 表匹配 (打预存分)</li>
 *   <li>位置权重: 句首/句尾 +0.5, 句中 +0</li>
 * </ol>
 *
 * <h2>复杂度</h2>
 * O(N*W*K) N=文本长度, W=窗口大小 (2-3), K=候选 phrase 数
 *
 * @author MiniMax
 * @since V3.5.6
 */
public final class NgramExtractor {

    private NgramExtractor() {}

    /**
     * 提取 N-gram (2-3 字) 并与候选 phrase 表匹配打分
     *
     * @param text 已分词的文本 (空格分隔, 或用 {@link ChineseTokenizer})
     * @param candidates 候选 phrase 字典: phrase -> 分数
     * @return 匹配结果, 按分数降序
     */
    public static List<NgramMatch> extract(String text, Map<String, Double> candidates) {
        List<NgramMatch> out = new ArrayList<>();
        if (text == null || text.isBlank() || candidates == null || candidates.isEmpty()) {
            return out;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        int len = lower.length();

        // 提取 n-gram
        for (int n = 2; n <= 3; n++) {
            for (int i = 0; i + n <= len; i++) {
                String gram = lower.substring(i, i + n);
                if (candidates.containsKey(gram)) {
                    double score = candidates.get(gram);
                    // 位置权重: 句首/句尾 +0.5, 句中 +0.0
                    double posWeight = positionWeight(i, n, len);
                    // 长度权重: 3-gram 准确度高于 2-gram
                    double lenWeight = n == 3 ? 1.2 : 1.0;
                    out.add(new NgramMatch(gram, i, i + n, score * posWeight * lenWeight));
                }
            }
        }
        // 按分数降序
        out.sort((a, b) -> Double.compare(b.score(), a.score()));
        return out;
    }

    /** 位置权重: 句首 30% 范围, 句尾 30% 范围, 中间 40% */
    static double positionWeight(int start, int len, int total) {
        if (total == 0) return 1.0;
        double startPct = (double) start / total;
        double endPct = (double) (start + len) / total;
        if (startPct < 0.3 || endPct > 0.7) return 1.5;
        return 1.0;
    }

    /** N-gram 匹配结果 */
    public record NgramMatch(String text, int start, int end, double score) {}
}
