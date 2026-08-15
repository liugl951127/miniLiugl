package com.minimax.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Agent 工作流存储服务 (V6.8.1+)
 *
 * 职责: 管理 Agent 画布节点的保存/列表/读取/删除。
 * 当前为内存存储（Map），生产环境替换为 MySQL + Redis。
 *
 * 前端 Canvas.vue / Index.vue 通过此服务存取工作流。
 */
@Slf4j
@Service
public class AgentWorkflowService {

    /** 内存存储: id → WorkflowRecord */
    private final Map<Long, WorkflowRecord> store = new ConcurrentHashMap<>();
    private volatile long idCounter = 1L;

    // ==================== 数据模型 ====================

    @lombok.Data
    public static class WorkflowRecord {
        public Long id;
        public Long userId;
        public String name;
        public String description;
        public List<Map<String, Object>> nodes;  // 画布节点
        public List<Map<String, Object>> edges;  // 画布连线
        public String status;                     // DRAFT / PUBLISHED
        public Long createdAt;
        public Long updatedAt;

        public WorkflowRecord() {}

        public WorkflowRecord(Long id, Long userId, String name, String description,
                             List<Map<String, Object>> nodes, List<Map<String, Object>> edges,
                             String status, Long createdAt, Long updatedAt) {
            this.id = id;
            this.userId = userId;
            this.name = name;
            this.description = description;
            this.nodes = nodes;
            this.edges = edges;
            this.status = status;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("userId", userId);
            m.put("name", name);
            m.put("description", description);
            m.put("nodes", nodes);
            m.put("edges", edges);
            m.put("status", status);
            m.put("createdAt", createdAt);
            m.put("updatedAt", updatedAt);
            return m;
        }
    }

    // ==================== CRUD ====================

    /**
     * 保存（新建或更新）工作流
     */
    public WorkflowRecord save(WorkflowRecord wf) {
        long now = System.currentTimeMillis();
        if (wf.id == null) {
            wf.id = idCounter++;
            wf.createdAt = now;
        }
        wf.updatedAt = now;
        store.put(wf.id, wf);
        log.info("[Workflow] 保存 id={} name={}", wf.id, wf.name);
        return wf;
    }

    /**
     * 查询列表（分页）
     */
    public List<WorkflowRecord> list(Long userId, int page, int size) {
        return store.values().stream()
                .filter(w -> userId == null || userId.equals(w.userId))
                .sorted((a, b) -> Long.compare(
                        b.updatedAt != null ? b.updatedAt : 0L,
                        a.updatedAt != null ? a.updatedAt : 0L))
                .skip((long) (page - 1) * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    /**
     * 统计总数
     */
    public long count(Long userId) {
        return store.values().stream()
                .filter(w -> userId == null || userId.equals(w.userId))
                .count();
    }

    /**
     * 获取单个
     */
    public WorkflowRecord get(Long id) {
        return store.get(id);
    }

    /**
     * 删除
     */
    public boolean delete(Long id) {
        WorkflowRecord removed = store.remove(id);
        if (removed != null) {
            log.info("[Workflow] 删除 id={}", id);
            return true;
        }
        return false;
    }
}
