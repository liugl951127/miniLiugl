package com.minimax.deployer.service;

import com.minimax.deployer.deploy.DeploymentStrategy;
import com.minimax.deployer.entity.ForgeDeployment;
import com.minimax.deployer.entity.ForgeRelease;
import com.minimax.deployer.mapper.ForgeDeploymentMapper;
import com.minimax.deployer.mapper.ForgeReleaseMapper;
import com.minimax.deployer.state.ReleaseStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部署服务 (V4.1) — 单一入口, 拆 4 个 Strategy
 *
 * V4.1 简化:
 *  - 4 个 Strategy 独立 (K8sSim/GitOps/Edge/Docker)
 *  - DeploymentService 只负责: 创建 deployment + 路由 + 状态机推进 + 失败处理
 *  - 业务逻辑 (K8s yaml / ArgoCD CRD / 边缘脚本 / docker) 全部在 strategy 内
 *  - 失败时 fire(FAIL, reason), 状态机自动转 FAILED
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeploymentService {

    private final ReleaseStateMachine stateMachine;
    private final ForgeReleaseMapper releaseMapper;
    private final ForgeDeploymentMapper deploymentMapper;
    private final DeploymentLogService logService;
    private final List<DeploymentStrategy> strategies;

    public Long deploy(Long releaseId) {
        ForgeRelease release = releaseMapper.selectById(releaseId);
        if (release == null) throw new IllegalArgumentException("Release 不存在: " + releaseId);

        // 1. 建 deployment
        ForgeDeployment deployment = ForgeDeployment.builder()
            .releaseId(releaseId)
            .instanceName((release.getImageTag() != null ? release.getImageTag() : "v0") + "-" + System.currentTimeMillis())
            .currentStage("PENDING")
            .status("PENDING")
            .target(release.getDeployTarget())
            .namespace("agent-forge")
            .desiredReplicas(release.getReplicas() != null ? release.getReplicas() : 2)
            .runningReplicas(0)
            .startedAt(LocalDateTime.now())
            .build();
        deploymentMapper.insert(deployment);
        logService.append(deployment.getId(), "INFO", "INIT", "部署启动 target=" + release.getDeployTarget());

        // 2. 状态机推进 (DRAFT → BUILDING)
        try { stateMachine.fire(releaseId, ReleaseStateMachine.Event.BUILD, "deploy triggered"); }
        catch (IllegalStateException e) { log.debug("[Deploy] 已非 DRAFT, 跳过 BUILD 事件: {}", e.getMessage()); }

        // 3. 路由 strategy
        Map<String, DeploymentStrategy> byName = strategies.stream()
            .collect(Collectors.toMap(DeploymentStrategy::name, s -> s));
        DeploymentStrategy strategy = byName.get((release.getDeployTarget() != null ? release.getDeployTarget() : "k8s").toLowerCase());
        if (strategy == null) {
            logService.append(deployment.getId(), "ERROR", "INIT",
                "未知部署目标: " + release.getDeployTarget() + " (可选: k8s, gitops, edge, docker)");
            failDeployment(deployment, release, "未知 target: " + release.getDeployTarget());
            throw new IllegalArgumentException("未知部署目标: " + release.getDeployTarget());
        }

        // 4. 执行
        try {
            strategy.execute(release, deployment);
        } catch (Exception e) {
            log.error("[Deploy] 失败", e);
            failDeployment(deployment, release, e.getMessage());
            throw e;
        }
        return deployment.getId();
    }

    private void failDeployment(ForgeDeployment deployment, ForgeRelease release, String reason) {
        deploymentMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ForgeDeployment>()
            .eq("id", deployment.getId())
            .set("status", "FAILED")
            .set("error_message", reason)
            .set("finished_at", LocalDateTime.now()));
        try {
            stateMachine.fire(release.getId(), ReleaseStateMachine.Event.FAIL, reason);
        } catch (IllegalStateException e) {
            log.debug("[Deploy] 状态机拒绝 FAIL: {}", e.getMessage());
        }
    }
}
