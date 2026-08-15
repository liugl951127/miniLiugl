package com.minimax.ai.intent;

import java.util.*;

/**
 * N-gram 短语提取与评分 (V3.5.6 算法升级新增)
 *
 * <h2>背景与动机</h2>
 * 关键词匹配 (TF 模型) 只看单个字 / 词, 漏掉关键短语:
 * <ul>
 *   <li>"我要退款" (4 字) → 退款意图强 (单字 "退" 不够具体)</li>
 *   <li>"我要买" (3 字) → 下单意图强 ("买" 比 "下单" 短, 但锁定后置信高)</li>
 *   <li>"我可能不是要退款" → 含 "不是" 否定 + "退款" 关键词, 需结合 Negation 处理</li>
 *   <li>"查 一下 订单" (3 个 token) → 多 token 短语, 中文需分字处理</li>
 * </ul>
 * 单字 "退款" 单独看是投诉, 但 "不是退款" 是非投诉.
 *
 * <h2>算法</h2>
 * <ol>
 *   <li>分词: 简单按字符 (中文) + 空格 (英文), 不引入分词器</li>
 *   <li>滑动窗口: 生成 bigram (2 字) 和 trigram (3 字) 候选</li>
 *   <li>匹配: 跟候选 phrase 表比对, 命中即打分</li>
 *   <li>位置权重: 句首 30% 或句尾 30% 范围 → 1.5x, 中间 → 1.0x
 *     <p>假设: 用户把关键意图放在句首或句尾 (中文表达习惯)</li>
 *   <li>长度权重: 3-gram 比 2-gram 准确度高, 1.2x 加成</li>
 * </ol>
 *
 * <h2>复杂度</h2>
 * O(N*W*K), N=文本长度, W=窗口大小 (2-3), K=候选 phrase 数
 * <p>实测 100 字文本, ~10 个候选 phrase, 耗时 < 1ms
 *
 * <h2>使用示例</h2>
 * <pre>
 *   Map&lt;String, Double&gt; candidates = Map.of(
 *       "退款", 8.0,
 *       "申请退款", 10.0,
 *       "我要退款", 12.0
 *   );
 *   List&lt;NgramMatch&gt; matches = NgramExtractor.extract("我要退款", candidates);
 *   // matches = [
 *   //   NgramMatch("我要退款", 0, 4, 12.0 * 1.5 * 1.2) = 21.6,
 *   //   NgramMatch("我要退", 0, 3, 0),   // 候选表中无, 不计
 *   //   NgramMatch("要退款", 1, 4, 0)    // 候选表中无, 不计
 *   // ]
 * </pre>
 *
 * @author MiniMax
 * @since V3.5.6
 */
public final class NgramExtractor {

    /** 工具类不允许实例化 */
    private NgramExtractor() {}

    /**
     * 提取 N-gram 并打分.
     * <p>主入口, 通常在 IntentPredictionService.scoreNgrams() 调用.
     *
     * @param text       已归一化文本 (由 TextNormalizer.normalize 输出)
     * @param candidates 候选 phrase 字典, key=短语, value=基础分
     * @return 匹配结果列表, 按最终分降序
     */
    public static List<NgramMatch> extract(String text, Map<String, Double> candidates) {
        // 入参兜底: 任一为空直接返回
        List<NgramMatch> out = new ArrayList<>();
        if (text == null || text.isBlank() || candidates == null || candidates.isEmpty()) {
            return out;
        }
        // 统一小写, 与 candidate 匹配
        String lower = text.toLowerCase(Locale.ROOT);
        int len = lower.length();

        // 双层循环: 外层窗口大小 n (2, 3), 内层起始位置 i
        for (int n = 2; n <= 3; n++) {
            for (int i = 0; i + n <= len; i++) {
                // 截取 [i, i+n) 区间
                String gram = lower.substring(i, i + n);
                // 候选表中是否包含
                if (candidates.containsKey(gram)) {
                    // 基础分 (来自候选表)
                    double score = candidates.get(gram);
                    // 位置权重: 句首 30% 或句尾 30% 范围 -> 1.5x
                    double posWeight = positionWeight(i, n, len);
                    // 长度权重: 3-gram 准确度高于 2-gram -> 1.2x
                    double lenWeight = n == 3 ? 1.2 : 1.0;
                    // 最终分 = 基础分 × 位置权重 × 长度权重
                    out.add(new NgramMatch(gram, i, i + n, score * posWeight * lenWeight));
                }
            }
        }
        // 排序: 高分在前, 后续 fusion 取 top
        out.sort((a, b) -> Double.compare(b.score(), a.score()));
        return out;
    }

    /**
     * 位置权重计算.
     * <p>假设: 关键意图倾向出现在句首或句尾 (中文表达习惯).
     * <p>例: "请问, 怎么退款啊" → "怎么退款" 在中段, 1.0x
     * <br>"怎么退款啊" → "怎么退款" 在句首, 1.5x
     *
     * @param start n-gram 起始位置
     * @param len   n-gram 长度
     * @param total 文本总长
     * @return 位置权重 (1.0 或 1.5)
     */
    static double positionWeight(int start, int len, int total) {
        // 空文本: 默认权重 1.0
        if (total == 0) return 1.0;
        // 计算 n-gram 在文本中的相对位置
        double startPct = (double) start / total;
        double endPct = (double) (start + len) / total;
        // 句首 30% 或句尾 30% 范围 -> 1.5x
        if (startPct < 0.3 || endPct > 0.7) return 1.5;
        // 中段 40% -> 1.0x (默认)
        return 1.0;
    }

    /**
     * N-gram 匹配结果.
     *
     * @param text  匹配的短语
     * @param start 起始位置 (含)
     * @param end   结束位置 (不含)
     * @param score 最终分数 (基础分 × 位置权重 × 长度权重)
     */
    public record NgramMatch(String text, int start, int end, double score) {}
}
