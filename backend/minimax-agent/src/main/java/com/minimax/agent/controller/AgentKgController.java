// =============================================================
// MiniMax - Agent 知识图谱 (KG) 控制器 (V7.0+)
//
// 路径前缀: /api/v1/agent/kg
// 数据存储: 内存 ConcurrentHashMap (生产建议迁到 Neo4j / ArangoDB)
// 用途: 供前端 KGExplorer / 知识图谱模块可视化使用
//
// 9 个端点 (与 frontend/src/api/kg.js 严格对应):
//   POST   /entities              upsertEntity
//   GET    /entities/{id}         getEntity
//   GET    /entities/search       searchEntities
//   DELETE /entities/{id}         deleteEntity
//   POST   /relations             createRelation
//   GET    /entities/{id}/neighbors    neighbors (1 跳)
//   GET    /entities/{id}/2hop         twoHopNeighbors (2 跳)
//   GET    /path                  shortestPath (BFS)
//   GET    /relations             listRelations
//
// @author general
// @since 2026-08-22
// =============================================================

package com.minimax.agent.controller;

import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Agent 知识图谱 (KG) HTTP API.
 *
 * <p>所有数据存储在内存 ConcurrentHashMap 中，启动时种 8 个示例实体 + 9 条关系。
 * 适用场景: 前端调试、演示、小规模数据 (建议 &lt; 1 万实体)。
 * 后续如需持久化，可替换为 Neo4j / ArangoDB / PostgreSQL + ltree。</p>
 */
@Slf4j
@Tag(name = "Agent 知识图谱", description = "实体 / 关系 CRUD + 邻居 + 最短路径")
@RestController
@RequestMapping("/api/v1/agent/kg")
public class AgentKgController {

    // ==================== 内存存储 ====================

    private final Map<Long, KgEntity> entities = new ConcurrentHashMap<>();
    private final Map<Long, KgRelation> relations = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    // ==================== 启动种数据 ====================

    @PostConstruct
    public void seed() {
        log.info("[AgentKG] 初始化示例数据...");
        // --- 8 个示例实体 ---
        KgEntity e1 = upsertInternal(1L, "阿尔伯特·爱因斯坦", "PERSON", "理论物理学家，相对论创始人",
                List.of("Einstein", "爱因斯坦"), 1.0);
        KgEntity e2 = upsertInternal(1L, "相对论", "THEORY", "关于时空和引力的物理理论",
                List.of("Relativity"), 0.95);
        KgEntity e3 = upsertInternal(1L, "诺贝尔物理学奖", "AWARD", "物理学界最高荣誉",
                List.of("Nobel Prize in Physics"), 0.9);
        KgEntity e4 = upsertInternal(1L, "普林斯顿大学", "ORG", "美国顶尖研究型大学",
                List.of("Princeton University"), 0.85);
        KgEntity e5 = upsertInternal(1L, "光电效应", "CONCEPT", "光照射金属释放电子的现象",
                List.of("Photoelectric Effect"), 0.8);
        KgEntity e6 = upsertInternal(1L, "玛丽·居里", "PERSON", "放射性研究先驱，两获诺贝尔奖",
                List.of("Marie Curie", "居里夫人"), 0.95);
        KgEntity e7 = upsertInternal(1L, "镭", "ELEMENT", "放射性化学元素，符号 Ra",
                List.of("Radium"), 0.7);
        KgEntity e8 = upsertInternal(1L, "巴黎大学", "ORG", "法国著名综合性大学",
                List.of("University of Paris", "索邦"), 0.75);

        // --- 9 条示例关系 ---
        createRelationInternal(1L, e1.getId(), e2.getId(), "提出", "爱因斯坦提出相对论", 1.0);
        createRelationInternal(1L, e1.getId(), e3.getId(), "获得", "1921 年获诺贝尔物理学奖", 1.0);
        createRelationInternal(1L, e1.getId(), e4.getId(), "任职", "在普林斯顿高等研究院工作", 0.9);
        createRelationInternal(1L, e1.getId(), e5.getId(), "解释", "用光量子假说解释光电效应", 0.95);
        createRelationInternal(1L, e6.getId(), e7.getId(), "发现", "居里夫人发现镭元素", 1.0);
        createRelationInternal(1L, e6.getId(), e3.getId(), "获得", "1903 年与丈夫共同获诺贝尔物理学奖", 1.0);
        createRelationInternal(1L, e6.getId(), e8.getId(), "任职", "在巴黎大学任教", 0.85);
        createRelationInternal(1L, e3.getId(), e5.getId(), "表彰", "光电效应获诺贝尔奖表彰", 0.6);
        createRelationInternal(1L, e1.getId(), e6.getId(), "同时代", "两位科学家生活在同一时代", 0.5);

        log.info("[AgentKG] 示例数据已加载: {} 个实体, {} 条关系", entities.size(), relations.size());
    }

