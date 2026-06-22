package com.minimax.pipeline.controller;

import com.minimax.common.result.Result;
import com.minimax.pipeline.service.WorkflowService;
import com.minimax.pipeline.vo.RunDetailVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Run Controller (V5.32) - 2 个端点
 */
@Tag(name = "工作流执行 (V5.32)")
@RestController
@RequestMapping("/api/v1/pipeline/runs")
@RequiredArgsConstructor
public class RunController {

    private final WorkflowService workflowService;

    @Operation(summary = "执行详情 (含每节点状态)")
    @GetMapping("/{runId}")
    public Result<RunDetailVo> getRun(@PathVariable Long runId) {
        return Result.ok(workflowService.getRunDetail(runId));
    }

    @Operation(summary = "获取输出节点数据 (前 100 行)")
    @GetMapping("/{runId}/result")
    public Result<List<Map<String, Object>>> getResult(@PathVariable Long runId,
                                                       @RequestParam String outputNodeId) {
        return Result.ok(workflowService.getRunResult(runId, outputNodeId));
    }
}
