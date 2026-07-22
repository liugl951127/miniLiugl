package com.minimax.ai.generation.model;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.minimax.ai.model.MiniTransformer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 神经意图模型 (V3.5.16+)
 *
 * <h3>原理</h3>
 * 用 MiniTransformer 提 query embedding, 与训练集 embedding 算余弦相似度.
 * 相似度最高的训练文本对应的 intent = 推断结果.
 *
 * <h3>对比 NgramModel</h3>
 * <table>
 *   <tr><th></th><th>N-gram</th><th>Neural (本类)</th></tr>
 *   <tr><td>特征</td><td>离散词搭配</td><td>连续语义向量</td></tr>
 *   <tr><td>未见词</td><td>概率 0 (无法识别)</td><td>embedding 近似匹配</td></tr>
 *   <tr><td>同义改写</td><td>依赖同义词扩展</td><td>自动 (语义近)</td></tr>
 *   <tr><td>训练成本</td><td>0 (语料库)</td><td>0 (用 MiniTransformer 抽 embedding)</td></tr>
 *   <tr><td>推理</td><td>O(query bigrams × intent ngrams)</td><td>O(query embed + candidates embed)</td></tr>
 * </table>
 *
 * <h3>局限</h3>
 * MiniTransformer 是 V2.5 随机初始化, **没真训练**. 但同 intent 的训练文本
 * 由于共享 token embedding 模式, 仍有"粗糙"的相似度区分.
 * 后续 V3.5.17+ 可用对比学习 (SimCSE) 微调.
 *
 * @author MiniMax
 * @since V3.5.16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NeuralIntentModel {

    private final QueryEmbedder embedder;

    /**
     * 训练集: intent → (training text → embedding)
     */
    private final Map<String, Map<String, double[]>> trainIndex = new ConcurrentHashMap<>();

    /**
     * 训练集总数 (debug 用)
     */
    private int totalTrainingTexts = 0;

    @PostConstruct
    public void init() {
        // 用 NgramModel 的同一份训练集
        train(NgramModel.defaultTrainingData());
    }

    /**
     * 训练: 把训练文本 embed 后入库
     */
    public void train(Map<String, List<String>> textsByIntent) {
        log.info("[neural] training: {} intents", textsByIntent.keySet());

        trainIndex.clear();
        for (Map.Entry<String, List<String>> entry : textsByIntent.entrySet()) {
            String intent = entry.getKey();
            List<String> texts = entry.getValue();

            Map<String, double[]> textEmbeds = new HashMap<>();
            for (String text : texts) {
                textEmbeds.put(text, embedder.embed(text));
            }
            trainIndex.put(intent, textEmbeds);
        }
        totalTrainingTexts = textsByIntent.values().stream().mapToInt(List::size).sum();
        log.info("[neural] trained: {} intents, {} texts",
                trainIndex.size(), totalTrainingTexts);
    }

    /**
     * 推理: query 与每个 intent 的所有训练文本算相似度, 取最高平均
     *
     * @param query 用户输入
     * @return intent → 得分 (0~1)
     */
    public Map<String, Double> score(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        double[] qVec = embedder.embed(query);
        Map<String, Double> scores = new HashMap<>();

        for (Map.Entry<String, Map<String, double[]>> entry : trainIndex.entrySet()) {
            String intent = entry.getKey();

            // Top-3 相似度平均 (避免单文本极端值)
            double[] similarities = new double[entry.getValue().size()];
            int idx = 0;
            for (double[] tVec : entry.getValue().values()) {
                similarities[idx++] = this_similarity(qVec, tVec);
            }
            Arrays.sort(similarities);

            // 取 top-3 平均 (如果训练文本 < 3, 取全部)
            int k = Math.min(3, similarities.length);
            double sum = 0;
            for (int i = similarities.length - k; i < similarities.length; i++) {
                sum += similarities[i];
            }
            scores.put(intent, sum / k);
        }
        return scores;
    }

    private static double this_similarity(double[] a, double[] b) {
        return MiniTransformer.cosineSimilarity(a, b);
    }

    public int size() {
        return totalTrainingTexts;
    }
}
