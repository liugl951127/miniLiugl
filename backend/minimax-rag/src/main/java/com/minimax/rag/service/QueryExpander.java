package com.minimax.rag.service;

import com.minimax.rag.retriever.Retriever;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * RAG 查询重写/扩展服务 (Day 30).
 *
 * <p>原始查询往往简短、模糊，直接检索效果差。
 * QueryExpander 通过 MiniMax-Text-03 对查询进行"展开"，
 * 生成多个语义等价但表述不同的查询句，再并发检索取最优。
 *
 * <h3>Query Expansion 流程</h3>
 * <pre>
 * 原始查询 → LLM 生成 N 个展开查询
 *         → 并发向量检索
 *         → 去重合并 topK hits
 *         → 返回扩展后的检索结果
 * </pre>
 *
 * <h3>展开策略</h3>
 * <ul>
 *   <li><b>SYNTACTIC</b>: 同义词替换 / 句式变换（快，无 LLM 调用）</li>
 *   <li><b>SEMANTIC_LLM</b>: MiniMax-Text-03 生成多个语义等价表述（推荐）</li>
 *   <li><b>HYBRID</b>: 先 SYNTACTIC，再用 LLM 补充（最全面但最慢）</b></li>
 * </ul>
 *
 * <p>配置项:
 * <pre>
 * minimax.rag.query-expansion.enabled=true
 * minimax.rag.query-expansion.strategy=SEMANTIC_LLM
 * minimax.rag.query-expansion.expanded-count=3
 * minimax.rag.query-expansion.model=MiniMax-Text-03
 * minimax.rag.query-expansion.dedup-similarity=0.85
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryExpander {

    /** 用于语义展开的 LLM */
    @Value("${minimax.rag.query-expansion.model:MiniMax-Text-03}")
    private String expansionModel;

    @Value("${minimax.rag.query-expansion.expanded-count:3}")
    private int expandedCount;

    @Value("${minimax.rag.query-expansion.strategy:SEMANTIC_LLM}")
    private ExpansionStrategy strategy;

    @Value("${minimax.rag.query-expansion.enabled:true}")
    private boolean enabled;

    @Value("${minimax.rag.chat.base-url:http://localhost:8083}")
    private String modelBaseUrl;

    @Value("${minimax.rag.chat.token:}")
    private String token;

    private final Retriever retriever;
    private final com.minimax.common.sdk.LlmClient llmClient;  // V9.1: LLM 兜底

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final RestTemplate restTemplate = new RestTemplate();

    // ============== 公开 API ==============

    /**
     * 判断是否启用查询扩展。
     */
    public boolean isEnabled() { return enabled; }

    /**
     * 获取当前策略。
     */
    public ExpansionStrategy getStrategy() { return strategy; }

    /**
     * 扩展检索：对原始查询做展开后并发检索。
     *
     * @param kbId   知识库 ID
     * @param query  原始用户查询
     * @param topK   返回总数（展开的各查询共享）
     * @return 扩展检索结果
     */
    public ExpansionResult expandRetrieve(Long kbId, String query, int topK) {
        if (!enabled || query == null || query.isBlank()) {
            return simpleRetrieve(kbId, query, topK);
        }

        long start = System.currentTimeMillis();
        List<String> expandedQueries = expand(query);

        log.info("[QueryExpander] '{}' → {} expansions: {}",
                query.length() > 50 ? query.substring(0, 50) : query,
                expandedQueries.size(),
                expandedQueries.stream()
                        .map(q -> q.length() > 30 ? q.substring(0, 30) : q)
                        .toList());

        // 并发检索
        List<HitWithQuery> allHits = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String q : expandedQueries) {
            CompletableFuture<Void> f = CompletableFuture.runAsync(() -> {
                List<Retriever.Hit> hits = retriever.retrieve(kbId, q, topK);
                synchronized (allHits) {
                    for (Retriever.Hit h : hits) {
                        allHits.add(new HitWithQuery(h, q));
                    }
                }
            });
            futures.add(f);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 去重合并（按 chunkId 合并，保留最高分）
        Map<Long, HitWithQuery> deduped = new LinkedHashMap<>();
        for (HitWithQuery h : allHits) {
            HitWithQuery existing = deduped.get(h.hit.chunkId);
            if (existing == null || h.hit.score > existing.hit.score) {
                deduped.put(h.hit.chunkId, h);
            }
        }

        // 取 topK
        List<Retriever.Hit> topHits = deduped.values().stream()
                .map(hw -> hw.hit)
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(topK)
                .toList();

        long elapsed = System.currentTimeMillis() - start;
        log.info("[QueryExpander] {} hits from {} queries ({}ms) → {} final hits",
                allHits.size(), expandedQueries.size(), elapsed, topHits.size());

        return new ExpansionResult(query, expandedQueries, topHits, elapsed,
                strategy.name());
    }

    /** 直接检索（扩展被禁用或查询为空时） */
    private ExpansionResult simpleRetrieve(Long kbId, String query, int topK) {
        List<Retriever.Hit> hits = retriever.retrieve(kbId, query, topK);
        return new ExpansionResult(query, List.of(query), hits, 0, "NONE");
    }

    // ============== 展开算法 ==============

    /**
     * 根据策略生成展开查询列表。
     */
    public List<String> expand(String query) {
        if (query == null || query.isBlank()) return List.of();

        return switch (strategy) {
            case SYNTACTIC -> syntacticExpand(query);
            case SEMANTIC_LLM -> semanticLlmExpand(query);
            case HYBRID -> {
                List<String> syn = syntacticExpand(query);
                List<String> llm = semanticLlmExpand(query);
                // 去重合并
                Set<String> seen = new HashSet<>();
                List<String> combined = new ArrayList<>();
                for (String q : syn) {
                    String norm = q.strip().toLowerCase();
                    if (seen.add(norm)) combined.add(q);
                }
                for (String q : llm) {
                    String norm = q.strip().toLowerCase();
                    if (seen.add(norm)) combined.add(q);
                }
                yield combined;
            }
        };
    }

    /**
     * 基于规则的句法展开。
     */
    private List<String> syntacticExpand(String query) {
        List<String> out = new ArrayList<>();
        out.add(query);

        // 同义词映射
        Map<String, List<String>> synonyms = Map.ofEntries(
                Map.entry("查询", List.of("检索", "搜索", "查找")),
                Map.entry("如何", List.of("怎么", "怎样", "如何做")),
                Map.entry("配置", List.of("设置", "安装", "部署")),
                Map.entry("使用", List.of("应用", "使用", "怎么用")),
                Map.entry("问题", List.of("报错", "故障", "异常")),
                Map.entry("API", List.of("接口", "API", "调用方式")),
                Map.entry("部署", List.of("安装", "启动", "配置")),
                Map.entry("密码", List.of("密钥", "token", "凭证")),
                Map.entry("错误", List.of("异常", "失败", "问题"))
        );

        String q = query;
        for (Map.Entry<String, List<String>> e : synonyms.entrySet()) {
            if (q.contains(e.getKey())) {
                for (String syn : e.getValue()) {
                    out.add(q.replace(e.getKey(), syn));
                    out.add(q.replace(e.getKey(), syn) + "？");
                }
            }
        }

        // 中英混合扩展
        if (q.contains("SQL")) out.add(q.replace("SQL", "数据库查询"));
        if (q.contains("JWT")) out.add(q.replace("JWT", "身份验证 token"));
        if (q.contains("Docker")) out.add(q.replace("Docker", "容器化"));
        if (q.contains("Redis")) out.add(q.replace("Redis", "缓存"));

        // 疑问词扩展
        if (!q.contains("？") && !q.contains("?")) {
            out.add(q + "？");
            out.add(q + "怎么做");
            out.add("关于" + q);
        }

        // 去重
        Set<String> seen = new HashSet<>();
        List<String> unique = new ArrayList<>();
        for (String v : out) {
            String norm = v.strip().toLowerCase();
            if (seen.add(norm)) unique.add(v);
        }
        return unique;
    }

    /**
     * 语义 LLM 展开：调 MiniMax-Text-03 生成多个不同表述。
     */
    private List<String> semanticLlmExpand(String query) {
        String prompt = String.format("""
                请为以下用户查询生成 %d 个不同的表述方式。
                要求：语义等价但措辞不同（如同义词替换、主动/被动转换、口语/书面语等）。
                直接输出，每行一个，不要编号，不要解释。

                查询：%s

                输出：
                """, expandedCount, query);

        try {
            Map<String, Object> body = Map.of(
                    "model", expansionModel,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "max_tokens", 300,
                    "temperature", 0.8
            );

            // V9.1: 优先调 minimax-model, 失败时降级到 LLM Gateway (cloud→local 兜底)
            String text = null;
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> resp = restTemplate.postForObject(
                        modelBaseUrl + "/models/chat", body, Map.class);
                text = extractContent(resp);
            } catch (Exception modelErr) {
                log.warn("[QueryExpander] minimax-model 失败, 降级到 LLM Gateway: {}", modelErr.getMessage());
                com.minimax.common.sdk.LlmClient.LlmResult fallback = llmClient.chat(
                    List.of(Map.of("role", "user", "content", prompt)));
                if (fallback.available()) {
                    text = fallback.content();
                    log.info("[QueryExpander] 降级成功, source={}", fallback.source());
                }
            }
            if (text == null || text.isBlank()) return List.of(query);

            // 解析每行作为展开查询
            return Arrays.stream(text.split("\n"))
                    .map(String::strip)
                    .filter(l -> !l.isEmpty() && l.length() > 3)
                    .limit(expandedCount)
                    .toList();

        } catch (Exception e) {
            log.warn("[QueryExpander] LLM expand failed: {}, fallback to original", e.getMessage());
            return List.of(query);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> resp) {
        if (resp == null) return null;
        Object content = resp.get("content");
        if (content instanceof String) return (String) content;
        if (content instanceof Map) return (String) ((Map<String, Object>) content).get("text");
        return String.valueOf(content);
    }

    // ============== 结果类 ==============

    @Getter
    public static class ExpansionResult {
        /** 原始查询 */
        private final String originalQuery;
        /** 展开后的查询列表 */
        private final List<String> expandedQueries;
        /** 最终检索到的 top hits */
        private final List<Retriever.Hit> hits;
        /** 耗时 ms */
        private final long elapsedMs;
        /** 使用的策略 */
        private final String strategy;

        public ExpansionResult(String originalQuery, List<String> expandedQueries,
                              List<Retriever.Hit> hits, long elapsedMs, String strategy) {
            this.originalQuery = originalQuery;
            this.expandedQueries = List.copyOf(expandedQueries);
            this.hits = List.copyOf(hits);
            this.elapsedMs = elapsedMs;
            this.strategy = strategy;
        }

        public boolean isExpanded() { return !strategy.equals("NONE"); }
    }

    /** Hit + 来源的展开查询 */
    public record HitWithQuery(Retriever.Hit hit, String fromQuery) {}

    public enum ExpansionStrategy {
        SYNTACTIC,    // 纯规则展开
        SEMANTIC_LLM, // LLM 语义展开（默认）
        HYBRID        // 混合
    }
}
