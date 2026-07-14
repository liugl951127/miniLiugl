package com.minimax.ai.intent;

import java.util.*;

/**
 * 加权投票融合 (Weighted Voting Ensemble) (V3.5.6 算法升级新增)
 *
 * <h2>背景</h2>
 * 单模型总有偏差. 业界经验: 多个弱模型加权融合, 准确率常比单强模型高 5-10%.
 *
 * <h2>4 个子模型</h2>
 * <ol>
 *   <li><b>关键词密度 (TF)</b>: 关键词出现次数 × 权重 / 文本总词数</li>
 *   <li><b>N-gram 短语</b>: 2-3 字关键短语 + 位置权重</li>
 *   <li><b>同义词扩展</b>: 归一化后扩展词 + 归一化词</li>
 *   <li><b>上下文继承</b>: 上一轮对话的意图 (短时记忆, 上一轮未关闭则继承)</li>
 * </ol>
 *
 * <h2>融合公式</h2>
 * <pre>
 *   final_score(intent) = Σ (model_score_i(intent) × weight_i)
 *   confidence = softmax(top_score - second_score) ≈ max_normalized
 * </pre>
 *
 * <h2>模型权重 (经验值, 可配置)</h2>
 * <ul>
 *   <li>关键词 TF: 0.4 (基础分, 召回保证)</li>
 *   <li>N-gram 短语: 0.3 (精度强, 短语锁定)</li>
 *   <li>同义词扩展: 0.2 (召回补充)</li>
 *   <li>上下文继承: 0.1 (兜底, 多轮用)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V3.5.6
 */
public final class WeightedVotingEnsemble {

    private WeightedVotingEnsemble() {}

    /** 默认 4 模型权重 */
    public static final double[] DEFAULT_WEIGHTS = { 0.4, 0.3, 0.2, 0.1 };

    /**
     * 融合 4 模型投票
     *
     * @param tfScores    关键词 TF 模型打分 [intent -> score]
     * @param ngramScores N-gram 模型打分 [intent -> score]
     * @param expandScores 同义词扩展打分 [intent -> score]
     * @param contextScore 上下文继承打分 (单个 intent 或 null)
     * @param weights    4 模型权重
     * @return 各意图最终分数 [intent -> finalScore]
     */
    public static Map<String, Double> fuse(
            Map<String, Double> tfScores,
            Map<String, Double> ngramScores,
            Map<String, Double> expandScores,
            String contextIntent,
            double[] weights) {
        if (weights == null || weights.length < 4) weights = DEFAULT_WEIGHTS;
        Map<String, Double> out = new HashMap<>();
        // 收集所有 intent
        Set<String> allIntents = new HashSet<>();
        if (tfScores != null) allIntents.addAll(tfScores.keySet());
        if (ngramScores != null) allIntents.addAll(ngramScores.keySet());
        if (expandScores != null) allIntents.addAll(expandScores.keySet());
        // 加权求和
        for (String intent : allIntents) {
            double s1 = tfScores != null ? tfScores.getOrDefault(intent, 0.0) : 0.0;
            double s2 = ngramScores != null ? ngramScores.getOrDefault(intent, 0.0) : 0.0;
            double s3 = expandScores != null ? expandScores.getOrDefault(intent, 0.0) : 0.0;
            double s4 = (contextIntent != null && intent.equals(contextIntent)) ? 1.0 : 0.0;
            double finalScore = s1 * weights[0] + s2 * weights[1]
                    + s3 * weights[2] + s4 * weights[3];
            if (finalScore > 0) out.put(intent, finalScore);
        }
        return out;
    }

    /**
     * 计算置信度: top1 vs top2 差距
     * softmax 风格: confidence = (e^s1 - e^s2) / (e^s1 + e^s2)
     * 简化: confidence = sigmoid(s1 - s2)
     *
     * @param sortedScores 排序后的分数列表
     * @param scale        缩放因子 (默认 5.0, 越小越敏感)
     */
    public static double confidence(List<Map.Entry<String, Double>> sortedScores, double scale) {
        if (sortedScores.isEmpty()) return 0.0;
        if (sortedScores.size() == 1) return Math.min(1.0, sortedScores.get(0).getValue());
        double s1 = sortedScores.get(0).getValue();
        double s2 = sortedScores.get(1).getValue();
        double diff = s1 - s2;
        return 0.5 + 0.5 * Math.tanh(diff / Math.max(0.1, scale));
    }

    /** 兼容旧 API (scale=5.0) */
    public static double confidence(List<Map.Entry<String, Double>> sortedScores) {
        return confidence(sortedScores, 5.0);
    }

    /**
     * Softmax 归一化 (保留可读性)
     */
    public static Map<String, Double> softmax(Map<String, Double> scores) {
        if (scores == null || scores.isEmpty()) return Map.of();
        double max = scores.values().stream().max(Double::compare).orElse(0.0);
        double sum = 0.0;
        Map<String, Double> exp = new HashMap<>();
        for (var e : scores.entrySet()) {
            double v = Math.exp(e.getValue() - max);
            exp.put(e.getKey(), v);
            sum += v;
        }
        Map<String, Double> result = new HashMap<>();
        for (var e : exp.entrySet()) {
            result.put(e.getKey(), e.getValue() / sum);
        }
        return result;
    }
}
