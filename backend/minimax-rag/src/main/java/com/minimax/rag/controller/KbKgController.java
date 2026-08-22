// =============================================================
// MiniMax - RAG KB 知识图谱控制器 (V7.3)
//
// 路径前缀: /api/v1/rag/kb
// 6 个端点:
//   POST   /{kbId}/kg/build     触发抽取, 返回 {entities, relations, docCount, elapsedMs}
//   GET    /{kbId}/kg           拉该 KB 的图 (entities + relations)
//   GET    /{kbId}/kg/stats     统计 {entities, relations, types}
//   GET    /{kbId}/kg/search    模糊搜索实体 (?kw=xxx&limit=20)
//   GET    /kg/reason           关系推理 (?src=X&tgt=Y[&kbId=Z])
//   DELETE /{kbId}/kg           清空该 KB 抽取结果
//
// @author general
// @since 2026-08-22
// =============================================================

package com.minimax.rag.controller;

import com.minimax.common.result.Result;
import com.minimax.rag.kg.EntityExtractor;
import com.minimax.rag.kg.RelationReasoner;
import com.minimax.rag.kg.entity.KbExtractedEntity;
import com.minimax.rag.kg.entity.KbExtractedRelation;
import com.minimax.rag.kg.mapper.KbExtractedEntityMapper;
import com.minimax.rag.kg.mapper.KbExtractedRelationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "RAG-KB 知识图谱", description = "KB 文档分词 → 实体抽取 + 图谱查询 + 关系推理")
@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
public class KbKgController {

    private final EntityExtractor extractor;
    private final RelationReasoner reasoner;
    private final KbExtractedEntityMapper entityMapper;
    private final KbExtractedRelationMapper relationMapper;

    // ==================== 1. POST /{kbId}/kg/build ====================

    @Operation(summary = "触发 KB 实体抽取 (异步写入 kb_extracted_entity / kb_extracted_relation)")
    @PostMapping("/kb/{kbId}/kg/build")
    public Result<EntityExtractor.BuildResult> buildKg(
            @PathVariable Long kbId,
            @RequestBody(required = false) Map<String, Object> body) {
        EntityExtractor.ExtractConfig cfg = EntityExtractor.ExtractConfig.defaults();
        if (body != null) {
            if (body.get("minFreq") instanceof Number n) {
                cfg.setMinFreq(Math.max(1, n.intValue()));
            }
            if (body.get("docLimit") instanceof Number n) {
                cfg.setDocLimit(Math.max(0, n.intValue()));
            }
            if (body.get("paragraphCooccur") instanceof Boolean b) {
                cfg.setParagraphCooccur(b);
            }
        }
        EntityExtractor.BuildResult r = extractor.build(kbId, cfg);
        return Result.ok(r);
    }

    // ==================== 2. GET /{kbId}/kg ====================

    @Operation(summary = "拉取该 KB 的图 (entities + relations)")
    @GetMapping("/kb/{kbId}/kg")
    public Result<Map<String, Object>> getKg(@PathVariable Long kbId) {
        List<KbExtractedEntity> entities = entityMapper.selectByKb(kbId);
        List<KbExtractedRelation> relations = relationMapper.selectByKb(kbId);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("kbId", kbId);
        resp.put("entities", entities);
        resp.put("relations", relations);
        resp.put("entityCount", entities.size());
        resp.put("relationCount", relations.size());
        return Result.ok(resp);
    }

    // ==================== 3. GET /{kbId}/kg/stats ====================

    @Operation(summary = "统计该 KB 的图谱数据")
    @GetMapping("/kb/{kbId}/kg/stats")
    public Result<Map<String, Object>> stats(@PathVariable Long kbId) {
        int ents = entityMapper.countByKb(kbId);
        int rels = relationMapper.countByKb(kbId);

        // 类型分布
        Map<String, Integer> typeDist = new LinkedHashMap<>();
        for (KbExtractedEntity e : entityMapper.selectByKb(kbId)) {
            typeDist.merge(e.getType() == null ? "CONCEPT" : e.getType(), 1, Integer::sum);
        }

        // 关系类型分布
        Map<String, Integer> relDist = new LinkedHashMap<>();
        for (KbExtractedRelation r : relationMapper.selectByKb(kbId)) {
            relDist.merge(r.getRel() == null ? "CO_OCCUR" : r.getRel(), 1, Integer::sum);
        }

        // Top 10 实体 (按 freq)
        List<KbExtractedEntity> top = entityMapper.selectByKb(kbId).stream()
                .limit(10)
                .collect(Collectors.toList());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("kbId", kbId);
        resp.put("entityCount", ents);
        resp.put("relationCount", rels);
        resp.put("typeDistribution", typeDist);
        resp.put("relTypeDistribution", relDist);
        resp.put("topEntities", top);
        return Result.ok(resp);
    }

    // ==================== 4. GET /{kbId}/kg/search ====================

    @Operation(summary = "模糊搜索实体 (按 name LIKE)")
    @GetMapping("/kb/{kbId}/kg/search")
    public Result<Map<String, Object>> search(
            @PathVariable Long kbId,
            @Parameter(description = "搜索关键词") @RequestParam String kw,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        String keyword = kw == null ? "" : kw.trim();
        int cap = Math.max(1, Math.min(limit, 200));
        List<KbExtractedEntity> hits = keyword.isEmpty()
                ? List.of()
                : entityMapper.searchByName(keyword, cap);

        // 过滤只属于该 KB (跨 KB 表用 LIKE, 此处做一次客户端过滤)
        hits = hits.stream()
                .filter(e -> Objects.equals(e.getKbId(), kbId))
                .collect(Collectors.toList());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("list", hits);
        resp.put("total", hits.size());
        resp.put("keyword", keyword);
        return Result.ok(resp);
    }

    // ==================== 5. GET /kg/reason ====================

    @Operation(summary = "关系推理 (BFS 找 src → tgt 路径, 1-3 hop)")
    @GetMapping("/kg/reason")
    public Result<RelationReasoner.ReasonResult> reason(
            @RequestParam String src,
            @RequestParam String tgt,
            @RequestParam(required = false) Long kbId,
            @RequestParam(required = false, defaultValue = "3") int maxHops,
            @RequestParam(required = false, defaultValue = "10") int maxPaths) {
        RelationReasoner.ReasonResult r = reasoner.reason(kbId, src, tgt, maxHops, maxPaths);
        return Result.ok(r);
    }

    // ==================== 6. DELETE /{kbId}/kg ====================

    @Operation(summary = "清空该 KB 的图谱抽取结果")
    @DeleteMapping("/kb/{kbId}/kg")
    public Result<EntityExtractor.ClearResult> clear(@PathVariable Long kbId) {
        EntityExtractor.ClearResult r = extractor.clear(kbId);
        return Result.ok(r);
    }
}
