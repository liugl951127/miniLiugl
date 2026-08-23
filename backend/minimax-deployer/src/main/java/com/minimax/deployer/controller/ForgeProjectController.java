package com.minimax.deployer.controller;

import com.minimax.common.result.Result;
import com.minimax.deployer.dto.ParseRequirementsRequest;
import com.minimax.deployer.entity.ForgeProject;
import com.minimax.deployer.service.ForgeProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Agent Forge 项目控制器 (V2.0)
 *
 * 提供:
 *  - POST   /api/v1/forge/projects              - 创建项目 (含需求解析)
 *  - GET    /api/v1/forge/projects              - 列表
 *  - GET    /api/v1/forge/projects/{id}         - 详情
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
    @Operation(summary = "创建项目 (含需求解析)")
    public Result<ForgeProject> create(@Valid @RequestBody ParseRequirementsRequest request,
                                       @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        log.info("[Forge] 创建项目, userId={}, source={}", userId, request.getSource());
        ForgeProject project = projectService.createWithParsed(
            request.getSource(),
            request.getContent(),
            request.getDocumentName(),
            request.getTemplateCode(),
            userId
        );
        return Result.ok(project);
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

    @DeleteMapping("/{id}")
    @Operation(summary = "删除项目")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return Result.ok();
    }
}
