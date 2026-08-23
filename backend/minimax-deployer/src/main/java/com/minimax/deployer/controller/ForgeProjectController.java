package com.minimax.deployer.controller;

import com.minimax.common.result.Result;
import com.minimax.deployer.dto.ParseRequirementsRequest;
import com.minimax.deployer.entity.ForgeAgent;
import com.minimax.deployer.entity.ForgeProject;
import com.minimax.deployer.entity.ForgeWorkflowStep;
import com.minimax.deployer.service.ForgeProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent Forge 项目控制器 (V4.0)
 *
 * 提供:
 *  - POST   /api/v1/forge/projects              - 创建项目 (含 LLM 解析)
 *  - GET    /api/v1/forge/projects              - 列表
 *  - GET    /api/v1/forge/projects/{id}         - 详情
 *  - GET    /api/v1/forge/projects/{id}/agents  - 列出项目 agent (子表)
 *  - GET    /api/v1/forge/projects/{id}/workflow - 列出 workflow (子表)
 *  - DELETE /api/v1/forge/projects/{id}         - 删除
 */
@Tag(name = "Agent Forge - 项目管理")
@RestController
@RequestMapping("/api/v1/forge/projects")
@RequiredArgsConstructor
@Slf4j
public class ForgeProjectController {

    private final ForgeProjectService projectService;

    @PostMapping
    @Operation(summary = "创建项目 (含 LLM 需求解析)")
    public Result<ForgeProjectService.CreateResult> create(
            @Valid @RequestBody ParseRequirementsRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        log.info("[Forge] 创建项目, userId={}, llm={}", userId, request.getLlmModel());
        ForgeProjectService.CreateResult result = projectService.createWithParsed(
            request.getSource(), request.getContent(),
            request.getDocumentName(), request.getTemplateCode(),
            request.getLlmModel(), userId
        );
        return Result.ok(result);
    }

    @GetMapping
    @Operation(summary = "列出我的项目")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.ok(projectService.listByOwner(userId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "项目详情")
    public Result<ForgeProject> detail(@PathVariable Long id) {
        return Result.ok(projectService.getById(id));
    }

    @GetMapping("/{id}/agents")
    @Operation(summary = "项目的智能体列表 (子表)")
    public Result<List<ForgeAgent>> agents(@PathVariable Long id) {
        return Result.ok(projectService.listAgents(id));
    }

    @GetMapping("/{id}/workflow")
    @Operation(summary = "项目的工作流步骤 (子表)")
    public Result<List<ForgeWorkflowStep>> workflow(@PathVariable Long id) {
        return Result.ok(projectService.listWorkflow(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除项目")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return Result.ok();
    }
}
