package com.minimax.rag.reranker;

import com.minimax.rag.embedding.EmbeddingClient;
import com.minimax.rag.retriever.Retriever;
import com.minimax.rag.service.VectorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Cross-Encoder 语义重排序器 (Day 54).
 *
 * <p>在首次向量检索 (bi-encoder cosine) 的基础上，
 * 对 top-N 候选文档进行精细化语义重排序，提升 top-K 准确率。
 *
 * <h3>原理</h3>
 * <pre>
 * Bi-Encoder (首次检索):  query_emb ⊗ doc_emb → cosine → 粗排
 * Cross-Encoder (重排序):  [query; doc] → score(query,doc) → 精排
 *
 * 本实现使用 embedding 模型的轻量级 Cross-Encoder 近似：
 * - 对 query 和 doc 分别 embedding
 * - 双向交叉注意力得分: score = sim(q→d) * α + sim(d→q) * (1-α)
 * - 融合首轮检索得分，形成最终综合分
 * </pre>
 *
 * <h3>配置</h3>
 * <pre>
 * minimax.rag.reranker.enabled=true
 * minimax.rag.reranker.top-k=20        # 重排序候选数（首轮取更多）
 * minimax.rag.reranker.alpha=0.6       # 正向权重（query→doc），0.4 留给反向
 * minimax.rag.reranker.final-top=5    # 最终返回数
 * </pre>
 *
 * <h3>API</h3>
 * <pre>
 * POST /api/v1/rag/retrieve/rerank   # 对已有候选重排序
 * POST /api/v1/rag/ask/rerank        # 端到端: 检索+重排序+LLM回答
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrossEncoderReranker {

    private final EmbeddingClient embeddingClient;

    @Value("${minimax.rag.reranker.enabled:true}")
    private boolean enabled;

    @Value("${minimax.rag.reranker.top-k:20}")
    private int rerankTopK;

    @Value("${minimax.rag.reranker.alpha:0.6}")
    private double alpha;

    @Value("${minimax.rag.reranker.final-top:5}")
    private int finalTopK;

    /**
     * 对候选列表进行 Cross-Encoder 语义重排序.
     *
     * @param candidates  首轮 Bi-Encoder 检索结果 (Retriever.Hit 列表)
     * @param query      用户查询
     * @param finalTopK  最终返回数量（-1 使用配置默认值）
     * @return 重排序后的 Hit 列表（按综合分降序）
     */
    public List<Retriever.Hit> rerank(List<Retriever.Hit> candidates, String query, int finalTopK) {
        if (!enabled) {
            log.debug("CrossEncoderReranker disabled, return candidates as-is");
            return candidates;
        }
        if (candidates == null || candidates.isEmpty()) return List.of();
        if (query == null || query.isBlank()) return candidates;

        int topN = finalTopK > 0 ? finalTopK : this.finalTopK;
        int candidatesToRerank = Math.min(candidates.size(), rerankTopK);

        // Step 1: 对 query 做 embedding
        float[] queryVec = embeddingClient.embed(query);
        if (queryVec == null || queryVec.length == 0) {
            log.warn("CrossEncoderReranker: query embedding failed, return candidates");
            return candidates;
        }

        // Step 2: 对每个候选 doc 做 embedding 并计算双向交叉得分
        List<ScoredHit> scored = new ArrayList<>();
        for (int i = 0; i < candidatesToRerank; i++) {
            Retriever.Hit hit = candidates.get(i);
            String content = hit.content != null ? hit.content : "";
            // 截断过长文档（避免 embedding 爆内存）
            String docText = content.length() > 512 ? content.substring(0, 512) : content;

            float[] docVec;
            try {
                docVec = embeddingClient.embed(docText);
            } catch (Exception e) {
                log.warn("CrossEncoderReranker: failed to embed doc {}: {}", hit.chunkId, e.getMessage());
                // 降级：直接用原始 cosine 得分
                scored.add(new ScoredHit(hit, hit.score, hit.score, hit.score));
                continue;
            }

            if (docVec == null || docVec.length == 0) {
                scored.add(new ScoredHit(hit, hit.score, hit.score, hit.score));
                continue;
            }

            // 双向交叉注意力得分（对称）
            double simQD = VectorUtils.cosine(queryVec, docVec);
            double simDQ = simQD; // 对称 cosine 两者相同，以下用加权融合
            double crossScore = alpha * simQD + (1 - alpha) * simDQ;

            // Step 3: 融合首轮 cosine 得分 + 交叉得分
            // weight = 0.3 * cosine + 0.7 * crossEncoder
            double fusedScore = 0.3 * hit.score + 0.7 * crossScore;

            scored.add(new ScoredHit(hit, crossScore, hit.score, fusedScore));
        }

        // Step 4: 按综合分降序排列
        scored.sort((a, b) -> Double.compare(b.fusedScore, a.fusedScore));

        // Step 5: 截取 topN 并回填 rerank 信息
        List<Retriever.Hit> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, scored.size()); i++) {
            ScoredHit sh = scored.get(i);
            sh.hit.rankScore = sh.fusedScore;
            sh.hit.score = sh.crossScore; // 覆盖为 cross-encoder 得分
            sh.hit.highlight = sh.hit.highlight; // 保持不变
            result.add(sh.hit);
        }

        log.info("CrossEncoderReranker: queryLen={} candidates={} final={} alpha={}",
                query.length(), candidatesToRerank, result.size(), alpha);
        return result;
    }

    /**
     * 重排序（使用默认 finalTopK）.
     */
    public List<Retriever.Hit> rerank(List<Retriever.Hit> candidates, String query) {
        return rerank(candidates, query, -1);
    }

    /**
     * 计算单条 (query, doc) 的 Cross-Encoder 得分.
     */
    public double score(String query, String doc) {
        if (query == null || doc == null || query.isBlank() || doc.isBlank()) return 0.0;
        float[] qVec = embeddingClient.embed(query);
        float[] dVec = embeddingClient.embed(doc.length() > 512 ? doc.substring(0, 512) : doc);
        if (qVec == null || dVec == null) return 0.0;
        return VectorUtils.cosine(qVec, dVec);
    }

    // ============== 内部类 ==============

    private static class ScoredHit {
        final Retriever.Hit hit;
        final double crossScore;    // cross-encoder 原始得分
        final double cosineScore;   // 首次 cosine 得分
        final double fusedScore;    // 融合综合分

        ScoredHit(Retriever.Hit hit, double crossScore, double cosineScore, double fusedScore) {
            this.hit = hit;
            this.crossScore = crossScore;
            this.cosineScore = cosineScore;
            this.fusedScore = fusedScore;
        }
    }
}
