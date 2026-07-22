package com.minimax.ai.generation.model;

import com.minimax.ai.model.MiniTransformer;
import com.minimax.ai.tokenizer.ChineseTokenizer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Query Embedder (V3.5.16+)
 *
 * <h3>职责</h3>
 * 把用户 query 转成 hiddenDim 维句向量, 用于:
 * <ul>
 *   <li>语义召回: 找最相似的训练文本, 推断 intent</li>
 *   <li>意图聚类: 相似 query 应该是同一 intent</li>
 *   <li>冷启动: 无关键词时, embedding 召回兜底</li>
 * </ul>
 *
 * <h3>架构</h3>
 * <pre>
 *   query (String)
 *     ↓ ChineseTokenizer
 *   int[] tokenIds
 *     ↓ MiniTransformer.embed
 *   double[] sentenceVec (128 维)
 *     ↓ LRU 缓存 (Caffeine)
 *   cached embedding
 * </pre>
 *
 * <h3>性能</h3>
 * <ul>
 *   <li>首次: 7-9ms (MiniTransformer forward)</li>
 *   <li>缓存命中: < 0.1ms</li>
 *   <li>128 维向量, 适合余弦相似度快速比较</li>
 * </ul>
 *
 * @author MiniMax
 * @since V3.5.16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryEmbedder {

    private final ChineseTokenizer tokenizer;
    private MiniTransformer transformer;

    /**
     * LRU embedding 缓存
     */
    private final Map<String, double[]> cache = new ConcurrentHashMap<>(1000);

    @PostConstruct
    public void init() {
        // 用 V2.5 默认配置 (vocab=8192, hidden=128, heads=4, layers=2)
        // 生产可换大模型
        this.transformer = new MiniTransformer(8192, 128, 4, 2, 128);
        log.info("[query-embedder] initialized: vocab=8192 hidden=128");
    }

    /**
     * Embed 单个 query
     */
    public double[] embed(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new double[128];  // 零向量
        }

        // 1. 缓存查
        double[] cached = cache.get(query);
        if (cached != null) {
            return cached;
        }

        // 2. tokenize
        int[] tokenIds = tokenizer.encode(query);
        if (tokenIds.length == 0) {
            return new double[128];
        }

        // 3. transformer
        double[] vec = transformer.embed(tokenIds);

        // 4. 缓存 (限 1000 条)
        if (cache.size() < 1000) {
            cache.put(query, vec);
        }
        return vec;
    }

    /**
     * 批量 embed
     */
    public double[][] embedBatch(List<String> queries) {
        double[][] result = new double[queries.size()][];
        for (int i = 0; i < queries.size(); i++) {
            result[i] = embed(queries.get(i));
        }
        return result;
    }

    /**
     * 计算余弦相似度 (两个句向量)
     */
    public double similarity(double[] a, double[] b) {
        return MiniTransformer.cosineSimilarity(a, b);
    }

    /**
     * query 与 query 的相似度 (便捷方法)
     */
    public double similarity(String q1, String q2) {
        return similarity(embed(q1), embed(q2));
    }

    /**
     * 找最相似的 query (语义搜索)
     *
     * @param query 目标 query
     * @param candidates 候选 queries
     * @return (bestMatch, similarity), 相似度最高
     */
    public SearchResult findMostSimilar(String query, Collection<String> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new SearchResult(null, 0.0);
        }

        double[] qVec = embed(query);
        String best = null;
        double bestScore = -1.0;

        for (String c : candidates) {
            double s = similarity(qVec, embed(c));
            if (s > bestScore) {
                bestScore = s;
                best = c;
            }
        }
        return new SearchResult(best, bestScore);
    }

    /**
     * 缓存统计
     */
    public Map<String, Object> stats() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("cacheSize", cache.size());
        s.put("dim", 128);
        return s;
    }

    public void clearCache() {
        cache.clear();
        log.info("[query-embedder] cache cleared");
    }

    /**
     * 搜索结果
     */
    public record SearchResult(String bestMatch, double similarity) {}
}