    // ==================== 1. upsertEntity ====================

    @Operation(summary = "创建或更新实体 (按 name + userId 判定 upsert)")
    @PostMapping("/entities")
    public Result<KgEntity> upsertEntity(@RequestBody Map<String, Object> body) {
        if (body == null) {
            return Result.fail(400, "请求体不能为空");
        }
        Long userId = body.get("userId") == null ? 1L : toLong(body.get("userId"));
        String name = (String) body.get("name");
        if (name == null || name.isBlank()) {
            return Result.fail(400, "name 不能为空");
        }
        String type = body.get("type") == null ? "OTHER" : String.valueOf(body.get("type"));
        String description = body.get("description") == null ? "" : String.valueOf(body.get("description"));
        @SuppressWarnings("unchecked")
        List<String> rawAliases = (List<String>) body.get("aliases");
        List<String> aliases = rawAliases == null ? new ArrayList<>() : new ArrayList<>(rawAliases);
        Double importance = body.get("importance") == null ? null : toDouble(body.get("importance"));

        // 按 (userId, name) 查找已有
        KgEntity existing = entities.values().stream()
                .filter(e -> Objects.equals(e.getUserId(), userId))
                .filter(e -> Objects.equals(e.getName(), name))
                .findFirst().orElse(null);
        if (existing != null) {
            existing.setType(type);
            existing.setDescription(description);
            existing.setAliases(aliases);
            existing.setImportance(importance);
            log.info("[AgentKG] 更新实体 id={} name={}", existing.getId(), name);
            return Result.ok(toEntityView(existing));
        }

        KgEntity created = upsertInternal(userId, name, type, description, aliases, importance);
        log.info("[AgentKG] 新建实体 id={} name={}", created.getId(), name);
        return Result.ok(toEntityView(created));
    }

    // ==================== 2. getEntity ====================

    @Operation(summary = "获取实体详情")
    @GetMapping("/entities/{id}")
    public Result<KgEntity> getEntity(@PathVariable Long id,
                                       @Parameter(description = "可选, 校验 userId 归属")
                                       @RequestParam(required = false) Long userId) {
        KgEntity e = entities.get(id);
        if (e == null) {
            return Result.fail(404, "实体不存在: id=" + id);
        }
        if (userId != null && !Objects.equals(e.getUserId(), userId)) {
            return Result.fail(404, "实体不存在或不属于该用户: id=" + id);
        }
        return Result.ok(toEntityView(e));
    }

    // ==================== 3. searchEntities ====================

