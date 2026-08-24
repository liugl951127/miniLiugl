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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Value("${minimax.rag.retrieve.timeliness-boost:0.10}")
    private double timelinessBoost;

    @Value("${minimax.rag.retrieve.max-age-days:365}")
    private int maxAgeDays;

    /**
     * @param kbId     限定 KB (null = 全公开 KB)
     * @param query    用户问题
     * @param topK     返回数量
     * @param useTimeliness 是否启用时效性加权排序，默认 true
     */
    public List<Hit> retrieve(Long kbId, String query, int topK, boolean useTimeliness) {
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

        // 批量拉取文档时间戳，用于时效性加权 (Day 51)
        Map<Long, LocalDateTime> docUpdatedMap = new HashMap<>();
        if (useTimeliness && timelinessBoost > 0) {
            Set<Long> docIds = hits.stream().map(h -> h.docId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
            if (!docIds.isEmpty()) {
                docMapper.selectBatchIds(docIds).forEach(d -> docUpdatedMap.put(d.getId(), d.getUpdatedAt()));
            }
        }

        // 排序：相关性 + 时效性加权 (Day 51)
        final LocalDateTime now = LocalDateTime.now();
        final double boost = timelinessBoost;
        final int maxAge = maxAgeDays;
        hits.forEach(h -> {
            if (useTimeliness && boost > 0) {
                LocalDateTime ta = docUpdatedMap.getOrDefault(h.docId, LocalDateTime.now().minusDays(maxAge + 1));
                double rec = recencyScore(ta, now, maxAge);
                h.rankScore = (1 - boost) * h.score + boost * rec;
            } else {
                h.rankScore = h.score;
            }
        });
        hits.sort((a, b) -> Double.compare(b.rankScore, a.rankScore));

        List<Hit> top = hits.subList(0, Math.min(topK, hits.size()));

        // touch access + 拉 doc title + 高亮摘要 (Day 43)
        Set<Long> topDocIds = top.stream().map(h -> h.docId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Document> docMap = topDocIds.isEmpty() ? Map.of() :
                docMapper.selectBatchIds(topDocIds).stream()
                        .collect(Collectors.toMap(Document::getId, d -> d, (a, b) -> a));
        for (Hit h : top) {
            chunkMapper.touchAccess(h.chunkId);
            Document doc = docMap.get(h.docId);
            h.docTitle = doc == null ? null : doc.getTitle();
            h.docSource = doc == null ? null : doc.getSourceUri();
            h.setHighlight(query);
        }
        log.info("retrieve: kbId={} queryLen={} candidates={} hits={} topK={} timeliness={}",
                kbId, query.length(), all.size(), hits.size(), top.size(), useTimeliness);
        return top;
    }

    /**
     * 旧版兼容：默认启用时效性加权 (Day 51).
     */
    public List<Hit> retrieve(Long kbId, String query, int topK) {
        return retrieve(kbId, query, topK, true);
    }

    /**
     * Day 53: 跨知识库联合检索.
     * 在多个 KB 中并行检索，合并去重，按综合分排序返回 topK.
     *
     * @param kbIds         知识库 ID 列表（不可为空或空列表）
     * @param query         用户查询
     * @param topK          返回数量（每个 KB 取 topK*2 的候选，合并后取 topK）
     * @param useTimeliness 是否启用时效性加权
     * @return 合并排序后的命中结果，含 kbName 字段
     */
    public List<Hit> retrieveMultiKb(List<Long> kbIds, String query, int topK, boolean useTimeliness) {
        if (kbIds == null || kbIds.isEmpty()) {
            log.warn("retrieveMultiKb: kbIds is empty");
            return List.of();
        }
        if (query == null || query.isBlank()) return List.of();
        if (topK <= 0) topK = 5;
        if (topK > 50) topK = 50;

        // 并行检索每个 KB（候选量放大以确保合并后仍有足够可选）
        List<Hit> allHits = new ArrayList<>();
        for (Long kbId : kbIds) {
            try {
                // 每个 KB 多取一些候选，防止某个 KB 大量命中而其他 KB 少的情况
                List<Hit> hits = retrieve(kbId, query, topK * 2, useTimeliness);
                allHits.addAll(hits);
            } catch (Exception e) {
                log.warn("retrieveMultiKb: kbId={} 检索失败: {}", kbId, e.getMessage());
            }
        }

        if (allHits.isEmpty()) return List.of();

        // 按综合分排序（已含时效性加权）
        allHits.sort((a, b) -> Double.compare(b.rankScore, a.rankScore));

        // 去重：同一 docId 的 chunk 只保留最高分那个
        Map<Long, Hit> deduped = new HashMap<>();
        for (Hit h : allHits) {
            if (!deduped.containsKey(h.chunkId) || deduped.get(h.chunkId).rankScore < h.rankScore) {
                deduped.put(h.chunkId, h);
            }
        }

        // KB 间均衡：每个 KB 至少保留 N 个（如果总数足够）
        int minPerKb = Math.max(1, topK / Math.max(kbIds.size(), 1));
        Map<Long, List<Hit>> byKb = new HashMap<>();
        for (Hit h : deduped.values()) {
            byKb.computeIfAbsent(h.kbId, k -> new ArrayList<>()).add(h);
        }

        List<Hit> balanced = new ArrayList<>();
        for (List<Hit> kbHits : byKb.values()) {
            kbHits.sort((a, b) -> Double.compare(b.rankScore, a.rankScore));
            balanced.addAll(kbHits.subList(0, Math.min(minPerKb, kbHits.size())));
        }
        // 补齐剩余配额（按全局分数）
        Set<Long> already = balanced.stream().map(h -> h.chunkId).collect(Collectors.toSet());
        for (Hit h : deduped.values()) {
            if (!already.contains(h.chunkId)) balanced.add(h);
            if (balanced.size() >= topK) break;
        }

        // 最终 topK
        List<Hit> result = balanced.subList(0, Math.min(topK, balanced.size()));
        log.info("retrieveMultiKb: kbIds={} queryLen={} candidates={} deduped={} final={}",
                kbIds, query.length(), allHits.size(), deduped.size(), result.size());
        return result;
    }

    /**
     * 跨 KB 检索（默认启用时效性加权）.
     */
    public List<Hit> retrieveMultiKb(List<Long> kbIds, String query, int topK) {
        return retrieveMultiKb(kbIds, query, topK, true);
    }

    /**
     * 计算时效性得分 (0~1)，越新的文档分数越高.
     * 使用指数衰减：score = exp(-days / halfLife)，halfLife = maxAge / 3
     */
    private double recencyScore(LocalDateTime updatedAt, LocalDateTime now, int maxAge) {
        if (updatedAt == null) return 0.0;
        long days = Math.abs(ChronoUnit.DAYS.between(updatedAt, now));
        if (days > maxAge) return 0.0;
        // 半衰期 = maxAge / 3，越接近半衰期分数衰减到 ~0.5
        double halfLife = maxAge / 3.0;
        return Math.exp(-days / halfLife);
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
        /** Day 51: 排序综合分 (相关性 + 时效性加权)，未启用加权时等于 score */
        public Double rankScore;
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
            this.rankScore = score;
        }

        /** Day 43: 根据 query 生成高亮摘要 */
        public void setHighlight(String query) {
            this.highlight = highlight(this.content, query, 120);
        }
    }
}
