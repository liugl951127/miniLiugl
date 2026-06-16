package com.minimax.memory.longterm;

import com.minimax.memory.embedding.EmbeddingClient;
import com.minimax.memory.embedding.MockEmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class LongTermMemoryService {

    private final LongTermMemoryMapper mapper;
    private final EmbeddingClient embedding;

    @Value("${minimax.memory.recall.top-k:5}")
    private int defaultTopK;

    @Value("${minimax.memory.recall.min-score:0.55}")
    private double minScore;

    public LongTermMemoryService(LongTermMemoryMapper mapper,
                                  @org.springframework.beans.factory.annotation.Qualifier("mockEmbeddingClient")
                                  EmbeddingClient embedding) {
        this.mapper = mapper;
        this.embedding = embedding;
    }

    /**
     * 存入一条长期记忆。
     * @return 新记录 id
     */
    public Long store(Long userId, Long sessionId, String role, String content,
                      String summary, String tags, Double importance) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content 不能为空");
        }
        String text = (summary != null && !summary.isBlank()) ? summary : content;
        float[] vec = embedding.embed(text);
        LongTermMemory m = new LongTermMemory();
        m.setUserId(userId);
        m.setSessionId(sessionId);
        m.setContent(content);
        m.setSummary(summary);
        m.setRole(role != null ? role : "user");
        m.setEmbedding(VectorUtils.toBytes(vec));
        m.setDim(vec.length);
        m.setImportance(importance != null ? new java.math.BigDecimal(importance)
                : new java.math.BigDecimal("0.50"));
        m.setTags(tags);
        mapper.insert(m);
        log.info("长期记忆入库: userId={} id={} dim={} contentLen={}",
                userId, m.getId(), vec.length, content.length());
        return m.getId();
    }

    /**
     * 跨会话召回：给一个 query，召回 top-k 最相关的记忆。
     */
    public List<RecallHit> recall(Long userId, String query, int topK) {
        if (query == null || query.isBlank()) return List.of();
        if (topK <= 0) topK = defaultTopK;
        if (topK > 50) topK = 50;

        float[] qVec = embedding.embed(query);
        // 拉用户所有 embedding（生产应分批 / 异步建索引）
        List<LongTermMemory> all = mapper.selectEmbeddingsByUser(userId, 5000);
        if (all.isEmpty()) return List.of();

        // 计算相似度，排序
        List<RecallHit> hits = new ArrayList<>(all.size());
        for (LongTermMemory m : all) {
            float[] v = VectorUtils.fromBytes(m.getEmbedding());
            double sim = VectorUtils.cosine(qVec, v);
            if (sim >= minScore) {
                hits.add(new RecallHit(m.getId(), m.getContent(), m.getSummary(),
                        m.getRole(), m.getSessionId(), m.getCreatedAt(),
                        m.getImportance(), sim));
            }
        }
        hits.sort((a, b) -> Double.compare(b.score, a.score));
        List<RecallHit> top = hits.subList(0, Math.min(topK, hits.size()));

        // 命中后异步 touch (这里简化同步)
        for (RecallHit h : top) mapper.touchAccess(h.id);

        log.info("召回: userId={} query='{}' candidates={} hits={} returned={}",
                userId, query.length() > 30 ? query.substring(0, 30) + "..." : query,
                all.size(), hits.size(), top.size());
        return top;
    }

    /** 列出最近记忆（无向量）。 */
    public List<LongTermMemory> recent(Long userId, int limit) {
        if (limit <= 0 || limit > 200) limit = 20;
        return mapper.selectRecentByUser(userId, limit);
    }

    public boolean delete(Long id, Long userId) {
        LongTermMemory m = mapper.selectById(id);
        if (m == null) return false;
        if (!m.getUserId().equals(userId)) return false;
        mapper.deleteById(id);
        return true;
    }

    public record RecallHit(
            Long id, String content, String summary, String role,
            Long sessionId, java.time.LocalDateTime createdAt,
            java.math.BigDecimal importance, double score) {}
}
