package com.minimax.rag.controller;

import com.minimax.common.result.Result;
import com.minimax.rag.entity.Document;
import com.minimax.rag.entity.DocumentChunk;
import com.minimax.rag.entity.KnowledgeBase;
import com.minimax.rag.retriever.Retriever;
import com.minimax.rag.service.DocumentService;
import com.minimax.rag.service.KnowledgeBaseService;
import com.minimax.rag.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * RAG 控制器 (Day 8 完整版).
 *
 * 知识库 (KB):
 *   POST   /rag/kb                          建库
 *   GET    /rag/kb                          列出我的
 *   GET    /rag/kb/public                   列出公开的
 *   GET    /rag/kb/{id}                     详情
 *   DELETE /rag/kb/{id}                     删除
 *
 * 文档 (Document):
 *   POST   /rag/doc/upload?kbId=1           上传 (multipart file)
 *   GET    /rag/doc?kbId=1                  列出
 *   GET    /rag/doc/{id}/chunks             切片列表
 *   DELETE /rag/doc/{id}                    删除
 *
 * 检索 + 问答:
 *   POST   /rag/retrieve                    纯检索 (返回 topK chunks)
 *   POST   /rag/ask                         RAG 问答 (检索+LLM+引用)
 */
@Tag(name = "RAG知识库")
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final KnowledgeBaseService kbService;
    private final DocumentService docService;
    private final Retriever retriever;
    private final RagService ragService;

    // ---------- KB ----------

    @Operation(summary = "创建知识库")
    @PostMapping("/kb")
    public Result<Long> createKb(@RequestParam Long ownerId,
                                  @RequestBody Map<String, String> body) {
        Long id = kbService.create(ownerId, body.get("name"),
                body.get("description"), body.get("visibility"), body.get("tags"));
        return Result.ok(id);
    }

    @Operation(summary = "列出租户知识库")
    @GetMapping("/kb")
    public Result<List<KnowledgeBase>> listMyKbs(@RequestParam Long ownerId) {
        return Result.ok(kbService.listByOwner(ownerId));
    }

    @Operation(summary = "列出公开知识库")
    @GetMapping("/kb/public")
    public Result<List<KnowledgeBase>> listPublicKbs() {
        return Result.ok(kbService.listPublic());
    }

    @Operation(summary = "获取知识库详情")
    @GetMapping("/kb/{id}")
    public Result<KnowledgeBase> getKb(@PathVariable Long id, @RequestParam Long ownerId) {
        return Result.ok(kbService.get(id, ownerId));
    }

    @Operation(summary = "删除知识库")
    @DeleteMapping("/kb/{id}")
    public Result<Boolean> deleteKb(@PathVariable Long id, @RequestParam Long ownerId) {
        return Result.ok(kbService.delete(id, ownerId));
    }

    // ---------- Document ----------

    @Operation(summary = "上传文档")
    @PostMapping("/doc/upload")
    public Result<Long> uploadDoc(@RequestParam Long ownerId,
                                   @RequestParam Long kbId,
                                   @RequestParam(required = false) String title,
                                   @RequestParam(required = false) String sourceType,
                                   @RequestParam(required = false) String tags,
                                   @RequestParam("file") MultipartFile file) throws Exception {
        byte[] content = file.getBytes();
        String name = file.getOriginalFilename();
        Long id = docService.upload(ownerId, kbId, title, sourceType, content, name, tags);
        return Result.ok(id);
    }

    @Operation(summary = "列出知识库文档")
    @GetMapping("/doc")
    public Result<List<Document>> listDocs(@RequestParam Long kbId,
                                            @RequestParam(defaultValue = "50") int limit) {
        return Result.ok(docService.listByKb(kbId, limit));
    }

    @Operation(summary = "获取文档切片列表")
    @GetMapping("/doc/{id}/chunks")
    public Result<List<DocumentChunk>> listChunks(@PathVariable Long id) {
        return Result.ok(docService.chunksOfDoc(id));
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/doc/{id}")
    public Result<Boolean> deleteDoc(@PathVariable Long id, @RequestParam Long ownerId) {
        return Result.ok(docService.delete(id, ownerId));
    }

    // ---------- 检索 + 问答 ----------

    @Operation(summary = "向量检索")
    @PostMapping("/retrieve")
    public Result<List<Retriever.Hit>> retrieve(@RequestBody Map<String, Object> body) {
        Long kbId = body.get("kbId") == null ? null : ((Number) body.get("kbId")).longValue();
        String query = (String) body.get("query");
        Integer topK = (Integer) body.getOrDefault("topK", 5);
        return Result.ok(retriever.retrieve(kbId, query, topK));
    }

    @Operation(summary = "RAG问答")
    @PostMapping("/ask")
    public Result<RagService.RagAnswer> ask(@RequestBody Map<String, Object> body) {
        Long kbId = body.get("kbId") == null ? null : ((Number) body.get("kbId")).longValue();
        String question = (String) body.get("question");
        String history = (String) body.get("history");
        Integer topK = (Integer) body.getOrDefault("topK", 5);
        return Result.ok(ragService.ask(kbId, question, history, topK));
    }
}