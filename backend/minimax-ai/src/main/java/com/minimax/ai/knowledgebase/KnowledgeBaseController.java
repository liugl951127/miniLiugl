package com.minimax.ai.knowledgebase;

import com.minimax.ai.entity.KbChunk;
import com.minimax.ai.entity.KbDocument;
import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 自研知识库 REST API (V3.4.0)
 *
 * <p>API 列表 (统一 /api/v1/ai/kb 前缀):
 * <ul>
 *   <li>POST   /upload                  上传文档 (multipart)</li>
 *   <li>GET    /docs/{docId}            查文档</li>
 *   <li>GET    /docs/list/{kbId}        列文档</li>
 *   <li>DELETE /docs/{docId}            删文档</li>
 *   <li>GET    /chunks/{docId}          列分块</li>
 *   <li>POST   /search                  混合检索 (RRF)</li>
 *   <li>POST   /search/keyword          纯关键词检索</li>
 *   <li>GET    /stats/{kbId}            知识库统计</li>
 *   <li>POST   /permission/grant        授权</li>
 *   <li>POST   /permission/revoke       撤销</li>
 *   <li>GET    /public                  公开知识库</li>
 * </ul>
 */
@Tag(name = "自研知识库")
@RestController
@RequestMapping("/api/v1/ai/kb")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService service;

    /**
     * 上传文档
     */
    @Operation(summary = "上传文档")
    @PostMapping("/upload")
    public Result<KbDocument> upload(@RequestParam String kbId,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestParam(required = false) Long ownerId,
                                       @RequestParam(required = false) String tags,
                                       @RequestParam(required = false, defaultValue = "false") Boolean isPublic) {
        try {
            return Result.ok(service.uploadDocument(kbId, file, ownerId, tags, isPublic));
        } catch (Exception e) {
            return Result.fail(500, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 查文档
     */
    @Operation(summary = "查文档")
    @GetMapping("/docs/{docId}")
    public Result<KbDocument> getDoc(@PathVariable String docId) {
        KbDocument d = service.findDocument(docId);
        if (d == null) return Result.fail(404, "文档不存在");
        return Result.ok(d);
    }

    /**
     * 列文档
     */
    @Operation(summary = "列知识库文档")
    @GetMapping("/docs/list/{kbId}")
    public Result<List<KbDocument>> listDocs(@PathVariable String kbId) {
        return Result.ok(service.listDocuments(kbId));
    }

    /**
     * 删文档
     */
    @Operation(summary = "删文档")
    @DeleteMapping("/docs/{docId}")
    public Result<Void> deleteDoc(@PathVariable String docId) {
        boolean ok = service.deleteDocument(docId);
        return ok ? Result.ok() : Result.fail(404, "文档不存在");
    }

    /**
     * 列分块
     */
    @Operation(summary = "列文档分块")
    @GetMapping("/chunks/{docId}")
    public Result<List<KbChunk>> getChunks(@PathVariable String docId) {
        return Result.ok(service.getChunks(docId));
    }

    /**
     * 混合检索
     */
    @Operation(summary = "混合检索 (RRF 融合)")
    @PostMapping("/search")
    public Result<List<KnowledgeBaseService.SearchHit>> search(@RequestBody Map<String, Object> body) {
        String kbId = (String) body.get("kbId");
        String query = (String) body.get("query");
        int topK = body.get("topK") == null ? 5 : ((Number) body.get("topK")).intValue();
        Long userId = body.get("userId") == null ? null : ((Number) body.get("userId")).longValue();
        if (kbId == null || query == null) return Result.fail(400, "kbId/query 必填");
        return Result.ok(service.hybridSearch(kbId, query, topK, userId));
    }

    /**
     * 关键词检索
     */
    @Operation(summary = "纯关键词检索")
    @PostMapping("/search/keyword")
    public Result<List<KnowledgeBaseService.SearchHit>> searchKeyword(@RequestBody Map<String, Object> body) {
        String kbId = (String) body.get("kbId");
        String query = (String) body.get("query");
        int topK = body.get("topK") == null ? 5 : ((Number) body.get("topK")).intValue();
        Long userId = body.get("userId") == null ? null : ((Number) body.get("userId")).longValue();
        return Result.ok(service.keywordSearch(kbId, query, topK, userId));
    }

    /**
     * 知识库统计
     */
    @Operation(summary = "知识库统计")
    @GetMapping("/stats/{kbId}")
    public Result<Map<String, Object>> stats(@PathVariable String kbId) {
        return Result.ok(service.stats(kbId));
    }

    /**
     * 授权
     */
    @Operation(summary = "授权")
    @PostMapping("/permission/grant")
    public Result<Void> grant(@RequestBody Map<String, Object> body) {
        String kbId = (String) body.get("kbId");
        String subjectType = (String) body.get("subjectType");
        Long subjectId = ((Number) body.get("subjectId")).longValue();
        String permission = (String) body.get("permission");
        Long grantBy = body.get("grantBy") == null ? null : ((Number) body.get("grantBy")).longValue();
        service.grantPermission(kbId, subjectType, subjectId, permission, grantBy);
        return Result.ok();
    }

    /**
     * 撤销
     */
    @Operation(summary = "撤销权限")
    @PostMapping("/permission/revoke")
    public Result<Void> revoke(@RequestBody Map<String, Object> body) {
        String kbId = (String) body.get("kbId");
        String subjectType = (String) body.get("subjectType");
        Long subjectId = ((Number) body.get("subjectId")).longValue();
        boolean ok = service.revokePermission(kbId, subjectType, subjectId);
        return ok ? Result.ok() : Result.fail(404, "权限不存在");
    }

    /**
     * 公开知识库
     */
    @Operation(summary = "公开文档列表")
    @GetMapping("/public")
    public Result<List<KbDocument>> publicDocs(@RequestParam(defaultValue = "20") int limit) {
        return Result.ok(service.listPublic(limit));
    }
}
