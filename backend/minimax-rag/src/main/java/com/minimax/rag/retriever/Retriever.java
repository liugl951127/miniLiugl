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

        // touch access + 拉 doc title + 高亮摘要 (Day 43)
        for (Hit h : top) {
            chunkMapper.touchAccess(h.chunkId);
            Document doc = docMapper.selectById(h.docId);
            h.docTitle = doc == null ? null : doc.getTitle();
            h.docSource = doc == null ? null : doc.getSourceUri();
            h.setHighlight(query);
        }
        log.info("retrieve: kbId={} queryLen={} candidates={} hits={} topK={}",
                kbId, query.length(), all.size(), hits.size(), top.size());
        return top;
    }

    /**
     * 为检索结果生成高亮摘要 (Day 43).
     * 将 query 中的关键词在 content 中用 &lt;mark&gt; 标签包裹.
     *
     * @param query  用户查询词
     * @param window 高亮片段前后各取多少字符，默认 120
     * @return 含 &lt;mark&gt; 标签的高亮片段，关键词在中间
     */
    public static String highlight(String content, String query, int window) {
        if (content == null || content.isBlank()) return "";
        if (query == null || query.isBlank()) {
            return content.length() > window * 2 ? content.substring(0, window * 2) + "..." : content;
        }
        // 提取关键词（中英文 + 数字，分词）
        String[] terms = query.trim().split("\\s+");
        StringBuilder pattern = new StringBuilder();
        for (int i = 0; i < terms.length; i++) {
            if (i > 0) pattern.append("|");
            // 转义特殊字符
            pattern.append(terms[i].replaceAll("([\\[\\](){}.*+?^$|\\\\])", "\\\\$1"));
        }
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                pattern.toString(), java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(content);
        if (!m.find()) {
            // 没有匹配，全文截取
            return content.length() > window * 2 ? content.substring(0, window * 2) + "..." : content;
        }
        m.reset();
        // 找到所有匹配位置，取第一个关键词匹配的起始位置附近
        int firstMatchStart = m.start();
        int start = Math.max(0, firstMatchStart - window);
        int end = Math.min(content.length(), firstMatchStart + window);
        String snippet = content.substring(start, end);
        if (start > 0) snippet = "..." + snippet;
        if (end < content.length()) snippet = snippet + "...";
        // 标记关键词
        return snippet.replaceAll(
                "(" + pattern + ")",
                "<mark>$1</mark>");
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
        public String highlight;  // Day 43: 高亮摘要片段

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

        /** Day 43: 根据 query 生成高亮摘要 */
        public void setHighlight(String query) {
            this.highlight = highlight(this.content, query, 120);
        }
    }
}
