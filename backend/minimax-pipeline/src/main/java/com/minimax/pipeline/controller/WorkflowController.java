package com.minimax.pipeline.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minimax.common.result.Result;
import com.minimax.pipeline.dto.WorkflowDto;
import com.minimax.pipeline.service.WorkflowService;
import com.minimax.pipeline.vo.DagValidationVo;
import com.minimax.pipeline.vo.WorkflowDetailVo;
import com.minimax.pipeline.vo.WorkflowVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工作流 Controller (V5.32) - 8 个端点
 */
@Tag(name = "画布工作流 (V5.32)")
@RestController
// V1.9.1: 改为相对路径, gateway StripPrefix=2 后转发到 /pipeline/workflows/...
@RequestMapping("/pipeline/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @Operation(summary = "新建工作流")
    @PostMapping
    public Result<Long> create(@AuthenticationPrincipal Long userId, @RequestBody @Valid WorkflowDto dto) {
        return Result.ok(workflowService.create(dto, userId));
    }

    @Operation(summary = "工作流列表 (分页)")
    @GetMapping
    public Result<Page<WorkflowVo>> list(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(workflowService.list(page, size, keyword));
    }

    @Operation(summary = "工作流详情 (含 definition)")
    @GetMapping("/{id}")
    public Result<WorkflowDetailVo> getById(@PathVariable Long id) {
        return Result.ok(workflowService.getById(id));
    }

    @Operation(summary = "更新工作流 (自动 +version, 写版本历史)")
    @PutMapping("/{id}")
    public Result<Void> update(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                @RequestBody @Valid WorkflowDto dto) {
        workflowService.update(id, dto, userId);
        return Result.ok();
    }

    @Operation(summary = "软删工作流")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        workflowService.softDelete(id, userId);
        return Result.ok();
    }

    @Operation(summary = "触发执行 (异步, 返回 runId)")
    @PostMapping("/{id}/run")
    public Result<Long> run(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return Result.ok(workflowService.triggerRun(id, userId));
    }

    @Operation(summary = "执行历史 (近 20 次)")
    @GetMapping("/{id}/runs")
    public Result<List<Map<String, Object>>> runs(@PathVariable Long id,
                                                    @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(workflowService.listRuns(id, limit));
    }

    @Operation(summary = "校验 DAG 结构 (不执行)")
    @PostMapping("/validate")
    public Result<DagValidationVo> validate(@RequestBody WorkflowDto dto) {
        return Result.ok(workflowService.validate(dto));
    }
}
