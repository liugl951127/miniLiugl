package com.minimax.ai.modelmarket;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 模型市场服务 (V2.9.1)
 *
 * <h3>核心能力</h3>
 * <ul>
 *   <li>模型上传 (multipart + 元数据 + SHA256 校验)</li>
 *   <li>浏览市场 (分类/类型/排序)</li>
 *   <li>下载 (本地文件系统)</li>
 *   <li>评分/评论 (1-5 星)</li>
 *   <li>状态机: DRAFT → PUBLISHED → DEPRECATED</li>
 *   <li>训练任务自动发布 (model.task.completed event)</li>
 * </ul>
 *
 * <h3>存储</h3>
 * <p>默认 <code>${MINIMAX_MODEL_DIR:-/var/minimax/models}</code></p>
 *
 * @author MiniMax
 * @since V2.9.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelMarketService {

    private final ModelMarketMapper marketMapper;
    private final ModelRatingMapper ratingMapper;

    /** 存储根目录 */
    private final String modelRoot = System.getenv().getOrDefault("MINIMAX_MODEL_DIR", "/var/minimax/models");

    /**
     * 上传模型
     *
     * @param file     模型文件
     * @param name     模型名
     * @param metadata 元数据 (description/modelType/taskType/...)
     * @param authorId 作者 ID
     * @return 已保存的 ModelEntry
     */
    @Transactional
    public ModelEntry upload(MultipartFile file, String name, Map<String, Object> metadata, Long authorId, String authorName) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("模型名不能为空");
        }
        long size = file.getSize();
        // 限制 5GB
        if (size > 5L * 1024 * 1024 * 1024) {
            throw new IllegalArgumentException("文件超过 5GB 限制");
        }

        // 1. 生成 modelKey
        String modelKey = generateKey(name, authorId);
        // 2. 保存到磁盘
        Path targetDir = Paths.get(modelRoot, modelKey);
        Files.createDirectories(targetDir);
        String originalName = file.getOriginalFilename() == null ? "model.bin" : file.getOriginalFilename();
        Path targetFile = targetDir.resolve(originalName);
        file.transferTo(targetFile.toFile());
        // 3. 计算 SHA256
        String sha256 = computeSha256(targetFile);

        // 4. 写库
        ModelEntry entry = ModelEntry.builder()
            .modelKey(modelKey)
            .name(name)
            .description((String) metadata.getOrDefault("description", ""))
            .modelType((String) metadata.getOrDefault("modelType", "PYTORCH"))
            .taskType((String) metadata.getOrDefault("taskType", "OTHER"))
            .baseModel((String) metadata.getOrDefault("baseModel", ""))
            .version((String) metadata.getOrDefault("version", "1.0.0"))
            .filePath(targetFile.toString())
            .fileName(originalName)
            .fileSize(size)
            .sha256(sha256)
            .license((String) metadata.getOrDefault("license", "MIT"))
            .authorId(authorId)
            .authorName(authorName)
            .tags((String) metadata.getOrDefault("tags", ""))
            .metricsJson((String) metadata.getOrDefault("metricsJson", "{}"))
            .status("DRAFT")
            .downloadCount(0L)
            .avgRating(0.0)
            .ratingCount(0L)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        marketMapper.insert(entry);
        log.info("[model-market] 上传 modelKey={} size={} type={}", modelKey, size, entry.getModelType());
        return entry;
    }

    /**
     * 简化版上传 (无文件, 仅元数据) - 用于演示
     */
    @Transactional
    public ModelEntry uploadMetadata(String name, String description, String modelType, String taskType,
                                       String baseModel, String version, String license,
                                       Long authorId, String authorName, String tags) {
        String modelKey = generateKey(name, authorId);
        ModelEntry entry = ModelEntry.builder()
            .modelKey(modelKey)
            .name(name)
            .description(description)
            .modelType(modelType == null ? "PYTORCH" : modelType)
            .taskType(taskType == null ? "OTHER" : taskType)
            .baseModel(baseModel)
            .version(version == null ? "1.0.0" : version)
            .filePath("")
            .fileName("")
            .fileSize(0L)
            .sha256("")
            .license(license == null ? "MIT" : license)
            .authorId(authorId)
            .authorName(authorName)
            .tags(tags)
            .metricsJson("{}")
            .status("PUBLISHED")
            .downloadCount(0L)
            .avgRating(0.0)
            .ratingCount(0L)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .publishedAt(LocalDateTime.now())
            .build();
        marketMapper.insert(entry);
        return entry;
    }

    /**
     * 浏览市场
     */
    public List<ModelEntry> browse(String modelType, String taskType, String keyword, String sortBy, int limit) {
        if (limit <= 0) limit = 50;
        if (limit > 200) limit = 200;
        QueryWrapper<ModelEntry> qw = new QueryWrapper<>();
        qw.eq("status", "PUBLISHED");
        if (modelType != null && !modelType.isEmpty() && !"ALL".equals(modelType)) {
            qw.eq("modelType", modelType);
        }
        if (taskType != null && !taskType.isEmpty() && !"ALL".equals(taskType)) {
            qw.eq("taskType", taskType);
        }
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("name", keyword).or().like("description", keyword));
        }
        if ("rating".equals(sortBy)) {
            qw.orderByDesc("avgRating");
        } else if ("download".equals(sortBy)) {
            qw.orderByDesc("downloadCount");
        } else {
            qw.orderByDesc("publishedAt");
        }
        qw.last("LIMIT " + limit);
        return marketMapper.selectList(qw);
    }

    /**
     * 详情
     */
    public ModelEntry detail(String modelKey) {
        return marketMapper.selectOne(new QueryWrapper<ModelEntry>().eq("modelKey", modelKey));
    }

    /**
     * 评分
     */
    @Transactional
    public void rate(String modelKey, Long userId, String username, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("评分必须在 1-5 之间");
        }
        ModelEntry entry = detail(modelKey);
        if (entry == null) throw new IllegalArgumentException("模型不存在");
        ModelRating existing = ratingMapper.findUserRating(modelKey, userId);
        if (existing != null) {
            existing.setRating(rating);
            existing.setComment(comment);
            existing.setCreatedAt(LocalDateTime.now());
            ratingMapper.updateById(existing);
        } else {
            ModelRating r = ModelRating.builder()
                .modelKey(modelKey).userId(userId).username(username)
                .rating(rating).comment(comment)
                .createdAt(LocalDateTime.now()).build();
            ratingMapper.insert(r);
        }
        marketMapper.updateRatingStats(modelKey);
    }

    /**
     * 评分列表
     */
    public List<ModelRating> ratings(String modelKey, int limit) {
        if (limit <= 0) limit = 20;
        return ratingMapper.findByModelKey(modelKey, limit);
    }

    /**
     * 记录下载
     */
    public void recordDownload(String modelKey) {
        marketMapper.incrementDownload(modelKey);
    }

    /**
     * 下载 (返回文件路径)
     */
    public Path downloadPath(String modelKey) {
        ModelEntry entry = detail(modelKey);
        if (entry == null) return null;
        if (entry.getFilePath() == null || entry.getFilePath().isEmpty()) return null;
        recordDownload(modelKey);
        return Paths.get(entry.getFilePath());
    }

    /**
     * 作者的模型
     */
    public List<ModelEntry> myModels(Long authorId) {
        return marketMapper.selectList(
            new QueryWrapper<ModelEntry>().eq("authorId", authorId)
                .orderByDesc("createdAt"));
    }

    /**
     * 发布/废弃
     */
    @Transactional
    public boolean changeStatus(String modelKey, String newStatus) {
        ModelEntry entry = detail(modelKey);
        if (entry == null) return false;
        entry.setStatus(newStatus);
        entry.setUpdatedAt(LocalDateTime.now());
        if ("PUBLISHED".equals(newStatus) && entry.getPublishedAt() == null) {
            entry.setPublishedAt(LocalDateTime.now());
        }
        marketMapper.updateById(entry);
        return true;
    }

    /**
     * 统计
     */
    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        long total = marketMapper.selectCount(null);
        long published = marketMapper.selectCount(new QueryWrapper<ModelEntry>().eq("status", "PUBLISHED"));
        long draft = marketMapper.selectCount(new QueryWrapper<ModelEntry>().eq("status", "DRAFT"));
        long totalDownloads = marketMapper.selectList(null).stream()
            .mapToLong(ModelEntry::getDownloadCount).sum();
        long totalSize = marketMapper.selectList(null).stream()
            .mapToLong(e -> e.getFileSize() == null ? 0 : e.getFileSize()).sum();
        // 按类型分布
        Map<String, Long> typeDist = marketMapper.selectList(null).stream()
            .filter(e -> e.getModelType() != null)
            .collect(Collectors.groupingBy(ModelEntry::getModelType, Collectors.counting()));
        result.put("total", total);
        result.put("published", published);
        result.put("draft", draft);
        result.put("totalDownloads", totalDownloads);
        result.put("totalSize", totalSize);
        result.put("typeDistribution", typeDist);
        return result;
    }

    // ============= 工具方法 =============

    private String generateKey(String name, Long authorId) {
        String slug = name.toLowerCase()
            .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
            .replaceAll("^-|-$", "");
        if (slug.length() > 30) slug = slug.substring(0, 30);
        int hash = Math.abs((name + authorId + System.nanoTime()).hashCode()) % 100000;
        return slug + "-" + hash;
    }

    private String computeSha256(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            try (var in = Files.newInputStream(file)) {
                int n;
                while ((n = in.read(buffer)) > 0) {
                    digest.update(buffer, 0, n);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            log.warn("[model-market] SHA256 计算失败: {}", e.getMessage());
            return "";
        }
    }
}
