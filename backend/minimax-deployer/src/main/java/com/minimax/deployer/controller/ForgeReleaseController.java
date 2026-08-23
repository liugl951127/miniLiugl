package com.minimax.deployer.controller;

import com.minimax.common.result.Result;
import com.minimax.deployer.dto.CreateReleaseRequest;
import com.minimax.deployer.entity.ForgeRelease;
import com.minimax.deployer.service.ForgeReleaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * Agent Forge Release 控制器 (V2.0)
 *
 * 提供:
 *  - POST   /api/v1/forge/releases              - 创建 release
 *  - GET    /api/v1/forge/releases/{id}         - 详情
 *  - GET    /api/v1/forge/releases?projectId=X  - 项目的所有 release
 *  - POST   /api/v1/forge/releases/{id}/deploy  - 触发部署
 *  - POST   /api/v1/forge/releases/{id}/rollback - 回滚
 *  - GET    /api/v1/forge/releases/{from}/diff/{to} - 差异
 *  - GET    /api/v1/forge/deployments/{id}/stream - SSE 实时状态
 */
@Tag(name = "Agent Forge - 发布管理")
@RestController
@RequestMapping("/api/v1/forge")
@RequiredArgsConstructor
@Slf4j
public class ForgeReleaseController {

    private final ForgeReleaseService releaseService;
    private final com.minimax.deployer.service.DeploymentOrchestrator orchestrator;

    @PostMapping("/releases")
    @Operation(summary = "创建 release")
    public Result<ForgeRelease> create(@Valid @RequestBody CreateReleaseRequest request,
                                       @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        ForgeRelease release = releaseService.create(userId, request);
        return Result.ok(release);
    }

    @GetMapping("/releases/{id}")
    @Operation(summary = "release 详情")
    public Result<ForgeRelease> detail(@PathVariable Long id) {
        return Result.ok(releaseService.getById(id));
    }

    @GetMapping("/releases")
    @Operation(summary = "项目的所有 release")
    public Result<List<ForgeRelease>> listByProject(@RequestParam Long projectId) {
        return Result.ok(releaseService.listByProject(projectId));
    }

    @PostMapping("/releases/{id}/deploy")
    @Operation(summary = "触发部署")
    public Result<Void> deploy(@PathVariable Long id) {
        log.info("[Release] 触发部署 release={}", id);
        releaseService.triggerDeploy(id);
        return Result.ok();
    }

    @PostMapping("/releases/{id}/rollback/{targetId}")
    @Operation(summary = "回滚到指定 release")
    public Result<Void> rollback(@PathVariable Long id, @PathVariable Long targetId) {
        log.info("[Release] 回滚 release={} -> {}", id, targetId);
        releaseService.rollback(id, targetId);
        return Result.ok();
    }

    @GetMapping("/releases/{fromId}/diff/{toId}")
    @Operation(summary = "两个 release 的差异")
    public Result<Map<String, Object>> diff(@PathVariable Long fromId, @PathVariable Long toId) {
        return Result.ok(releaseService.diff(fromId, toId));
    }

    @GetMapping(value = "/deployments/{id}/stream", produces = "text/event-stream")
    @Operation(summary = "SSE 实时部署状态")
    public SseEmitter stream(@PathVariable Long id) {
        log.info("[Release] 订阅部署状态 deploymentId={}", id);
        return orchestrator.subscribe(id);
    }
}
