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
        // 步骤 1: 权重参数兜底. 任何 null 或长度不足 4 的输入, 都用默认值
        // 原因: 集成方在配置更新时可能传不完整数组, 不能因为 NPE 让主链路崩
        if (weights == null || weights.length < 4) weights = DEFAULT_WEIGHTS;
        // 步骤 2: 准备结果 Map, intent -> 最终分
        Map<String, Double> out = new HashMap<>();
        // 步骤 3: 收集所有出现的意图 (3 模型 + 1 上下文)
        Set<String> allIntents = new HashSet<>();
        if (tfScores != null)     allIntents.addAll(tfScores.keySet());
        if (ngramScores != null)  allIntents.addAll(ngramScores.keySet());
        if (expandScores != null) allIntents.addAll(expandScores.keySet());
        // 步骤 4: 对每个意图, 4 模型加权求和
        //   s1 = 关键词 TF 分 (基础分)
        //   s2 = N-gram 短语分 (精度分)
        //   s3 = 同义词扩展分 (召回补充)
        //   s4 = 上下文继承分 (短时记忆, 仅当前 intent 与上轮一致时为 1.0)
        for (String intent : allIntents) {
            double s1 = tfScores     != null ? tfScores.getOrDefault(intent, 0.0)     : 0.0;
            double s2 = ngramScores  != null ? ngramScores.getOrDefault(intent, 0.0)  : 0.0;
            double s3 = expandScores != null ? expandScores.getOrDefault(intent, 0.0) : 0.0;
            // 上下文项: 命中时给 1.0, 由外部权重 0.1 衰减 (避免覆盖主意图)
            double s4 = (contextIntent != null && intent.equals(contextIntent)) ? 1.0 : 0.0;
            // 加权求和: finalScore = Σ (model_i * weight_i)
            double finalScore = s1 * weights[0] + s2 * weights[1]
                    + s3 * weights[2] + s4 * weights[3];
            // 过滤 0 分意图, 减少输出体积
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
        // 空集合: 无分数 = 无置信
        if (sortedScores.isEmpty()) return 0.0;
        // 单一意图: 把 top1 分限制在 [0, 1] 作为置信
        if (sortedScores.size() == 1) return Math.min(1.0, sortedScores.get(0).getValue());
        // 多意图: 用 top1 - top2 差距, 衡量 '领先优势'
        double s1 = sortedScores.get(0).getValue();  // 冠军分
        double s2 = sortedScores.get(1).getValue();  // 亚军分
        double diff = s1 - s2;                       // 领先优势
        // tanh 平滑: diff/scale 越正 -> 置信越接近 1
        // 0.5 偏移: 让输出从 0.5 起步, 差为 0 时输出 0.5 (中性)
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
        // 空入参: 返回空 map, 不报错
        if (scores == null || scores.isEmpty()) return Map.of();
        // 步骤 1: 减去最大值, 数值稳定 (避免 e^x 爆炸)
        double max = scores.values().stream().max(Double::compare).orElse(0.0);
        // 步骤 2: 逐个计算 e^(x-max), 累加总和
        double sum = 0.0;
        Map<String, Double> exp = new HashMap<>();
        for (var e : scores.entrySet()) {
            double v = Math.exp(e.getValue() - max);
            exp.put(e.getKey(), v);
            sum += v;
        }
        // 步骤 3: 归一化 (除以总和) -> 概率分布
        Map<String, Double> result = new HashMap<>();
        for (var e : exp.entrySet()) {
            result.put(e.getKey(), e.getValue() / sum);
        }
        return result;
    }
}
