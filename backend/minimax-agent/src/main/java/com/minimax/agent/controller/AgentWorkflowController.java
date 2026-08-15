package com.minimax.agent.controller;

import com.minimax.agent.service.AgentWorkflowService;
import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent 工作流控制器 (V6.8.1+)
 *
 * 对应前端 Canvas.vue / Index.vue 的工作流存取需求：
 *   - GET  /agent/workflows        — 列出我的工作流
 *   - GET  /agent/workflows/{id} — 获取单个工作流
 *   - POST /agent/workflows       — 保存（新建/更新）工作流
 *   - DELETE /agent/workflows/{id} — 删除工作流
 *
 * 存储：内存 Map（生产换 MySQL）
 *
 * @since 2026-08-12
 */
@Slf4j
@Tag(name = "Agent工作流", description = "画布节点/连线的存取管理")
@RestController
@RequestMapping("/api/v1/agent/workflows")
@RequiredArgsConstructor
public class AgentWorkflowController {

    private final AgentWorkflowService workflowService;

    // ==================== List ====================

    @Operation(summary = "工作流列表 (分页)")
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "50") int limit) {
        // 前端传的是 limit，兼容两种参数名
        int effectiveLimit = limit < 50 ? 50 : Math.min(limit, 200);
        int effectivePage = Math.max(1, page);

        List<AgentWorkflowService.WorkflowRecord> records = workflowService.list(userId, effectivePage, effectiveLimit);
        long total = workflowService.count(userId);

        List<Map<String, Object>> items = records.stream()
                .map(AgentWorkflowService.WorkflowRecord::toMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", items);
        result.put("total", total);
        result.put("page", effectivePage);
        result.put("size", effectiveLimit);
        return Result.ok(result);
    }

    // ==================== Get ====================

    @Operation(summary = "获取单个工作流")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable Long id) {
        AgentWorkflowService.WorkflowRecord wf = workflowService.get(id);
        if (wf == null) {
            return Result.fail(404, "工作流不存在: " + id);
        }
        return Result.ok(wf.toMap());
    }

    // ==================== Save ====================

    @Operation(summary = "保存工作流 (新建或更新)")
    @PostMapping
    public Result<Map<String, Object>> save(@RequestBody Map<String, Object> body) {
        Long userId = parseUserId(body.get("userId"));
        String name = (String) body.getOrDefault("name", "未命名工作流");
        String description = (String) body.getOrDefault("description", "");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) body.get("nodes");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> edges = (List<Map<String, Object>>) body.get("edges");

        AgentWorkflowService.WorkflowRecord wf = new AgentWorkflowService.WorkflowRecord();
        wf.setUserId(userId);
        wf.setName(name);
        wf.setDescription(description);
        wf.setNodes(nodes != null ? nodes : Collections.emptyList());
        wf.setEdges(edges != null ? edges : Collections.emptyList());
        wf.setStatus((String) body.getOrDefault("status", "DRAFT"));

        // 前端传 id 表示更新，不传表示新建
        Object idObj = body.get("id");
        if (idObj != null) wf.setId(((Number) idObj).longValue());

        AgentWorkflowService.WorkflowRecord saved = workflowService.save(wf);
        return Result.ok(saved.toMap());
    }

    // ==================== Delete ====================

    @Operation(summary = "删除工作流")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = workflowService.delete(id);
        if (!ok) return Result.fail(404, "工作流不存在: " + id);
        return Result.ok();
    }

    // ==================== Utils ====================

    private Long parseUserId(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(obj.toString()); }
        catch (Exception e) { return null; }
    }
}
