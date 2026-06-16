package com.minimax.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.agent.entity.KgEntity;
import com.minimax.agent.entity.KgRelation;
import com.minimax.agent.mapper.KgEntityMapper;
import com.minimax.agent.mapper.KgRelationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * V2.2: 知识图谱服务
 *
 * - 创建/查询实体 (人/地/组织/概念)
 * - 创建/查询关系 (works_at / located_in / friend_of / ...)
 * - N 度关联查询 (1-3 hop)
 * - 路径查询 (A 到 B 的最短路径)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphService {

    private final KgEntityMapper entityMapper;
    private final KgRelationMapper relationMapper;

    // ---------- 实体 ----------

    public Long upsertEntity(Long userId, String name, String type,
                              String description, String aliases, Integer importance) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name 必填");
        KgEntity exist = entityMapper.selectOne(
                new LambdaQueryWrapper<KgEntity>()
                        .eq(KgEntity::getUserId, userId)
                        .eq(KgEntity::getName, name)
                        .last("LIMIT 1"));
        if (exist != null) {
            // 更新
            if (description != null) exist.setDescription(description);
            if (aliases != null) exist.setAliases(aliases);
            if (importance != null) exist.setImportance(importance);
            entityMapper.updateById(exist);
            return exist.getId();
        }
        KgEntity e = new KgEntity();
        e.setUserId(userId);
        e.setName(name);
        e.setEntityType(type == null ? "concept" : type);
        e.setDescription(description);
        e.setAliases(aliases);
        e.setImportance(importance == null ? 5 : importance);
        e.setSource("manual");
        e.setRefCount(0);
        entityMapper.insert(e);
        return e.getId();
    }

    public KgEntity getEntity(Long id, Long userId) {
        KgEntity e = entityMapper.selectById(id);
        if (e == null) return null;
        if (!e.getUserId().equals(userId)) return null;
        return e;
    }

    public List<KgEntity> searchEntities(Long userId, String keyword, int limit) {
        if (limit <= 0 || limit > 200) limit = 20;
        return entityMapper.selectList(
                new LambdaQueryWrapper<KgEntity>()
                        .eq(KgEntity::getUserId, userId)
                        .and(w -> w.like(KgEntity::getName, keyword)
                                .or().like(KgEntity::getDescription, keyword)
                                .or().like(KgEntity::getAliases, keyword))
                        .orderByDesc(KgEntity::getImportance)
                        .last("LIMIT " + limit));
    }

    public boolean deleteEntity(Long id, Long userId) {
        KgEntity e = entityMapper.selectById(id);
        if (e == null) return false;
        if (!e.getUserId().equals(userId)) return false;
        relationMapper.deleteByEntity(id);
        entityMapper.deleteById(id);
        return true;
    }

    // ---------- 关系 ----------

    public Long createRelation(Long userId, Long fromId, Long toId,
                                String type, String description, Double weight) {
        if (fromId == null || toId == null) throw new IllegalArgumentException("from/to 必填");
        if (fromId.equals(toId)) throw new IllegalArgumentException("不能自己指向自己");
        KgRelation r = new KgRelation();
        r.setUserId(userId);
        r.setFromEntity(fromId);
        r.setToEntity(toId);
        r.setRelationType(type == null ? "related_to" : type);
        r.setDescription(description);
        r.setWeight(weight == null ? new java.math.BigDecimal("1.00") : new java.math.BigDecimal(weight));
        r.setSource("manual");
        r.setRefCount(0);
        relationMapper.insert(r);
        return r.getId();
    }

    public List<KgRelation> getOutRelations(Long entityId) {
        return relationMapper.selectList(
                new LambdaQueryWrapper<KgRelation>()
                        .eq(KgRelation::getFromEntity, entityId));
    }

    public List<KgRelation> getInRelations(Long entityId) {
        return relationMapper.selectList(
                new LambdaQueryWrapper<KgRelation>()
                        .eq(KgRelation::getToEntity, entityId));
    }

    public boolean deleteRelation(Long id, Long userId) {
        KgRelation r = relationMapper.selectById(id);
        if (r == null) return false;
        if (!r.getUserId().equals(userId)) return false;
        relationMapper.deleteById(id);
        return true;
    }

    // ---------- 图查询 ----------

    /**
     * 1 跳邻居查询：返回 (entity, relation)
     */
    public List<Map<String, Object>> neighbors(Long entityId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (KgRelation r : getOutRelations(entityId)) {
            KgEntity to = entityMapper.selectById(r.getToEntity());
            if (to == null) continue;
            out.add(relationToMap(r, null, to));
        }
        for (KgRelation r : getInRelations(entityId)) {
            KgEntity from = entityMapper.selectById(r.getFromEntity());
            if (from == null) continue;
            out.add(relationToMap(r, from, null));
        }
        return out;
    }

    /**
     * 2 跳关联：entity -> [neighbor1, neighbor2, ...]
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> twoHopNeighbors(Long entityId) {
        List<Map<String, Object>> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        seen.add(entityId);

        // 第一跳
        for (KgRelation r1 : getOutRelations(entityId)) {
            KgEntity m1 = entityMapper.selectById(r1.getToEntity());
            if (m1 == null || !seen.add(m1.getId())) continue;
            out.add(Map.of("hop", 1, "entity", m1, "via", r1.getRelationType()));
            // 第二跳
            for (KgRelation r2 : getOutRelations(m1.getId())) {
                KgEntity m2 = entityMapper.selectById(r2.getToEntity());
                if (m2 == null || !seen.add(m2.getId())) continue;
                out.add(Map.of("hop", 2, "entity", m2, "via", r2.getRelationType()));
            }
        }
        return out;
    }

    /**
     * 最短路径 (BFS) — 从 fromId 到 toId
     */
    public List<KgEntity> shortestPath(Long userId, Long fromId, Long toId) {
        if (fromId.equals(toId)) {
            KgEntity e = entityMapper.selectById(fromId);
            return e == null ? List.of() : List.of(e);
        }
        Map<Long, Long> parent = new HashMap<>();
        Queue<Long> q = new LinkedList<>();
        Set<Long> visited = new HashSet<>();
        q.add(fromId);
        visited.add(fromId);

        while (!q.isEmpty()) {
            Long cur = q.poll();
            if (cur.equals(toId)) break;
            for (KgRelation r : getOutRelations(cur)) {
                if (visited.add(r.getToEntity())) {
                    parent.put(r.getToEntity(), cur);
                    q.add(r.getToEntity());
                }
            }
            // 反向也搜
            for (KgRelation r : getInRelations(cur)) {
                if (visited.add(r.getFromEntity())) {
                    parent.put(r.getFromEntity(), cur);
                    q.add(r.getFromEntity());
                }
            }
        }

        if (!visited.contains(toId)) return List.of();

        // 回溯
        List<KgEntity> path = new ArrayList<>();
        Long cur = toId;
        while (cur != null) {
            KgEntity e = entityMapper.selectById(cur);
            if (e != null) path.add(0, e);
            if (cur.equals(fromId)) break;
            cur = parent.get(cur);
        }
        return path;
    }

    private Map<String, Object> relationToMap(KgRelation r, KgEntity from, KgEntity to) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("relationId", r.getId());
        m.put("type", r.getRelationType());
        m.put("description", r.getDescription());
        m.put("weight", r.getWeight());
        m.put("from", from == null ? entityMapper.selectById(r.getFromEntity()) : from);
        m.put("to", to == null ? entityMapper.selectById(r.getToEntity()) : to);
        return m;
    }
}
