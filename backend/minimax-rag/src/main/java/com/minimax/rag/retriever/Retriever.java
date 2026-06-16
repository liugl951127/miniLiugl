package com.minimax.rag.retriever;

import com.minimax.rag.embedding.EmbeddingClient;
import com.minimax.rag.entity.Document;
import com.minimax.rag.entity.DocumentChunk;
import com.minimax.rag.mapper.DocumentChunkMapper;
import com.minimax.rag.mapper.DocumentMapper;
import com.minimax.rag.service.VectorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 向量检索器。
 *
 * 流程:
 *  1) query → embedding
 *  2) 拉 KB 内全部 chunks (含向量)
 *  3) 计算 cosine 相似度
 *  4) 过滤 + 排序 → topK
 *  5) 回填 doc 标题 + touchAccess
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Retriever {

    private final DocumentChunkMapper chunkMapper;
    private final DocumentMapper docMapper;
    private final EmbeddingClient embedding;

    @Value("${minimax.rag.retrieve.min-score:0.10}")
    private double minScore;

    /**
     * @param kbId     限定 KB (null = 全公开 KB)
     * @param query    用户问题
     * @param topK     返回数量
     */
    public List<Hit> retrieve(Long kbId, String query, int topK) {
        if (query == null || query.isBlank()) return List.of();
        if (topK <= 0) topK = 5;
        if (topK > 50) topK = 50;
        if (kbId == null) {
            log.warn("retrieve: kbId is null, return empty (全库搜索待实现)");
            return List.of();
        }
        float[] qVec = embedding.embed(query);
        List<DocumentChunk> all = chunkMapper.selectEmbeddingsByKb(kbId, 5000);
        if (all.isEmpty()) return List.of();

        List<Hit> hits = new ArrayList<>(all.size());
        for (DocumentChunk c : all) {
            float[] v = VectorUtils.fromBytes(c.getEmbedding());
            double sim = VectorUtils.cosine(qVec, v);
            if (sim >= minScore) {
                hits.add(new Hit(c.getId(), c.getDocId(), c.getKbId(), c.getChunkIndex(),
                        c.getContent(), c.getStartPos(), c.getEndPos(), sim));
            }
        }
        hits.sort((a, b) -> Double.compare(b.score, a.score));
        List<Hit> top = hits.subList(0, Math.min(topK, hits.size()));

        // touch access + 拉 doc title
        for (Hit h : top) {
            chunkMapper.touchAccess(h.chunkId);
            Document doc = docMapper.selectById(h.docId);
            h.docTitle = doc == null ? null : doc.getTitle();
            h.docSource = doc == null ? null : doc.getSourceUri();
        }
        log.info("retrieve: kbId={} queryLen={} candidates={} hits={} topK={}",
                kbId, query.length(), all.size(), hits.size(), top.size());
        return top;
    }

    public static class Hit {
        public Long chunkId;
        public Long docId;
        public Long kbId;
        public Integer chunkIndex;
        public String content;
        public Integer startPos;
        public Integer endPos;
        public Double score;
        public String docTitle;   // 召回时填充
        public String docSource;

        public Hit(Long chunkId, Long docId, Long kbId, Integer chunkIndex,
                   String content, Integer startPos, Integer endPos, Double score) {
            this.chunkId = chunkId;
            this.docId = docId;
            this.kbId = kbId;
            this.chunkIndex = chunkIndex;
            this.content = content;
            this.startPos = startPos;
            this.endPos = endPos;
            this.score = score;
        }
    }
}
