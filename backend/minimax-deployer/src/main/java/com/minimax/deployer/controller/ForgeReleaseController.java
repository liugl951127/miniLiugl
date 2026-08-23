package com.minimax.deployer.controller;

import com.minimax.common.result.Result;
import com.minimax.deployer.dto.CreateReleaseRequest;
import com.minimax.deployer.entity.ForgeDeployment;
import com.minimax.deployer.entity.ForgeDeploymentLog;
import com.minimax.deployer.entity.ForgeManifest;
import com.minimax.deployer.entity.ForgeRelease;
import com.minimax.deployer.gitops.ArgoCdClient;
import com.minimax.deployer.mapper.ForgeDeploymentLogMapper;
import com.minimax.deployer.mapper.ForgeDeploymentMapper;
import com.minimax.deployer.mapper.ForgeManifestMapper;
import com.minimax.deployer.service.ForgeReleaseService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent Forge Release 控制器 (V4.0)
 *
 * V4.0 简化:
 *  - 删 /deploy-gitops (V3.0 假功能)  → 统一走 /deploy, target 字段在 body 决定
 *  - 删 /argocd/applications/{name}      → 由 deployment 的 status 字段反映
 *  - 删 /deployments/{id}/stream (SSE)   → 简化为 /deployments/{id}/logs 分页查询
 *  - 状态变更统一走 ReleaseStateMachine
 *
 * 提供:
 *  - POST   /api/v1/forge/releases              - 创建 release (agent/workflow 落子表)
 *  - GET    /api/v1/forge/releases/{id}         - 详情
 *  - GET    /api/v1/forge/releases?projectId=X  - 项目的所有 release
 *  - POST   /api/v1/forge/releases/{id}/deploy  - 触发部署 (按 deploy_target 路由)
 *  - GET    /api/v1/forge/releases/{id}/manifests - 列出 manifest
 *  - POST   /api/v1/forge/releases/{id}/rollback/{targetId} - 回滚
 *  - GET    /api/v1/forge/releases/{from}/diff/{to} - 差异
 *  - GET    /api/v1/forge/deployments/{id}/logs - 部署日志 (子表分页)
 *  - GET    /api/v1/forge/deployments/{id}      - 部署详情
 */
@Tag(name = "Agent Forge - 发布管理")
@RestController
@RequestMapping("/api/v1/forge")
@RequiredArgsConstructor
@Slf4j
public class ForgeReleaseController {

    private final ForgeReleaseService releaseService;
    private final ForgeDeploymentMapper deploymentMapper;
    private final ForgeDeploymentLogMapper logMapper;
    private final ForgeManifestMapper manifestMapper;
    private final ArgoCdClient argoCdClient;

    @PostMapping("/releases")
    @Operation(summary = "创建 release")
    public Result<ForgeRelease> create(@Valid @RequestBody CreateReleaseRequest req,
                                       @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.ok(releaseService.create(userId, req));
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

    @GetMapping("/releases/{id}/manifests")
    @Operation(summary = "release 的所有 manifest (子表)")
    public Result<List<ForgeManifest>> manifests(@PathVariable Long id) {
        return Result.ok(manifestMapper.selectList(
            new QueryWrapper<ForgeManifest>().eq("release_id", id).orderByAsc("type")
        ));
    }

    @PostMapping("/releases/{id}/deploy")
    @Operation(summary = "触发部署 (按 release.deploy_target 路由)")
    public Result<Long> deploy(@PathVariable Long id) {
        log.info("[Release] 部署 release={}", id);
        return Result.ok(releaseService.deploy(id));
    }

    @PostMapping("/releases/{id}/rollback/{targetId}")
    @Operation(summary = "回滚到指定 release (重新部署 target)")
    public Result<Long> rollback(@PathVariable Long id, @PathVariable Long targetId) {
        log.info("[Release] 回滚 release={} -> {}", id, targetId);
        return Result.ok(releaseService.deploy(targetId));
    }

    @GetMapping("/releases/{fromId}/diff/{toId}")
    @Operation(summary = "两个 release 的差异")
    public Result<Map<String, Object>> diff(@PathVariable Long fromId, @PathVariable Long toId) {
        return Result.ok(releaseService.diff(fromId, toId));
    }

    @GetMapping("/deployments/{id}")
    @Operation(summary = "部署详情")
    public Result<ForgeDeployment> deployment(@PathVariable Long id) {
        return Result.ok(deploymentMapper.selectById(id));
    }

    @GetMapping("/deployments/{id}/logs")
    @Operation(summary = "部署日志 (分页, 子表)")
    public Result<List<ForgeDeploymentLog>> logs(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "0") int offset,
                                                 @RequestParam(defaultValue = "100") int limit) {
        return Result.ok(logMapper.selectList(
            new QueryWrapper<ForgeDeploymentLog>()
                .eq("deployment_id", id)
                .orderByAsc("created_at")
                .last("LIMIT " + Math.min(limit, 500) + " OFFSET " + Math.max(offset, 0))
        ));
    }

    // ============ V5.0: ArgoCD 真 API 代理 ============

    @GetMapping("/argocd/applications/{appName}")
    @Operation(summary = "V5.0: 查询 ArgoCD Application 真实状态")
    public Result<ArgoCdClient.ApplicationStatus> argoStatus(@PathVariable String appName) {
        return Result.ok(argoCdClient.queryStatus(appName));
    }

    @PostMapping("/argocd/applications/{appName}/sync")
    @Operation(summary = "V5.0: 触发 ArgoCD Application 同步")
    public Result<Void> argoSync(@PathVariable String appName) {
        argoCdClient.triggerSync(appName);
        return Result.ok();
    }
}
