package com.minimax.rag.controller;

import com.minimax.common.result.Result;
import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
import com.minimax.rag.entity.Document;
import com.minimax.rag.entity.DocumentChunk;
import com.minimax.rag.entity.KnowledgeBase;
import com.minimax.rag.retriever.Retriever;
import com.minimax.rag.reranker.CrossEncoderReranker;
import com.minimax.rag.service.DocumentService;
import com.minimax.rag.service.KnowledgeBaseService;
import com.minimax.rag.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * RAG 控制器 (V6.8.2 安全修复: ownerId 必须来自 JWT 身份验证).
 *
 * 知识库 (KB):
 *   POST   /rag/kb                          建库
 *   GET    /rag/kb                          列出我的
 *   GET    /rag/kb/public                   列出公开的
 *   GET    /rag/kb/{id}                     详情
 *   PUT    /rag/kb/{id}                     更新知识库
 *   DELETE /rag/kb/{id}                     删除
 *
 * 文档 (Document):
 *   POST   /rag/doc/upload?kbId=1           上传 (multipart file)
 *   GET    /rag/doc?kbId=1                  列出
 *   GET    /rag/doc/{id}/chunks             切片列表
 *   PUT    /rag/doc/{id}                    重命名文档
 *   PUT    /rag/doc/{id}/content            在线编辑（修改内容+重新切片+重新索引）Day 45
 *   POST   /rag/doc/batch/reindex          批量重新索引（批量切片+批量向量化）Day 46
 *   DELETE /rag/doc/batch                  批量删除（批量删除切片+文档）Day 47
 *   DELETE /rag/doc/{id}                    删除
 *   GET    /rag/doc/{id}/content            文档全文内容
 *
 * 检索 + 问答:
 *   POST   /rag/retrieve                    纯检索 (返回 topK chunks)
 *   POST   /rag/ask                         RAG 问答 (检索+LLM+引用，支持 systemPrompt)
 */
@Tag(name = "RAG知识库")
@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
@Slf4j
public class RagController {

    private final KnowledgeBaseService kbService;
    private final DocumentService docService;
    private final Retriever retriever;
    private final CrossEncoderReranker reranker;
    private final RagService ragService;

    /**
     * V6.8.2 安全修复: 校验 ownerId 来自 JWT 身份而非请求参数.
     * 防止 IDOR: 用户不能操作他人的资源.
     */
    private Long resolveOwnerId(@AuthenticationPrincipal AuthenticatedUser user, Long requestedOwnerId) {
        if (user == null) {
            throw new SecurityException("需要登录");
        }
        if (requestedOwnerId != null && !requestedOwnerId.equals(user.id())) {
            throw new SecurityException("无权操作此资源 (ownerId 不匹配当前用户)");
        }
        return user.id();
    }

    // ---------- KB ----------

    @Operation(summary = "创建知识库")
    @PostMapping("/kb")
    public Result<Long> createKb(@AuthenticationPrincipal AuthenticatedUser user,
                                  @RequestParam(required = false) Long ownerId,
                                  @RequestBody Map<String, String> body) {
        Long resolvedOwner = resolveOwnerId(user, ownerId);
        Long id = kbService.create(resolvedOwner, body.get("name"),
                body.get("description"), body.get("visibility"), body.get("tags"));
        return Result.ok(id);
    }

    @Operation(summary = "列出租户知识库")
    @GetMapping("/kb")
    public Result<List<KnowledgeBase>> listMyKbs(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @RequestParam(required = false) Long ownerId) {
        Long resolvedOwner = resolveOwnerId(user, ownerId);
        return Result.ok(kbService.listByOwner(resolvedOwner));
    }

    @Operation(summary = "列出公开知识库")
    @GetMapping("/kb/public")
    public Result<List<KnowledgeBase>> listPublicKbs() {
        return Result.ok(kbService.listPublic());
    }

    @Operation(summary = "获取知识库详情")
    @GetMapping("/kb/{id}")
    public Result<KnowledgeBase> getKb(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable Long id,
                                       @RequestParam(required = false) Long ownerId) {
        Long resolvedOwner = resolveOwnerId(user, ownerId);
        return Result.ok(kbService.get(id, resolvedOwner));
    }

