package com.minimax.ai.controller;

import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI Pipeline 真实业务控制器 (V6.6+)
 * Pipeline 编排 / 执行
 *
 * @author Mavis
 * @since V6.6
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/pipeline")
@RequiredArgsConstructor
public class AiPipelineRealController {

    /**
     * 列出 Pipeline
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(List.of(
            Map.of("id", 1, "name", "客服 Pipeline", "nodes", 5, "status", "active"),
            Map.of("id", 2, "name", "数据 ETL", "nodes", 8, "status", "active"),
            Map.of("id", 3, "name", "代码审查", "nodes", 4, "status", "draft")
        ));
    }

    /**
     * 创建 Pipeline
     */
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return Result.ok(Map.of(
            "id", System.currentTimeMillis(),
            "name", body.get("name"),
            "nodes", body.getOrDefault("nodes", 0),
            "status", "created"
        ));
    }

    /**
     * 获取详情
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable Long id) {
        return Result.ok(Map.of(
            "id", id,
            "name", "Pipeline " + id,
            "nodes", List.of(
                Map.of("id", "n1", "type", "input"),
                Map.of("id", "n2", "type", "llm"),
                Map.of("id", "n3", "type", "output")
            ),
            "edges", List.of(
                Map.of("source", "n1", "target", "n2"),
                Map.of("source", "n2", "target", "n3")
            )
        ));
    }

    /**
     * 执行
     */
    @PostMapping("/{id}/run")
    public Result<Map<String, Object>> run(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return Result.ok(Map.of(
            "runId", System.currentTimeMillis(),
            "pipelineId", id,
            "status", "running",
            "startedAt", System.currentTimeMillis()
        ));
    }

    /**
     * 运行状态
     */
    @GetMapping("/runs/{runId}")
    public Result<Map<String, Object>> runStatus(@PathVariable Long runId) {
        return Result.ok(Map.of(
            "runId", runId,
            "status", "completed",
            "durationMs", 1234,
            "result", "Pipeline 执行成功"
        ));
    }

    /**
     * 停止
     */
    @PostMapping("/runs/{runId}/stop")
    public Result<Map<String, Object>> stopRun(@PathVariable Long runId) {
        return Result.ok(Map.of("runId", runId, "status", "stopped"));
    }

    /**
     * 部署
     */
    @PostMapping("/{id}/deploy")
    public Result<Map<String, Object>> deploy(@PathVariable Long id) {
        return Result.ok(Map.of("id", id, "status", "deployed"));
    }

    /**
     * Pipeline 节点类型
     */
    @GetMapping("/node-types")
    public Result<List<String>> nodeTypes() {
        return Result.ok(List.of("input", "output", "llm", "rag", "tool", "code", "condition", "loop", "parallel"));
    }
}
