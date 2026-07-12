package com.minimax.ai.modelmarket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 模型市场 API (V2.9.1)
 *
 * <h3>端点</h3>
 * <pre>
 *   POST /api/v1/ai/model-market/upload         multipart 上传
 *   POST /api/v1/ai/model-market/publish       仅元数据发布 (无文件)
 *   GET  /api/v1/ai/model-market/models        浏览
 *   GET  /api/v1/ai/model-market/models/{key}  详情
 *   GET  /api/v1/ai/model-market/models/{key}/download  下载
 *   POST /api/v1/ai/model-market/models/{key}/rate
 *   GET  /api/v1/ai/model-market/models/{key}/ratings
 *   GET  /api/v1/ai/model-market/my?authorId=
 *   POST /api/v1/ai/model-market/models/{key}/status
 *   GET  /api/v1/ai/model-market/stats
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/ai/model-market")
@RequiredArgsConstructor
public class ModelMarketController {

    private final ModelMarketService service;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadWrapped(
            @RequestPart("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("authorId") Long authorId,
            @RequestParam(value = "authorName", required = false) String authorName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "modelType", required = false) String modelType,
            @RequestParam(value = "taskType", required = false) String taskType,
            @RequestParam(value = "baseModel", required = false) String baseModel,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "license", required = false) String license,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "metricsJson", required = false) String metricsJson) {
        return upload(file, name, authorId, authorName, description, modelType, taskType, baseModel, version, license, tags, metricsJson);
    }

    public ResponseEntity<Map<String, Object>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("authorId") Long authorId,
            @RequestParam(value = "authorName", required = false) String authorName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "modelType", required = false) String modelType,
            @RequestParam(value = "taskType", required = false) String taskType,
            @RequestParam(value = "baseModel", required = false) String baseModel,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "license", required = false) String license,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "metricsJson", required = false) String metricsJson) {
        try {
            Map<String, Object> meta = new HashMap<>();
            meta.put("description", description);
            meta.put("modelType", modelType);
            meta.put("taskType", taskType);
            meta.put("baseModel", baseModel);
            meta.put("version", version);
            meta.put("license", license);
            meta.put("tags", tags);
            meta.put("metricsJson", metricsJson);
            ModelEntry entry = service.upload(file, name, meta, authorId, authorName);
            return ResponseEntity.ok(Map.of("code", 0, "data", entry, "message", "上传成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @PostMapping("/publish")
    public ResponseEntity<Map<String, Object>> publish(@RequestBody PublishRequest req) {
        try {
            ModelEntry entry = service.uploadMetadata(
                req.getName(), req.getDescription(), req.getModelType(), req.getTaskType(),
                req.getBaseModel(), req.getVersion(), req.getLicense(),
                req.getAuthorId(), req.getAuthorName(), req.getTags()
            );
            return ResponseEntity.ok(Map.of("code", 0, "data", entry));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @GetMapping("/models")
    public ResponseEntity<Map<String, Object>> browse(
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "50") int limit) {
        List<ModelEntry> list = service.browse(modelType, taskType, keyword, sortBy, limit);
        return ResponseEntity.ok(Map.of("code", 0, "data", list, "total", list.size()));
    }

    @GetMapping("/models/{key}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable String key) {
        ModelEntry entry = service.detail(key);
        if (entry == null) {
            return ResponseEntity.ok(Map.of("code", 404, "message", "模型不存在"));
        }
        return ResponseEntity.ok(Map.of("code", 0, "data", entry));
    }

    @GetMapping("/models/{key}/download")
    public ResponseEntity<Resource> download(@PathVariable String key) {
        Path path = service.downloadPath(key);
        if (path == null || !Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(path);
        ModelEntry entry = service.detail(key);
        String filename = entry != null && entry.getFileName() != null ? entry.getFileName() : "model.bin";
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .body(resource);
    }

    @PostMapping("/models/{key}/rate")
    public ResponseEntity<Map<String, Object>> rate(
            @PathVariable String key, @RequestBody RateRequest req) {
        try {
            service.rate(key, req.getUserId(), req.getUsername(), req.getRating(), req.getComment());
            return ResponseEntity.ok(Map.of("code", 0, "message", "评分成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @GetMapping("/models/{key}/ratings")
    public ResponseEntity<Map<String, Object>> ratings(
            @PathVariable String key,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(Map.of("code", 0, "data", service.ratings(key, limit)));
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> my(@RequestParam Long authorId) {
        return ResponseEntity.ok(Map.of("code", 0, "data", service.myModels(authorId)));
    }

    @PostMapping("/models/{key}/status")
    public ResponseEntity<Map<String, Object>> changeStatus(
            @PathVariable String key, @RequestBody Map<String, Object> body) {
        String newStatus = (String) body.get("status");
        boolean ok = service.changeStatus(key, newStatus);
        return ResponseEntity.ok(Map.of("code", ok ? 0 : 404, "message", ok ? "已更新" : "模型不存在"));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(Map.of("code", 0, "data", service.stats()));
    }

    // ============= DTO =============

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublishRequest {
        private String name;
        private String description;
        private String modelType;
        private String taskType;
        private String baseModel;
        private String version;
        private String license;
        private Long authorId;
        private String authorName;
        private String tags;
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