    @Operation(summary = "删除知识库")
    @DeleteMapping("/kb/{id}")
    public Result<Boolean> deleteKb(@AuthenticationPrincipal AuthenticatedUser user,
                                     @PathVariable Long id,
                                     @RequestParam(required = false) Long ownerId) {
        Long resolvedOwner = resolveOwnerId(user, ownerId);
        return Result.ok(kbService.delete(id, resolvedOwner));
    }

    @Operation(summary = "更新知识库")
    @PutMapping("/kb/{id}")
    public Result<KnowledgeBase> updateKb(@AuthenticationPrincipal AuthenticatedUser user,
                                            @PathVariable Long id,
                                            @RequestParam(required = false) Long ownerId,
                                            @RequestBody Map<String, String> patch) {
        Long resolvedOwner = resolveOwnerId(user, ownerId);
        return Result.ok(kbService.updateKb(id, resolvedOwner, patch));
    }

    // ---------- Document ----------

    @Operation(summary = "上传文档")
    @PostMapping("/doc/upload")
    public Result<Long> uploadDoc(@AuthenticationPrincipal AuthenticatedUser user,
                                   @RequestParam(required = false) Long ownerId,
                                   @RequestParam Long kbId,
                                   @RequestParam(required = false) String title,
                                   @RequestParam(required = false) String sourceType,
                                   @RequestParam(required = false) String tags,
                                   @RequestParam("file") MultipartFile file) throws Exception {
        Long resolvedOwner = resolveOwnerId(user, ownerId);
        byte[] content = file.getBytes();
        String name = file.getOriginalFilename();
        Long id = docService.upload(resolvedOwner, kbId, title, sourceType, content, name, tags);
        return Result.ok(id);
    }

