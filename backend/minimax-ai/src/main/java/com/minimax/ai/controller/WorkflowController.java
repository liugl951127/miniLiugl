package com.minimax.ai.controller;

import com.minimax.ai.generation.WorkflowEngine;
import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * AI 工作流 API (V2.7.3)
 *
 * 端点:
 *   POST /api/ai/workflow/execute - 执行工作流
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowEngine workflowEngine;

    @PostMapping("/execute")
    public Result<WorkflowEngine.WorkflowResult> execute(@RequestBody WorkflowEngine.Workflow workflow,
                                                         @RequestBody(required = false) Map<String, Object> context) {
        if (workflow == null) return Result.fail("工作流不能为空");
        try {
            WorkflowEngine.WorkflowResult r = workflowEngine.execute(workflow, context);
            return Result.ok(r);
        } catch (Exception e) {
            log.error("Workflow execution failed", e);
            return Result.fail("工作流执行失败: " + e.getMessage());
        }
    }

    /**
     * 验证工作流 (只检查拓扑结构, 不执行)
     */
    @PostMapping("/validate")
    public Result<Map<String, Object>> validate(@RequestBody WorkflowEngine.Workflow workflow) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        if (workflow == null) {
            r.put("valid", false);
            r.put("error", "工作流为空");
            return Result.ok(r);
        }
        // 检查: 节点 id 唯一
        Set<String> ids = new HashSet<>();
        for (WorkflowEngine.Node n : workflow.nodes) {
            if (!ids.add(n.id)) {
                r.put("valid", false);
                r.put("error", "重复节点 ID: " + n.id);
                return Result.ok(r);
            }
        }
        // 检查: 边引用存在
        for (WorkflowEngine.Edge e : workflow.edges) {
            if (!ids.contains(e.from) || !ids.contains(e.to)) {
                r.put("valid", false);
                r.put("error", "边引用不存在: " + e.from + " -> " + e.to);
                return Result.ok(r);
            }
        }
        r.put("valid", true);
        r.put("nodeCount", workflow.nodes.size());
        r.put("edgeCount", workflow.edges.size());
        return Result.ok(r);
    }
}
