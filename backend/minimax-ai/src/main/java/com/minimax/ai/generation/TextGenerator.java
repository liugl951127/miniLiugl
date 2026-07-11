package com.minimax.ai.generation;

import com.minimax.ai.model.MiniTransformer;
import com.minimax.ai.tokenizer.ChineseTokenizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 文本生成器 (V2.5 自研)
 *
 * 集成:
 *   1. Transformer forward (深度学习打分)
 *   2. N-gram 统计 (注入先验, 让输出更合理)
 *   3. Beam search / Top-K + Top-P 采样
 *
 * 输出策略:
 *   - 70% 权重: N-gram 统计 (基于训练语料)
 *   - 30% 权重: Transformer logits
 *
 * 这是 hybrid 方案, 让自研 AI 在少训练数据情况下也能输出"看起来合理"的文本
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TextGenerator {

    private final MiniTransformer transformer;
    private final ChineseTokenizer tokenizer;

    /** N-gram 统计: tokenId -> nextTokenId -> count */
    private Map<Integer, Map<Integer, Integer>> bigramStats = new HashMap<>();
    private boolean statsReady = false;

    /**
     * 注入 n-gram 统计 (从训练语料)
     */
    public void setBigramStats(Map<Integer, Map<Integer, Integer>> stats) {
        this.bigramStats = stats;
        this.statsReady = true;
        log.info("Bigram 统计已加载: {} unique keys", stats.size());
    }

    /**
     * 文本生成
     *
     * @param prompt 输入提示
     * @param maxLen 最大生成长度
     * @param temperature 温度 (0-2, 越大越发散)
     * @return 生成的文本
     */
    public String generate(String prompt, int maxLen, double temperature) {
        if (prompt == null || prompt.isEmpty()) {
            prompt = "你好";
        }

        int[] inputIds = tokenizer.encode(prompt);
        if (inputIds.length == 0) return prompt;

        List<Integer> generated = new ArrayList<>();
        for (int id : inputIds) generated.add(id);

        int maxSeqLen = transformer.getMaxSeqLen();
        int vocabSize = transformer.getVocabSize();

        Random random = new Random();
        for (int step = 0; step < maxLen; step++) {
            // 准备输入: 最近 maxSeqLen 个 token
            int start = Math.max(0, generated.size() - maxSeqLen);
            int[] input = new int[generated.size() - start];
            for (int i = 0; i < input.length; i++) {
                input[i] = generated.get(start + i);
            }

            // 1. Transformer 预测
            double[][] logits = transformer.forward(input);
            double[] lastLogits = logits[logits.length - 1];

            // 2. Hybrid: 结合 bigram 统计
            double[] finalScores = new double[vocabSize];
            int lastToken = generated.get(generated.size() - 1);

            // 70% bigram + 30% transformer
            if (statsReady && bigramStats.containsKey(lastToken)) {
                Map<Integer, Integer> candidates = bigramStats.get(lastToken);
                int totalCount = candidates.values().stream().mapToInt(Integer::intValue).sum();
                for (Map.Entry<Integer, Integer> e : candidates.entrySet()) {
                    finalScores[e.getKey()] = 0.7 * Math.log((double) e.getValue() / totalCount + 1e-6);
                }
            }
            for (int i = 0; i < vocabSize; i++) {
                finalScores[i] += 0.3 * lastLogits[i];
            }

            // 3. 温度
            if (temperature > 0) {
                for (int i = 0; i < vocabSize; i++) {
                    finalScores[i] /= temperature;
                }
            }

            // 4. 采样 (top-k=40 + top-p=0.9)
            int nextToken = sampleWithTopKTopP(finalScores, 40, 0.9, random);

            // 5. 终止: 遇到 EOS 或 <unk> 多次
            if (nextToken == ChineseTokenizer.EOS) break;
            if (nextToken == ChineseTokenizer.UNK && random.nextDouble() < 0.5) break;
            // 防重复: 连续 3 个相同 token
            if (generated.size() >= 3
                    && generated.get(generated.size() - 1) == nextToken
                    && generated.get(generated.size() - 2) == nextToken
                    && generated.get(generated.size() - 3) == nextToken) {
                break;
            }

            generated.add(nextToken);
        }

        // 解码 (只返回新生成的部分)
        int newLen = generated.size() - inputIds.length;
        int[] outputIds = new int[Math.max(0, newLen)];
        for (int i = 0; i < outputIds.length; i++) {
            outputIds[i] = generated.get(inputIds.length + i);
        }
        return tokenizer.decode(outputIds);
    }

    /**
     * Top-K + Top-P 采样
     */
    private int sampleWithTopKTopP(double[] scores, int k, double p, Random random) {
        int n = scores.length;

        // argsort
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(scores[b], scores[a]));

        // top-k 截断
        int topK = Math.min(k, n);
        for (int i = topK; i < n; i++) {
            scores[idx[i]] = Double.NEGATIVE_INFINITY;
        }

        // softmax
        double max = Double.NEGATIVE_INFINITY;
        for (double s : scores) if (s > max) max = s;
        double sum = 0;
        for (int i = 0; i < n; i++) {
            if (Double.isFinite(scores[i])) {
                scores[i] = Math.exp(scores[i] - max);
                sum += scores[i];
            } else {
                scores[i] = 0;
            }
        }
        for (int i = 0; i < n; i++) scores[i] /= sum;

        // top-p (nucleus) 截断
        double cumSum = 0;
        int cutoff = 0;
        for (int i = 0; i < n; i++) {
            cumSum += scores[idx[i]];
            cutoff++;
            if (cumSum >= p) break;
        }
        for (int i = cutoff; i < n; i++) scores[idx[i]] = 0;

        // 重新归一化
        sum = 0;
        for (double s : scores) sum += s;
        for (int i = 0; i < n; i++) scores[i] /= sum;

        // 采样
        double r = random.nextDouble();
        double acc = 0;
        for (int i = 0; i < n; i++) {
            acc += scores[i];
            if (r <= acc) return i;
        }
        return idx[0];
    }
}
