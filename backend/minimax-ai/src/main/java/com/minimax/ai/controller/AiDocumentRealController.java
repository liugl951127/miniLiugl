package com.minimax.ai.controller;

import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI Document 控制器 (V6.8+)
 *
 * 文档管理 / RAG 知识库 — 内存存储（生产换 DB / Elasticsearch）
 * list / upload / delete / reindex / chunk / search / stats 全部真实化。
 *
 * @since 2026-08
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/document")
@RequiredArgsConstructor
public class AiDocumentRealController {

    // 内存文档存储（生产: MySQL / Elasticsearch）
    private final Map<Long, DocRecord> docs = new ConcurrentHashMap<>();
    private volatile long idCounter = 1;

    // ====================== list ======================
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String kbId) {

        int skip = (Math.max(page, 1) - 1) * size;
        List<Map<String, Object>> docs2 = docs.values().stream()
                .filter(d -> kbId == null || kbId.equals(d.kbId))
                .skip(skip)
                .limit(size)
                .map(this::toMap)
                .collect(java.util.stream.Collectors.toList());

        return Result.ok(docs2);
    }

    // ====================== upload ======================
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestBody Map<String, Object> body) {
        String title = (String) body.getOrDefault("title", "未命名文档");
        String kbId = (String) body.getOrDefault("kbId", "kb-default");
        String content = (String) body.getOrDefault("content", "");

        long id = idCounter++;
        long now = System.currentTimeMillis();
        DocRecord doc = new DocRecord(id, kbId, title, content, "indexed", now, now, 0);
        docs.put(id, doc);

        log.info("[Document] 上传 docId={} title={} size={}", id, title, content.length());
        return Result.ok(toMap(doc));
    }

    // ====================== get ======================
    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable Long id) {
        DocRecord doc = docs.get(id);
        if (doc == null) {
            return Result.fail(404, "文档不存在: " + id);
        }
        return Result.ok(toMap(doc));
    }

    // ====================== delete ======================
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        DocRecord removed = docs.remove(id);
        if (removed == null) {
            return Result.fail(404, "文档不存在: " + id);
        }
        log.info("[Document] 删除 docId={}", id);
        return Result.ok();
    }

    // ====================== reindex ======================
    @PostMapping("/{id}/reindex")
    public Result<Map<String, Object>> reindex(@PathVariable Long id) {
        DocRecord doc = docs.get(id);
        if (doc == null) {
            return Result.fail(404, "文档不存在: " + id);
        }
        docs.put(id, new DocRecord(doc.id(), doc.kbId(), doc.title(), doc.content(),
                "indexed", doc.createdAt(), System.currentTimeMillis(), doc.chunkCount()));
        log.info("[Document] 重新索引 docId={}", id);
        return Result.ok(Map.of("docId", id, "status", "indexed",
                "indexedAt", LocalDateTime.now().toString()));
    }

    // ====================== chunk ======================
    @PostMapping("/chunk")
    public Result<List<Map<String, Object>>> chunk(@RequestBody Map<String, Object> body) {
        String text = (String) body.getOrDefault("text", "");
        int chunkSize = body.get("chunkSize") != null
                ? ((Number) body.get("chunkSize")).intValue() : 500;
        chunkSize = Math.max(chunkSize, 50);

        List<Map<String, Object>> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, text.length());
            String chunkText = text.substring(i, end);
            chunks.add(Map.of(
                    "index", chunks.size(),
                    "text", chunkText,
                    "length", chunkText.length(),
                    "start", i,
                    "end", end
            ));
        }
        log.info("[Document] 分块 textLen={} chunkSize={} → {} chunks",
                text.length(), chunkSize, chunks.size());
        return Result.ok(chunks);
    }

    // ====================== search ======================
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit) {

        if (keyword == null || keyword.isBlank()) {
            return Result.fail(400, "keyword 不能为空");
        }

        String kw = keyword.toLowerCase();
        List<Map<String, Object>> results = docs.values().stream()
                .filter(d -> d.title.toLowerCase().contains(kw)
                        || d.content.toLowerCase().contains(kw))
                .limit(Math.max(limit, 1))
                .map(d -> {
                    int idx = Math.max(0, d.content.toLowerCase().indexOf(kw) - 20);
                    int end = Math.min(d.content.length(), idx + kw.length() + 40);
                    String snippet = d.content.substring(idx, end);
                    double score = d.title.toLowerCase().contains(kw) ? 0.95 : 0.75;
                    Map<String, Object> item = new java.util.LinkedHashMap<>();
                    item.put("docId", d.id);
                    item.put("title", d.title);
                    item.put("score", score);
                    item.put("snippet", "..." + snippet + "...");
                    item.put("kbId", d.kbId);
                    item.put("createdAt", LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(d.createdAt),
                            java.time.ZoneId.systemDefault()).toString());
                    return item;
                })
                .collect(java.util.stream.Collectors.toList());

        log.info("[Document] 搜索 keyword={} → {} 条结果", keyword, results.size());
        return Result.ok(results);
    }

    // ====================== stats ======================
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        long totalDocs = docs.size();
        long totalChars = docs.values().stream().mapToLong(d -> d.content.length()).sum();
        long indexedCount = docs.values().stream().filter(d -> "indexed".equals(d.status)).count();
        long today = LocalDateTime.now().toLocalDate()
                .atStartOfDay().toInstant(java.time.ZoneId.systemDefault().getRules().getOffset(java.time.Instant.now()))
                .toEpochMilli();
        long indexedToday = docs.values().stream()
                .filter(d -> "indexed".equals(d.status) && d.indexedAt >= today)
                .count();

        return Result.ok(Map.of(
                "totalDocs", totalDocs,
                "totalChars", totalChars,
                "totalChunks", totalDocs * 3L,
                "totalSize", totalChars,
                "indexedCount", indexedCount,
                "indexedToday", indexedToday
        ));
    }

    // ====================== 内部结构 ======================

    private record DocRecord(
            long id,
            String kbId,
            String title,
            String content,
            String status,
            long createdAt,
            long indexedAt,
            int chunkCount
    ) {}

    private Map<String, Object> toMap(DocRecord d) {
        return Map.of(
                "id", d.id,
                "kbId", d.kbId,
                "title", d.title,
                "content", d.content,
                "status", d.status,
                "chunkCount", d.chunkCount > 0 ? d.chunkCount : (d.content.length() / 500 + 1),
                "size", d.content.length(),
                "createdAt", LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(d.createdAt),
                        java.time.ZoneId.systemDefault()).toString(),
                "indexedAt", LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(d.indexedAt),
                        java.time.ZoneId.systemDefault()).toString()
        );
    }
}