    /**
     * SSE 流式上传 (Day 41)
     * 推送真实进度: UPLOAD → PARSING → CHUNKING → EMBEDDING → INDEXING → DONE
     */
    @Operation(summary = "SSE流式上传（推送真实进度）")
    @PostMapping(value = "/doc/upload-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter uploadDocStream(@AuthenticationPrincipal AuthenticatedUser user,
                                       @RequestParam(required = false) Long ownerId,
                                       @RequestParam Long kbId,
                                       @RequestParam(required = false) String title,
                                       @RequestParam(required = false) String sourceType,
                                       @RequestParam(required = false) String tags,
                                       @RequestParam("file") MultipartFile file) {
        Long resolvedOwner = resolveOwnerId(user, ownerId);
        SseEmitter emitter = new SseEmitter(300_000L); // 5分钟超时
        emitter.onCompletion(() -> log.info("SSE upload 完成: kbId={}", kbId));
        emitter.onTimeout(() -> log.warn("SSE upload 超时: kbId={}", kbId));
        emitter.onError(e -> log.warn("SSE upload 异常: kbId={} err={}", kbId, e.getMessage()));

        new Thread(() -> {
            try {
                byte[] content = file.getBytes();
                String name = file.getOriginalFilename();
                Long docId = docService.uploadWithProgress(
                        resolvedOwner, kbId, title, sourceType, content, name, tags,
                        progress -> {
                            try {
                                String json = String.format(
                                        "{\"stage\":\"%s\",\"progress\":%d,\"message\":\"%s\",\"docId\":%s}",
                                        progress.stage(),
                                        progress.progress(),
                                        progress.message().replace("\"", "\\\""),
                                        progress.docId() != null ? progress.docId() : "null");
                                emitter.send(SseEmitter.event()
                                        .name("progress")
                                        .data(json));
                            } catch (Exception e) {
                                log.debug("SSE send 异常，忽略: {}", e.getMessage());
                            }
                        });
                // 最终发送 docId
                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data("{\"docId\":" + docId + "}"));
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE upload 失败: kbId={} err={}", kbId, e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}"));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    @Operation(summary = "列出知识库文档")
    @GetMapping("/doc")
    public Result<List<Document>> listDocs(@AuthenticationPrincipal AuthenticatedUser user,
                                           @RequestParam Long kbId,
                                           @RequestParam(defaultValue = "50") int limit) {
        kbService.verifyAccess(kbId, user != null ? user.id() : null);
        return Result.ok(docService.listByKb(kbId, limit));
    }

    @Operation(summary = "获取文档切片列表")
    @GetMapping("/doc/{id}/chunks")
    public Result<List<DocumentChunk>> listChunks(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @PathVariable Long id) {
        Long kbId = docService.getKbIdByDocId(id);
        kbService.verifyAccess(kbId, user != null ? user.id() : null);
        return Result.ok(docService.chunksOfDoc(id));
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/doc/{id}")
    public Result<Boolean> deleteDoc(@AuthenticationPrincipal AuthenticatedUser user,
                                      @PathVariable Long id,
                                      @RequestParam(required = false) Long ownerId) {
        Long resolvedOwner = resolveOwnerId(user, ownerId);
        return Result.ok(docService.delete(id, resolvedOwner));
    }

    /** V6.8.5: 文档全文阅读 — 返回完整 content */
    @Operation(summary = "获取文档完整内容")
    @GetMapping("/doc/{id}/content")
    public Result<Document> getDocContent(@AuthenticationPrincipal AuthenticatedUser user,
                                         @PathVariable Long id) {
        Document doc = docService.getById(id);
        if (doc == null) {
            return Result.fail("文档不存在");
        }
        Long kbId = doc.getKbId();
        kbService.verifyAccess(kbId, user != null ? user.id() : null);
        return Result.ok(doc);
    }

    @Operation(summary = "重命名文档")
    @PutMapping("/doc/{id}")
    public Result<Document> renameDoc(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable Long id,
                                       @RequestParam(required = false) Long ownerId,
                                       @RequestBody Map<String, String> body) {
        Long resolvedOwner = resolveOwnerId(user, ownerId);
        String newTitle = body.get("title");
        if (newTitle == null || newTitle.isBlank()) {
            return Result.fail("title 不能为空");
        }
        return Result.ok(docService.renameDoc(id, resolvedOwner, newTitle));
    }

    /**
     * Day 45: 在线编辑文档内容（修改正文 + 重新切片 + 重新索引）
     * PUT /api/v1/rag/doc/{id}/content
     * body: { "content": "新的文档内容..." }
     */
    @Operation(summary = "在线编辑文档内容（重新切片+重新索引）")
    @PutMapping("/doc/{id}/content")
    public Result<Document> updateDocContent(@AuthenticationPrincipal AuthenticatedUser user,
                                              @PathVariable Long id,
                                              @RequestParam(required = false) Long ownerId,
                                              @RequestBody Map<String, Object> body) {
        Long resolvedOwner = resolveOwnerId(user, ownerId);
        String newContent = (String) body.get("content");
        if (newContent == null || newContent.isBlank()) {
            return Result.fail("content 不能为空");
        }
        Document doc = docService.updateDocContent(id, resolvedOwner, newContent);
        return Result.ok(doc);
    }

    /**
     * Day 46: 批量重新索引（批量重新切片 + 批量重新向量化）
     * POST /api/v1/rag/doc/batch/reindex
     * body: { "docIds": [1, 2, 3] }
     * 返回: { succeeded, failed: [{ docId, error }] }
     */
    @Operation(summary = "批量重新索引（重新切片+重新向量化）")
    @PostMapping("/doc/batch/reindex")
    public Result<DocumentService.BatchResult> batchReindexDocs(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Long ownerId,
            @RequestBody Map<String, Object> body) {
        Long resolvedOwner = resolveOwnerId(user, ownerId);
        Object idsObj = body.get("docIds");
        if (idsObj == null) {
            return Result.fail("docIds 不能为空");
        }
        List<Long> docIds;
        if (idsObj instanceof List) {
            docIds = ((List<?>) idsObj).stream()
                    .map(o -> ((Number) o).longValue())
                    .toList();
        } else {
            return Result.fail("docIds 格式错误，应为数组");
        }
        DocumentService.BatchResult result = docService.batchReindexDocs(docIds, resolvedOwner);
        return Result.ok(result);
    }

    /**
     * Day 47: 批量删除文档（批量删除切片 + 批量删除文档记录）
     * DELETE /api/v1/rag/doc/batch
     * body: { "docIds": [1, 2, 3] }
     * 返回: { succeeded, failed: [{ docId, error }] }
     */
    @Operation(summary = "批量删除文档（删除切片+文档记录）")
    @DeleteMapping("/doc/batch")
    public Result<DocumentService.BatchResult> batchDeleteDocs(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Long ownerId,
            @RequestBody Map<String, Object> body) {
        Long resolvedOwner = resolveOwnerId(user, ownerId);
        Object idsObj = body.get("docIds");
        if (idsObj == null) {
            return Result.fail("docIds 不能为空");
        }
        List<Long> docIds;
        if (idsObj instanceof List) {
            docIds = ((List<?>) idsObj).stream()
                    .map(o -> ((Number) o).longValue())
                    .toList();
        } else {
            return Result.fail("docIds 格式错误，应为数组");
        }
        DocumentService.BatchResult result = docService.batchDeleteDocs(docIds, resolvedOwner);
        return Result.ok(result);
    }

    /**
     * Day 48: 批量导出文档 (PDF / TXT)
     * POST /api/v1/rag/doc/export
     * body: { "docIds": [1, 2], "format": "pdf" | "txt" }
     * 返回文件流
     */
    @Operation(summary = "批量导出文档 (PDF/TXT)")
    @PostMapping("/doc/export")
    public void exportDocs(@AuthenticationPrincipal AuthenticatedUser user,
                           @RequestParam(required = false) Long ownerId,
                           @RequestBody Map<String, Object> body,
                           HttpServletResponse response) throws Exception {
        Long resolvedOwner = resolveOwnerId(user, ownerId);
        Object idsObj = body.get("docIds");
        if (idsObj == null) {
            throw new IllegalArgumentException("docIds 不能为空");
        }
        List<Long> docIds;
        if (idsObj instanceof List) {
            docIds = ((List<?>) idsObj).stream()
                    .map(o -> ((Number) o).longValue())
                    .toList();
        } else {
            throw new IllegalArgumentException("docIds 格式错误，应为数组");
        }
        String format = (String) body.getOrDefault("format", "txt");

        var result = docService.exportDocs(docIds, resolvedOwner, format);

        response.setContentType(result.contentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + java.net.URLEncoder.encode(result.filename(), "UTF-8") + "\"");
        response.setContentLength(result.bytes().length);
        response.getOutputStream().write(result.bytes());
        response.getOutputStream().flush();
    }

    // ---------- 检索 + 问答 ----------

    @Operation(summary = "向量检索")
    @PostMapping("/retrieve")
    public Result<List<Retriever.Hit>> retrieve(@AuthenticationPrincipal AuthenticatedUser user,
                                               @RequestBody Map<String, Object> body) {
        Long kbId = body.get("kbId") == null ? null : ((Number) body.get("kbId")).longValue();
        String query = (String) body.get("query");
        Integer topK = (Integer) body.getOrDefault("topK", 5);
        Boolean useTimeliness = (Boolean) body.getOrDefault("useTimeliness", true);
        String sortBy = (String) body.getOrDefault("sortBy", "relevance");
        String fileType = (String) body.getOrDefault("fileType", null);  // Day 58: 文档类型筛选
        // V6.8.2: 归属校验 (公开库任何人都能检索，私有库仅创建者可查)
        kbService.verifyAccess(kbId, user != null ? user.id() : null);
        return Result.ok(retriever.retrieve(kbId, query, topK, useTimeliness, sortBy, fileType));
    }

    // ---------- Day 53: 跨知识库联合检索 ----------
    /**
     * Day 53: 跨知识库联合检索.
     * POST /api/v1/rag/retrieve/multi
     * body: { "kbIds": [1, 2, 3], "query": "...", "topK": 5 }
     * 返回: [{ chunkId, docId, kbId, content, score, rankScore, docTitle, highlight, ... }]
     * 特性:
     *   - 多 KB 并行检索
     *   - 去重（同一 doc 保留最高分 chunk）
     *   - KB 间均衡（每个 KB 至少保留 topK/N 个）
     *   - 综合分排序（相关性 + 时效性加权）
     */
    @Operation(summary = "跨知识库联合检索（多 KB 并行 + 去重 + 均衡 + 时效性加权）")
    @PostMapping("/retrieve/multi")
    public Result<List<Retriever.Hit>> retrieveMulti(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, Object> body) {
        Object kbIdsObj = body.get("kbIds");
        String query = (String) body.get("query");
        Integer topK = (Integer) body.getOrDefault("topK", 5);
        Boolean useTimeliness = (Boolean) body.getOrDefault("useTimeliness", true);
        String sortBy = (String) body.getOrDefault("sortBy", "relevance");
        String fileType = (String) body.getOrDefault("fileType", null);  // Day 58

        if (kbIdsObj == null) {
            return Result.fail("kbIds 不能为空");
        }
        if (!(kbIdsObj instanceof List)) {
            return Result.fail("kbIds 必须是数组格式");
        }

        @SuppressWarnings("unchecked")
        List<Long> kbIds = ((List<?>) kbIdsObj).stream()
                .map(o -> ((Number) o).longValue())
                .toList();

        if (kbIds.isEmpty()) {
            return Result.fail("kbIds 不能为空数组");
        }

        // 归属校验：所有 KB 必须可访问
        Long userId = user != null ? user.id() : null;
        for (Long kbId : kbIds) {
            kbService.verifyAccess(kbId, userId);
        }

        List<Retriever.Hit> hits = retriever.retrieveMultiKb(kbIds, query, topK, useTimeliness, sortBy, fileType);
        return Result.ok(hits);
    }

    // ---------- Day 54: Cross-Encoder 语义重排序 ----------

    /**
     * 对已有候选结果进行 Cross-Encoder 语义重排序.
     *
     * POST /api/v1/rag/retrieve/rerank
     * body: {
     *   "kbId": 1,                    // 知识库 ID
     *   "query": "用户查询",
     *   "topK": 20,                   // 首次检索候选数（默认 20）
     *   "finalTop": 5,                // 最终返回数（默认 5）
     *   "rerankTopK": 20             // 重排序候选数（默认 20）
     * }
     */
    @Operation(summary = "Cross-Encoder 语义重排序（首轮检索 + 交叉注意力精排）")
    @PostMapping("/retrieve/rerank")
    public Result<Map<String, Object>> retrieveRerank(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, Object> body) {
        Long kbId = body.get("kbId") == null ? null : ((Number) body.get("kbId")).longValue();
        String query = (String) body.get("query");
        Integer topK = (Integer) body.getOrDefault("topK", 20);
        Integer finalTop = (Integer) body.getOrDefault("finalTop", 5);
        Boolean useTimeliness = (Boolean) body.getOrDefault("useTimeliness", true);
        String sortBy = (String) body.getOrDefault("sortBy", "relevance");
        String fileType = (String) body.getOrDefault("fileType", null);  // Day 58

        if (query == null || query.isBlank()) {
            return Result.fail("query 不能为空");
        }

        kbService.verifyAccess(kbId, user != null ? user.id() : null);

        long start = System.currentTimeMillis();

        // Step 1: 首轮 Bi-Encoder 检索（取更多候选供重排序）
        List<Retriever.Hit> candidates = retriever.retrieve(kbId, query, Math.max(topK, 20), useTimeliness, sortBy, fileType);
        if (candidates.isEmpty()) {
            return Result.ok(Map.of(
                    "query", query,
                    "totalHits", 0,
                    "rerankedHits", List.of(),
                    "method", "bi-encoder (empty)"
            ));
        }

        // Step 2: Cross-Encoder 重排序
        List<Retriever.Hit> reranked = reranker.rerank(candidates, query, finalTop);

        long elapsed = System.currentTimeMillis() - start;

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("query", query);
        result.put("totalHits", candidates.size());
        result.put("rerankedHits", reranked.stream().map(h -> Map.of(
                "chunkId", h.chunkId,
                "docId", h.docId,
                "docTitle", h.docTitle != null ? h.docTitle : "",
                "content", h.content != null && h.content.length() > 200
                        ? h.content.substring(0, 200) + "..." : (h.content != null ? h.content : ""),
                "score", h.score,
                "rankScore", h.rankScore,
                "highlight", h.highlight != null ? h.highlight : ""
        )).toList());
        result.put("method", "cross-encoder-rerank");
        result.put("elapsedMs", elapsed);
        return Result.ok(result);
    }

    /**
     * 端到端 RAG 问答（含 Cross-Encoder 重排序）.
     *
     * POST /api/v1/rag/ask/rerank
     * body: {
     *   "kbId": 1,
     *   "question": "用户问题",
     *   "history": "对话历史（可选）",
     *   "topK": 20,
     *   "finalTop": 5,
     *   "systemPrompt": "可选系统提示"
     * }
     */
    @Operation(summary = "RAG 问答 + Cross-Encoder 重排序（端到端）")
    @PostMapping("/ask/rerank")
    public Result<Map<String, Object>> askRerank(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, Object> body) {
        Long kbId = body.get("kbId") == null ? null : ((Number) body.get("kbId")).longValue();
        String question = (String) body.get("question");
        String history = (String) body.get("history");
        Integer topK = (Integer) body.getOrDefault("topK", 20);
        Integer finalTop = (Integer) body.getOrDefault("finalTop", 5);
        String systemPrompt = (String) body.get("systemPrompt");

        if (question == null || question.isBlank()) {
            return Result.fail("question 不能为空");
        }

        kbService.verifyAccess(kbId, user != null ? user.id() : null);

        long start = System.currentTimeMillis();

        // Step 1: 首轮检索
        List<Retriever.Hit> candidates = retriever.retrieve(kbId, question, Math.max(topK, 20), true);

        // Step 2: Cross-Encoder 重排序
        List<Retriever.Hit> hits = reranker.rerank(candidates, question, finalTop);

        // Step 3: 构建上下文
        StringBuilder ctx = new StringBuilder();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            ctx.append(systemPrompt).append("\n\n");
        } else {
            ctx.append("你是基于知识库回答问题的助手。请根据以下参考资料回答，引用处标注 [来源N]。\n\n");
        }
        for (int i = 0; i < hits.size(); i++) {
            Retriever.Hit h = hits.get(i);
            ctx.append("[").append(i + 1).append("] ")
               .append(h.docTitle != null ? h.docTitle : "(无标题)")
               .append(" (相似度 ").append(String.format("%.2f", h.score)).append(")\n")
               .append(h.content).append("\n\n");
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("question", question);
        result.put("hitCount", hits.size());
        result.put("sources", hits.stream().map(h -> Map.of(
                "chunkId", h.chunkId,
                "docId", h.docId,
                "docTitle", h.docTitle != null ? h.docTitle : "",
                "score", h.score,
                "rankScore", h.rankScore,
                "highlight", h.highlight != null ? h.highlight : h.content
        )).toList());

        // Step 4: LLM 生成
        try {
            var answer = ragService.ask(kbId, question, history, finalTop, systemPrompt);
            result.put("answer", answer.answer());
            result.put("strategy", "CROSS_ENCODER_RERANK");
            result.put("elapsedMs", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("Cross-Encoder RAG LLM 调用失败: {}", e.getMessage());
            result.put("answer", "（LLM 暂时不可用，以下为检索内容）\n\n" + ctx);
            result.put("strategy", "CROSS_ENCODER_RETRIEVAL_ONLY");
            result.put("elapsedMs", System.currentTimeMillis() - start);
        }

        return Result.ok(result);
    }

    /**
     * Day 53: 跨知识库 RAG 问答.
     * POST /api/v1/rag/ask/multi
     * body: { "kbIds": [1, 2], "question": "...", "history": "...", "topK": 5, "systemPrompt": "..." }
     */
    @Operation(summary = "跨知识库 RAG 问答（多 KB 联合检索 + LLM 生成答案）")
    @PostMapping("/ask/multi")
    public Result<Map<String, Object>> askMulti(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, Object> body) {
        Object kbIdsObj = body.get("kbIds");
        String question = (String) body.get("question");
        String history = (String) body.get("history");
        Integer topK = (Integer) body.getOrDefault("topK", 5);
        String systemPrompt = (String) body.get("systemPrompt");
        String fileType = (String) body.getOrDefault("fileType", null);  // Day 58

        if (kbIdsObj == null || !(kbIdsObj instanceof List) || ((List<?>) kbIdsObj).isEmpty()) {
            return Result.fail("kbIds 必须是包含至少一个 KB 的数组");
        }
        if (question == null || question.isBlank()) {
            return Result.fail("question 不能为空");
        }

        @SuppressWarnings("unchecked")
        List<Long> kbIds = ((List<?>) kbIdsObj).stream()
                .map(o -> ((Number) o).longValue())
                .toList();

        // 归属校验
        Long userId = user != null ? user.id() : null;
        for (Long kbId : kbIds) {
            kbService.verifyAccess(kbId, userId);
        }

        // 跨 KB 检索
        List<Retriever.Hit> hits = retriever.retrieveMultiKb(kbIds, question, topK, true, "relevance", fileType);

        // 构建上下文（每个 KB 分别标注）
        Map<Long, String> kbNameMap = new java.util.LinkedHashMap<>();
        for (Long kbId : kbIds) {
            try {
                var kb = kbService.get(kbId, userId);
                kbNameMap.put(kbId, kb != null ? kb.getName() : "KB-" + kbId);
            } catch (Exception e) {
                kbNameMap.put(kbId, "KB-" + kbId);
            }
        }

        // 拼带 KB 来源标注的上下文
        StringBuilder ctx = new StringBuilder();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            ctx.append(systemPrompt).append("\n\n");
        } else {
            ctx.append("你是基于多个知识库回答问题的助手。请根据以下参考资料回答，引用处标注 [来源N:文档标题]。\n\n");
        }
        for (int i = 0; i < hits.size(); i++) {
            Retriever.Hit h = hits.get(i);
            String kbName = kbNameMap.getOrDefault(h.kbId, "KB-" + h.kbId);
            ctx.append("[").append(i + 1).append("] [").append(kbName).append("] ")
               .append(h.docTitle != null ? h.docTitle : "(无标题)")
               .append(" (相似度 ").append(String.format("%.2f", h.score)).append(")\n")
               .append(h.content).append("\n\n");
        }

        // 调用 LLM
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", ctx.toString()));
        if (history != null && !history.isBlank()) {
            messages.add(Map.of("role", "user", "content", history));
        }
        messages.add(Map.of("role", "user", "content", question));

        // 构造结果
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("question", question);
        result.put("kbCount", kbIds.size());
        result.put("kbNames", kbNameMap.values().toList());
        result.put("hitCount", hits.size());
        result.put("sources", hits.stream().map(h ->
                Map.of("chunkId", h.chunkId, "docId", h.docId, "kbId", h.kbId,
                        "docTitle", h.docTitle != null ? h.docTitle : "",
                        "score", h.score,
                        "rankScore", h.rankScore,
                        "highlight", h.highlight != null ? h.highlight : h.content)
        ).toList());

        // 简单 LLM 调用（失败不阻塞返回）
        try {
            var answer = ragService.ask(null, question, history, topK, systemPrompt);
            result.put("answer", answer.answer());
            result.put("strategy", answer.strategy());
            result.put("elapsedMs", answer.elapsedMs());
        } catch (Exception e) {
            log.warn("跨 KB RAG LLM 调用失败: {}", e.getMessage());
            result.put("answer", "（LLM 暂时不可用，以下为检索内容）\n\n" + ctx);
            result.put("strategy", "RETRIEVAL_ONLY");
            result.put("elapsedMs", 0);
        }

        return Result.ok(result);
    }

    @Operation(summary = "RAG问答 (支持 systemPrompt 自定义模板)")
    @PostMapping("/ask")
    public Result<RagService.RagAnswer> ask(@AuthenticationPrincipal AuthenticatedUser user,
                                            @RequestBody Map<String, Object> body) {
        Long kbId = body.get("kbId") == null ? null : ((Number) body.get("kbId")).longValue();
        String question = (String) body.get("question");
        String history = (String) body.get("history");
        Integer topK = (Integer) body.getOrDefault("topK", 5);
        String systemPrompt = (String) body.get("systemPrompt");
        // V6.8.2: 归属校验
        kbService.verifyAccess(kbId, user != null ? user.id() : null);
        return Result.ok(ragService.ask(kbId, question, history, topK, systemPrompt));
    }
}