    @Operation(summary = "搜索实体 (按 name / aliases 模糊匹配)")
    @GetMapping("/entities/search")
    public Result<Map<String, Object>> searchEntities(
            @RequestParam(required = false, defaultValue = "1") Long userId,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        int max = Math.max(1, Math.min(limit, 200));

        List<KgEntity> hits;
        if (kw.isEmpty()) {
            // 空 keyword 返回该 userId 下所有实体 (按 importance 倒序)
            hits = entities.values().stream()
                    .filter(e -> Objects.equals(e.getUserId(), userId))
                    .sorted(Comparator.comparing(KgEntity::getImportance,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(max)
                    .collect(Collectors.toList());
        } else {
            hits = entities.values().stream()
                    .filter(e -> Objects.equals(e.getUserId(), userId))
                    .filter(e -> {
                        if (e.getName() != null && e.getName().toLowerCase(Locale.ROOT).contains(kw)) {
                            return true;
                        }
                        if (e.getDescription() != null && e.getDescription().toLowerCase(Locale.ROOT).contains(kw)) {
                            return true;
                        }
                        if (e.getAliases() != null) {
                            for (String a : e.getAliases()) {
                                if (a != null && a.toLowerCase(Locale.ROOT).contains(kw)) return true;
                            }
                        }
                        return false;
                    })
                    .limit(max)
                    .collect(Collectors.toList());
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("list", hits.stream().map(this::toEntityView).collect(Collectors.toList()));
        resp.put("total", hits.size());
        return Result.ok(resp);
    }

    // ==================== 4. deleteEntity ====================

    @Operation(summary = "删除实体 (级联删除其全部关系)")
    @DeleteMapping("/entities/{id}")
    public Result<Map<String, Object>> deleteEntity(@PathVariable Long id,
                                                     @RequestParam(required = false) Long userId) {
        KgEntity e = entities.get(id);
        if (e == null) {
            return Result.fail(404, "实体不存在: id=" + id);
        }
        if (userId != null && !Objects.equals(e.getUserId(), userId)) {
            return Result.fail(404, "实体不存在或不属于该用户: id=" + id);
        }
        // 级联删除关系
        List<Long> relIdsToRemove = relations.values().stream()
                .filter(r -> Objects.equals(r.getFromId(), id) || Objects.equals(r.getToId(), id))
                .map(KgRelation::getId)
                .collect(Collectors.toList());
        for (Long rid : relIdsToRemove) {
            relations.remove(rid);
        }
        entities.remove(id);
        log.info("[AgentKG] 删除实体 id={} name={}, 联动删除 {} 条关系",
                id, e.getName(), relIdsToRemove.size());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("id", id);
        resp.put("cascadedRelations", relIdsToRemove.size());
        return Result.ok(resp);
    }

    // ==================== 5. createRelation ====================

    @Operation(summary = "创建实体关系")
    @PostMapping("/relations")
    public Result<Map<String, Object>> createRelation(@RequestBody Map<String, Object> body) {
        if (body == null) {
            return Result.fail(400, "请求体不能为空");
        }
        Long userId = body.get("userId") == null ? 1L : toLong(body.get("userId"));
        Long fromId = body.get("fromId") == null ? null : toLong(body.get("fromId"));
        Long toId = body.get("toId") == null ? null : toLong(body.get("toId"));
        if (fromId == null || toId == null) {
            return Result.fail(400, "fromId / toId 不能为空");
        }
        if (Objects.equals(fromId, toId)) {
            return Result.fail(400, "fromId 与 toId 不能相同 (自环关系暂不支持)");
        }
        if (!entities.containsKey(fromId) || !entities.containsKey(toId)) {
            return Result.fail(404, "fromId 或 toId 对应的实体不存在");
        }
        String type = body.get("type") == null ? "关联" : String.valueOf(body.get("type"));
        String description = body.get("description") == null ? "" : String.valueOf(body.get("description"));
        Double weight = body.get("weight") == null ? 1.0 : toDouble(body.get("weight"));

        KgRelation r = createRelationInternal(userId, fromId, toId, type, description, weight);
        log.info("[AgentKG] 新建关系 id={} {} -[{}]-> {}", r.getId(), fromId, type, toId);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", r.getId());
        resp.put("fromId", fromId);
        resp.put("toId", toId);
        resp.put("type", type);
        resp.put("weight", weight);
        resp.put("description", description);
        resp.put("createdAt", r.getCreatedAt().toString());
        return Result.ok(resp);
    }

    // ==================== 6. neighbors (1 跳) ====================

    @Operation(summary = "获取实体的 1 跳邻居 (出 + 入)")
    @GetMapping("/entities/{id}/neighbors")
    public Result<Map<String, Object>> neighbors(@PathVariable Long id,
                                                  @RequestParam(required = false) Long userId) {
        KgEntity center = entities.get(id);
        if (center == null) {
            return Result.fail(404, "实体不存在: id=" + id);
        }
        if (userId != null && !Objects.equals(center.getUserId(), userId)) {
            return Result.fail(404, "实体不存在或不属于该用户");
        }

        // 1 跳邻居 (按 userId 过滤)
        List<KgRelation> incident = relations.values().stream()
                .filter(r -> userId == null || isRelInUser(r, userId))
                .filter(r -> Objects.equals(r.getFromId(), id) || Objects.equals(r.getToId(), id))
                .collect(Collectors.toList());

        Set<Long> neighborIds = new LinkedHashSet<>();
        for (KgRelation r : incident) {
            if (Objects.equals(r.getFromId(), id)) neighborIds.add(r.getToId());
            else neighborIds.add(r.getFromId());
        }
        List<KgEntity> neighborEntities = neighborIds.stream()
                .map(entities::get)
                .filter(Objects::nonNull)
                .map(this::toEntityView)
                .collect(Collectors.toList());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("entities", neighborEntities);
        resp.put("relations", incident);
        return Result.ok(resp);
    }

    // ==================== 7. twoHopNeighbors (2 跳) ====================

    @Operation(summary = "获取实体的 2 跳邻居 (含中间实体)")
    @GetMapping("/entities/{id}/2hop")
    public Result<Map<String, Object>> twoHopNeighbors(@PathVariable Long id,
                                                       @RequestParam(required = false) Long userId) {
        KgEntity center = entities.get(id);
        if (center == null) {
            return Result.fail(404, "实体不存在: id=" + id);
        }
        if (userId != null && !Objects.equals(center.getUserId(), userId)) {
            return Result.fail(404, "实体不存在或不属于该用户");
        }

        Set<Long> hop1 = new HashSet<>();
        List<KgRelation> hop1Rels = new ArrayList<>();
        for (KgRelation r : relations.values()) {
            if (userId != null && !isRelInUser(r, userId)) continue;
            if (Objects.equals(r.getFromId(), id)) {
                hop1.add(r.getToId());
                hop1Rels.add(r);
            } else if (Objects.equals(r.getToId(), id)) {
                hop1.add(r.getFromId());
                hop1Rels.add(r);
            }
        }

        Set<Long> hop2 = new HashSet<>();
        List<KgRelation> hop2Rels = new ArrayList<>();
        for (Long mid : hop1) {
            for (KgRelation r : relations.values()) {
                if (userId != null && !isRelInUser(r, userId)) continue;
                if (Objects.equals(r.getId(), 0L)) continue; // 防御性
                Long other = null;
                if (Objects.equals(r.getFromId(), mid) && !Objects.equals(r.getToId(), id)) {
                    other = r.getToId();
                } else if (Objects.equals(r.getToId(), mid) && !Objects.equals(r.getFromId(), id)) {
                    other = r.getFromId();
                }
                if (other != null && !Objects.equals(other, id) && !hop1.contains(other)) {
                    hop2.add(other);
                }
                if (other != null) {
                    hop2Rels.add(r);
                }
            }
        }

        Set<Long> allIds = new LinkedHashSet<>();
        allIds.add(id);
        allIds.addAll(hop1);
        allIds.addAll(hop2);
        List<KgEntity> entityList = allIds.stream()
                .map(entities::get)
                .filter(Objects::nonNull)
                .map(this::toEntityView)
                .collect(Collectors.toList());

        List<KgRelation> allRels = new ArrayList<>();
        allRels.addAll(hop1Rels);
        allRels.addAll(hop2Rels);
        // 去重
        List<KgRelation> dedupRels = allRels.stream()
                .distinct()
                .collect(Collectors.toList());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("entities", entityList);
        resp.put("relations", dedupRels);
        return Result.ok(resp);
    }

    // ==================== 8. shortestPath (BFS) ====================

    @Operation(summary = "BFS 最短路径 (按关系 hop 数)")
    @GetMapping("/path")
    public Result<Map<String, Object>> shortestPath(
            @RequestParam Long fromId,
            @RequestParam Long toId,
            @RequestParam(required = false) Long userId) {
        if (fromId == null || toId == null) {
            return Result.fail(400, "fromId / toId 不能为空");
        }
        if (Objects.equals(fromId, toId)) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("path", List.of(fromId));
            resp.put("length", 0);
            return Result.ok(resp);
        }
        if (!entities.containsKey(fromId) || !entities.containsKey(toId)) {
            return Result.fail(404, "fromId 或 toId 对应的实体不存在");
        }

        // BFS
        Queue<Long> queue = new ArrayDeque<>();
        Map<Long, Long> prev = new HashMap<>();
        Set<Long> visited = new HashSet<>();
        queue.add(fromId);
        visited.add(fromId);
        boolean found = false;
        while (!queue.isEmpty()) {
            Long cur = queue.poll();
            if (Objects.equals(cur, toId)) {
                found = true;
                break;
            }
            for (KgRelation r : relations.values()) {
                if (userId != null && !isRelInUser(r, userId)) continue;
                Long next = null;
                if (Objects.equals(r.getFromId(), cur)) next = r.getToId();
                else if (Objects.equals(r.getToId(), cur)) next = r.getFromId();
                if (next != null && !visited.contains(next)) {
                    visited.add(next);
                    prev.put(next, cur);
                    if (Objects.equals(next, toId)) {
                        found = true;
                        break;
                    }
                    queue.add(next);
                }
            }
            if (found) break;
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        if (!found) {
            resp.put("path", Collections.emptyList());
            resp.put("length", -1);
            resp.put("found", false);
            return Result.ok(resp);
        }
        // 还原路径
        LinkedList<Long> path = new LinkedList<>();
        Long step = toId;
        while (step != null) {
            path.addFirst(step);
            if (Objects.equals(step, fromId)) break;
            step = prev.get(step);
        }
        resp.put("path", path);
        resp.put("length", path.size() - 1);
        resp.put("found", true);
        return Result.ok(resp);
    }

    // ==================== 9. listRelations ====================

    @Operation(summary = "列出关系 (可按 entityId 过滤, 即与该实体相关的全部关系)")
    @GetMapping("/relations")
    public Result<Map<String, Object>> listRelations(
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) Long userId) {
        List<KgRelation> all = relations.values().stream()
                .filter(r -> entityId == null
                        || Objects.equals(r.getFromId(), entityId)
                        || Objects.equals(r.getToId(), entityId))
                .filter(r -> userId == null || isRelInUser(r, userId))
                .sorted(Comparator.comparing(KgRelation::getCreatedAt))
                .collect(Collectors.toList());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("list", all);
        resp.put("total", all.size());
        return Result.ok(resp);
    }

    // ==================== 内部工具方法 ====================

    /**
     * 关系是否属于指定 userId (两端实体任一归属该 userId 即可)。
     */
    private boolean isRelInUser(KgRelation r, Long userId) {
        KgEntity from = entities.get(r.getFromId());
        KgEntity to = entities.get(r.getToId());
        boolean fromOk = from != null && Objects.equals(from.getUserId(), userId);
        boolean toOk = to != null && Objects.equals(to.getUserId(), userId);
        return fromOk || toOk;
    }

    /**
     * 内部 upsert: 分配 id 并写入存储 (seed 阶段也用)。
     */
    private KgEntity upsertInternal(Long userId, String name, String type,
                                    String description, List<String> aliases, Double importance) {
        Long id = nextId.getAndIncrement();
        KgEntity e = new KgEntity();
        e.setId(id);
        e.setUserId(userId);
        e.setName(name);
        e.setType(type);
        e.setDescription(description);
        e.setAliases(aliases);
        e.setImportance(importance);
        e.setCreatedAt(LocalDateTime.now());
        entities.put(id, e);
        return e;
    }

    /**
     * 内部创建关系: 分配 id 并写入存储。
     */
    private KgRelation createRelationInternal(Long userId, Long fromId, Long toId,
                                              String type, String description, Double weight) {
        Long id = nextId.getAndIncrement();
        KgRelation r = new KgRelation();
        r.setId(id);
        r.setUserId(userId);
        r.setFromId(fromId);
        r.setToId(toId);
        r.setType(type);
        r.setDescription(description);
        r.setWeight(weight);
        r.setCreatedAt(LocalDateTime.now());
        relations.put(id, r);
        return r;
    }

    /**
     * 转返回视图: id/name/type/description/createdAt (与 kg.js upsertEntity 注释一致)。
     */
    private KgEntity toEntityView(KgEntity e) {
        KgEntity v = new KgEntity();
        v.setId(e.getId());
        v.setName(e.getName());
        v.setType(e.getType());
        v.setDescription(e.getDescription());
        v.setCreatedAt(e.getCreatedAt());
        return v;
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Double toDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // ==================== 内部数据模型 ====================

    @Data
    public static class KgEntity {
        private Long id;
        private Long userId;
        private String name;
        private String type;
        private String description;
        private List<String> aliases;
        private Double importance;
        private LocalDateTime createdAt;
    }

    @Data
    public static class KgRelation {
        private Long id;
        private Long userId;
        private Long fromId;
        private Long toId;
        private String type;
        private String description;
        private Double weight;
        private LocalDateTime createdAt;
    }
}
