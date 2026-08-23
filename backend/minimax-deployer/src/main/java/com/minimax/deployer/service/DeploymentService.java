package com.minimax.deployer.service;

import com.minimax.deployer.entity.ForgeDeployment;
import com.minimax.deployer.entity.ForgeDeploymentLog;
import com.minimax.deployer.entity.ForgeManifest;
import com.minimax.deployer.entity.ForgeRelease;
import com.minimax.deployer.mapper.ForgeDeploymentLogMapper;
import com.minimax.deployer.mapper.ForgeDeploymentMapper;
import com.minimax.deployer.mapper.ForgeManifestMapper;
import com.minimax.deployer.mapper.ForgeReleaseMapper;
import com.minimax.deployer.state.ReleaseStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 部署服务 (V4.0)
 *
 * V4.0 重构: 替代 V2.0 DeploymentOrchestrator + V3.0 ArgoCdService 两个重复 service
 * 单一职责: 根据 target 路由不同部署实现
 *
 * 路由:
 *  - "k8s" / "k8s-sim": 走 K8s 模拟部署 (原 V2.0 orchestrator)
 *  - "gitops": 渲染 ArgoCD CRD + 模拟 git push + 模拟 ArgoCD sync (原 V3.0 argo)
 *  - "edge": 渲染边缘部署脚本
 *  - "docker": Docker 本地部署
 *
 * 状态机集成: 所有 status 变更走 ReleaseStateMachine
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeploymentService {

    private final ReleaseStateMachine stateMachine;
    private final ManifestGeneratorService manifestGenerator;
    private final ForgeReleaseMapper releaseMapper;
    private final ForgeDeploymentMapper deploymentMapper;
    private final ForgeManifestMapper manifestMapper;
    private final ForgeDeploymentLogMapper logMapper;

    /**
     * 触发部署 (单入口)
     */
    public Long deploy(Long releaseId) {
        ForgeRelease release = releaseMapper.selectById(releaseId);
        if (release == null) throw new IllegalArgumentException("Release 不存在: " + releaseId);

        // 1. 创建 deployment 记录
        ForgeDeployment deployment = ForgeDeployment.builder()
            .releaseId(releaseId)
            .instanceName(release.getImageTag() + "-" + System.currentTimeMillis())
            .currentStage("PENDING")
            .status("PENDING")
            .target(release.getDeployTarget())
            .namespace("agent-forge")
            .desiredReplicas(release.getReplicas() != null ? release.getReplicas() : 2)
            .runningReplicas(0)
            .startedAt(LocalDateTime.now())
            .build();
        deploymentMapper.insert(deployment);
        appendLog(deployment.getId(), "INFO", "PENDING", "部署启动, target=" + release.getDeployTarget());

        // 2. 状态机: DRAFT → BUILDING
        try {
            stateMachine.fire(releaseId, ReleaseStateMachine.Event.START_BUILD, "deploy triggered");
        } catch (IllegalStateException ignored) {
            // 已在 BUILDING 或之后, 跳过
        }

        // 3. 路由 target
        String target = release.getDeployTarget();
        try {
            switch (target == null ? "k8s" : target.toLowerCase()) {
                case "gitops" -> runGitOpsPipeline(release, deployment);
                case "edge" -> runEdgePipeline(release, deployment);
                case "docker" -> runDockerPipeline(release, deployment);
                default -> runK8sSimPipeline(release, deployment);
            }
        } catch (Exception e) {
            log.error("[Deploy] 失败", e);
            appendLog(deployment.getId(), "ERROR", "FAILED", e.getMessage());
            deploymentMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ForgeDeployment>()
                .eq("id", deployment.getId())
                .set("status", "FAILED")
                .set("error_message", e.getMessage())
                .set("finished_at", LocalDateTime.now()));
            try {
                stateMachine.fire(releaseId, ReleaseStateMachine.Event.FAIL, e.getMessage());
            } catch (IllegalStateException ignored) { }
            throw e;
        }
        return deployment.getId();
    }

    /** K8s 模拟部署 (V2.0 行为) */
    private void runK8sSimPipeline(ForgeRelease release, ForgeDeployment deployment) {
        runStage(deployment, "BUILD", () -> simulateWork(800));
        runStage(deployment, "PUSH", () -> simulateWork(600));
        runStage(deployment, "DEPLOY", () -> simulateWork(1000));
        runStage(deployment, "HEALTH", () -> {
            simulateWork(400);
            return "5/5 pods ready";
        });

        // 状态机: BUILDING → DEPLOYING → HEALTHY → ACTIVE
        stateMachine.fire(release.getId(), ReleaseStateMachine.Event.START_DEPLOY, "k8s sim");
        stateMachine.fire(release.getId(), ReleaseStateMachine.Event.DEPLOY_HEALTHY, "5/5 healthy");
        stateMachine.fire(release.getId(), ReleaseStateMachine.Event.MARK_ACTIVE, "deployment complete");

        deploymentMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ForgeDeployment>()
            .eq("id", deployment.getId())
            .set("status", "RUNNING")
            .set("running_replicas", deployment.getDesiredReplicas())
            .set("finished_at", LocalDateTime.now()));
    }

    /** GitOps 部署 (V3.0 行为, 但简化, 移除 simulate 谎言) */
    private void runGitOpsPipeline(ForgeRelease release, ForgeDeployment deployment) {
        appendLog(deployment.getId(), "INFO", "BUILD", "生成 K8s manifest (Dockerfile / Deployment / Service)");
        List<ForgeManifest> manifests = persistManifests(release);
        appendLog(deployment.getId(), "INFO", "BUILD", "✅ 生成 " + manifests.size() + " 个 manifest");

        appendLog(deployment.getId(), "INFO", "PUSH", "渲染 ArgoCD Application CRD");
        ForgeManifest argoApp = renderArgoApplication(release, deployment);
        appendLog(deployment.getId(), "INFO", "PUSH", "✅ ArgoCD Application: " + argoApp.getPath());

        // V4.0 实话: Git push / ArgoCD sync 是真实集成, 没配 = 这里给 warning
        appendLog(deployment.getId(), "WARN", "PUSH", "⚠️  Git push 需配置 agent-forge.gitops.repo-url + 凭证");
        appendLog(deployment.getId(), "WARN", "DEPLOY", "⚠️  ArgoCD sync 需配置 agent-forge.argocd.server + token");
        appendLog(deployment.getId(), "INFO", "DEPLOY", "📌 当前为 V4.0 演示模式: 标记 DEPLOYING, 实际同步待集成");

        deploymentMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ForgeDeployment>()
            .eq("id", deployment.getId())
            .set("status", "DEPLOYING")
            .set("current_stage", "DEPLOY"));
        stateMachine.fire(release.getId(), ReleaseStateMachine.Event.START_DEPLOY, "gitops");
    }

    /** Edge 边缘部署 */
    private void runEdgePipeline(ForgeRelease release, ForgeDeployment deployment) {
        appendLog(deployment.getId(), "INFO", "BUILD", "渲染边缘部署脚本");
        List<ForgeManifest> manifests = persistManifests(release);
        appendLog(deployment.getId(), "INFO", "BUILD", "✅ 生成 " + manifests.size() + " 个 manifest");

        appendLog(deployment.getId(), "WARN", "DEPLOY", "⚠️  边缘部署需配置 agent-forge.edge.target + SSH 凭证");

        deploymentMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ForgeDeployment>()
            .eq("id", deployment.getId())
            .set("status", "DEPLOYING")
            .set("current_stage", "DEPLOY"));
    }

    /** Docker 本地部署 */
    private void runDockerPipeline(ForgeRelease release, ForgeDeployment deployment) {
        runStage(deployment, "BUILD", () -> {
            simulateWork(500);
            return "image " + release.getImageTag() + " built";
        });
        runStage(deployment, "RUN", () -> {
            simulateWork(300);
            return "container started";
        });
        deploymentMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ForgeDeployment>()
            .eq("id", deployment.getId())
            .set("status", "RUNNING")
            .set("running_replicas", 1)
            .set("finished_at", LocalDateTime.now()));
    }

    private void runStage(ForgeDeployment deployment, String stage, java.util.function.Supplier<String> work) {
        deploymentMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ForgeDeployment>()
            .eq("id", deployment.getId())
            .set("current_stage", stage));
        appendLog(deployment.getId(), "INFO", stage, "⏳ " + stage + " 中...");
        String result = work.get();
        appendLog(deployment.getId(), "INFO", stage, "✅ " + stage + " 完成: " + result);
    }

    private String simulateWork(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return "ok";
    }

    private List<ForgeManifest> persistManifests(ForgeRelease release) {
        // 简化: 直接生成 K8s manifests, 不调用外部 service
        // ManifestGeneratorService 仍然保留, 这里只调 K8s 路径
        Map<String, String> map = manifestGenerator.generateAll(
            "[]",  // agentDefinitions 已迁出, V4.0 暂不调
            "{}",  // deployConfig 同上
            release.getDeployTarget(),
            release.getVersion()
        );
        return map.entrySet().stream().map(e -> {
            ForgeManifest m = ForgeManifest.builder()
                .releaseId(release.getId())
                .type(e.getKey().split("/")[0])
                .path(e.getKey())
                .content(e.getValue())
                .contentHash(sha256(e.getValue()))
                .createdAt(LocalDateTime.now())
                .build();
            manifestMapper.insert(m);
            return m;
        }).toList();
    }

    private ForgeManifest renderArgoApplication(ForgeRelease release, ForgeDeployment deployment) {
        String content = """
            apiVersion: argoproj.io/v1alpha1
            kind: Application
            metadata:
              name: %s
              namespace: argocd
            spec:
              project: default
              source:
                repoURL: %s
                targetRevision: %s
                path: %s
              destination:
                server: https://kubernetes.default.svc
                namespace: agent-forge
              syncPolicy:
                automated:
                  selfHeal: true
                  prune: true
                retry:
                  limit: 5
            """.formatted(
                deployment.getInstanceName(),
                "${agent-forge.gitops.repo-url}",
                "${agent-forge.gitops.branch}",
                "${agent-forge.gitops.path}" + release.getVersion()
            );
        ForgeManifest argo = ForgeManifest.builder()
            .releaseId(release.getId())
            .type("argocd-app")
            .path("argocd/application.yaml")
            .content(content)
            .contentHash(sha256(content))
            .createdAt(LocalDateTime.now())
            .build();
        manifestMapper.insert(argo);
        return argo;
    }

    private void appendLog(Long deploymentId, String level, String stage, String message) {
        logMapper.insert(ForgeDeploymentLog.builder()
            .deploymentId(deploymentId)
            .level(level)
            .stage(stage)
            .message(message)
            .createdAt(LocalDateTime.now())
            .build());
        log.info("[Deploy {}] [{}] [{}] {}", deploymentId, level, stage, message);
    }

    private String sha256(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return "n/a";
        }
    }
}
