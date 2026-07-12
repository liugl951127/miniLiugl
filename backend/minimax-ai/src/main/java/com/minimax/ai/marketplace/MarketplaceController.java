package com.minimax.ai.marketplace;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent Marketplace API (V2.9.0)
 *
 * <h3>端点</h3>
 * <pre>
 *   GET  /api/v1/ai/marketplace/agents                  浏览市场
 *   GET  /api/v1/ai/marketplace/agents/{key}           详情
 *   POST /api/v1/ai/marketplace/agents                 上传
 *   POST /api/v1/ai/marketplace/agents/{key}/rate      评分
 *   GET  /api/v1/ai/marketplace/agents/{key}/ratings   评分列表
 *   POST /api/v1/ai/marketplace/agents/{key}/use       记录使用
 *   POST /api/v1/ai/marketplace/agents/{key}/approve   审核
 *   GET  /api/v1/ai/marketplace/my?authorId=           我的
 *   GET  /api/v1/ai/marketplace/stats                  统计
 * </pre>
 *
 * @author MiniMax
 * @since V2.9.0
 */
@RestController
@RequestMapping("/api/v1/ai/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplaceService service;

    @GetMapping("/agents")
    public ResponseEntity<Map<String, Object>> browse(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "50") int limit) {
        List<MarketplaceAgent> list = service.browse(category, keyword, sortBy, limit);
        return ResponseEntity.ok(Map.of(
            "code", 0, "data", list, "total", list.size()
        ));
    }

    @GetMapping("/agents/{key}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable String key) {
        MarketplaceAgent agent = service.detail(key);
        if (agent == null) {
            return ResponseEntity.ok(Map.of("code", 404, "message", "Agent 不存在"));
        }
        // 详情时增加使用次数
        service.recordUsage(key);
        return ResponseEntity.ok(Map.of("code", 0, "data", agent));
    }

    @PostMapping("/agents")
    public ResponseEntity<Map<String, Object>> upload(@RequestBody UploadRequest req) {
        try {
            MarketplaceAgent agent = service.upload(
                req.getName(), req.getDescription(), req.getCategory(), req.getIcon(),
                req.getAuthorId(), req.getAuthorName(), req.getDefinitionJson(),
                req.getVersion(), req.getVisibility(), req.getTags(), req.getCapabilities()
            );
            return ResponseEntity.ok(Map.of("code", 0, "data", agent, "message", "上传成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @PostMapping("/agents/{key}/rate")
    public ResponseEntity<Map<String, Object>> rate(
            @PathVariable String key, @RequestBody RateRequest req) {
        try {
            service.rate(key, req.getUserId(), req.getUsername(),
                req.getRating(), req.getComment());
            return ResponseEntity.ok(Map.of("code", 0, "message", "评分成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @GetMapping("/agents/{key}/ratings")
    public ResponseEntity<Map<String, Object>> ratings(
            @PathVariable String key,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(Map.of(
            "code", 0, "data", service.ratings(key, limit)
        ));
    }

    @PostMapping("/agents/{key}/use")
    public ResponseEntity<Map<String, Object>> use(@PathVariable String key) {
        service.recordUsage(key);
        return ResponseEntity.ok(Map.of("code", 0, "message", "ok"));
    }

    @PostMapping("/agents/{key}/approve")
    public ResponseEntity<Map<String, Object>> approve(
            @PathVariable String key, @RequestBody Map<String, Object> body) {
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        String reason = (String) body.get("reason");
        boolean ok = service.approve(key, approved, reason);
        return ResponseEntity.ok(Map.of("code", ok ? 0 : 404, "message", ok ? "审核完成" : "Agent 不存在"));
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> my(@RequestParam Long authorId) {
        return ResponseEntity.ok(Map.of("code", 0, "data", service.myAgents(authorId)));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(Map.of("code", 0, "data", service.stats()));
    }

    // ============= DTO =============

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadRequest {
        private String name;
        private String description;
        private String category;
        private String icon;
        private Long authorId;
        private String authorName;
        private String definitionJson;
        private String version;
        private String visibility;
        private String tags;
        private String capabilities;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateRequest {
        private Long userId;
        private String username;
        private Integer rating;
        private String comment;
    }
}
