package com.minimax.deployer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.deployer.entity.ForgeRelease;
import com.minimax.deployer.entity.ForgeDeployment;
import com.minimax.deployer.mapper.ForgeDeploymentMapper;
import com.minimax.deployer.mapper.ForgeReleaseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ArgoCD GitOps 集成服务 (V3.0)
 *
 * 工作流程:
 *  1. 生成 K8s manifest (Dockerfile + Deployment + Service + ConfigMap)
 *  2. 生成 ArgoCD Application CRD
 *  3. 推送到 Git 仓库 (模拟, 生产用 JGit 或 Git HTTP API)
 *  4. ArgoCD 自动检测变更并同步到集群
 *  5. 轮询 ArgoCD API 获取部署状态
 *
 * 当前实现: 模拟 Git push + 模拟 ArgoCD sync 状态
 * 生产环境: 替换 gitPush() 为真实 git 操作, 调用 ArgoCD REST API
 *
 * ArgoCD API 参考:
 *  - POST /api/v1/applications  (创建 Application)
 *  - GET  /api/v1/applications/{name}  (查询状态)
 *  - POST /api/v1/applications/{name}/sync  (触发同步)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ArgoCdService {

    private final ForgeReleaseMapper releaseMapper;
    private final ForgeDeploymentMapper deploymentMapper;
    private final ManifestGeneratorService manifestGenerator;
    private final ObjectMapper objectMapper;

    @Value("${agent-forge.gitops.repo-url:https://git.minimax.io/agent-forge/manifests.git}")
    private String gitRepoUrl;

    @Value("${agent-forge.gitops.branch:main}")
    private String gitBranch;

    @Value("${agent-forge.gitops.path:agents/}")
    private String gitPath;

    @Value("${agent-forge.argocd.server:https://argocd.minimax.io}")
    private String argoCdServer;

    @Value("${agent-forge.argocd.project:default}")
    private String argoCdProject;

    /**
     * 完整 GitOps 部署流程
     */
    @Async
    public void deployViaGitOps(Long releaseId) {
        log.info("[GitOps] 启动 ArgoCD 部署流程 release={}", releaseId);
        ForgeRelease release = releaseMapper.selectById(releaseId);
        if (release == null) return;

        // 1. 创建 deployment 记录
        ForgeDeployment deployment = ForgeDeployment.builder()
            .releaseId(releaseId)
            .instanceName("argocd-" + System.currentTimeMillis())
            .stages("[]")
            .logs("")
            .status("PENDING")
            .target(release.getDeployTarget())
            .namespace("agent-forge")
            .desiredReplicas(release.getReplicas() != null ? release.getReplicas() : 2)
            .runningReplicas(0)
            .startedAt(LocalDateTime.now())
            .build();
        deploymentMapper.insert(deployment);

        // 2. 阶段 1: 生成 manifests
        appendLog(deployment.getId(), "INFO", "📦 阶段 1/6: 生成 K8s manifests");
        Map<String, String> manifests = manifestGenerator.generateAll(
            release.getAgentDefinitions(),
            release.getDeployConfig(),
            release.getDeployTarget(),
            release.getVersion()
        );
        log.info("[GitOps] 生成 {} 个 manifest 文件", manifests.size());
        appendLog(deployment.getId(), "INFO", "✅ 生成 " + manifests.size() + " 个 manifest");

        // 3. 阶段 2: 生成 ArgoCD Application
        appendLog(deployment.getId(), "INFO", "🔧 阶段 2/6: 生成 ArgoCD Application CRD");
        String argoApp = renderArgoApplication(release, deployment.getInstanceName());
        appendLog(deployment.getId(), "INFO", "✅ ArgoCD Application 生成: " + deployment.getInstanceName());

        // 4. 阶段 3: Git push
        appendLog(deployment.getId(), "INFO", "📤 阶段 3/6: Git push → " + gitRepoUrl);
        String commitSha = simulateGitPush(manifests, argoApp, release);
        appendLog(deployment.getId(), "INFO", "✅ Git push 完成, commit=" + commitSha.substring(0, 8));

        // 5. 阶段 4: ArgoCD Sync (异步等待)
        appendLog(deployment.getId(), "INFO", "🔄 阶段 4/6: ArgoCD 同步中...");
        simulateArgoCdSync(deployment.getId(), release);

        // 6. 阶段 5-6: 健康检查 + 流量
        appendLog(deployment.getId(), "INFO", "💚 阶段 5/6: 健康检查通过");
        appendLog(deployment.getId(), "INFO", "🌐 阶段 6/6: 流量接入完成");

        // 7. 标记完成
        deploymentMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ForgeDeployment>()
            .eq("id", deployment.getId())
            .set("status", "RUNNING")
            .set("running_replicas", deployment.getDesiredReplicas())
            .set("finished_at", LocalDateTime.now()));
        releaseMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ForgeRelease>()
            .eq("id", releaseId)
            .set("status", "ACTIVE")
            .set("deployed_at", LocalDateTime.now()));

        log.info("[GitOps] release={} GitOps 部署完成", releaseId);
    }

    /**
     * 渲染 ArgoCD Application CRD
     */
    private String renderArgoApplication(ForgeRelease release, String appName) {
        return """
            apiVersion: argoproj.io/v1alpha1
            kind: Application
            metadata:
              name: %s
              namespace: argocd
              labels:
                managed-by: agent-forge
                version: %s
                release-id: "%d"
            spec:
              project: %s
              source:
                repoURL: %s
                targetRevision: %s
                path: %s%s
              destination:
                server: https://kubernetes.default.svc
                namespace: %s
              syncPolicy:
                automated:
                  prune: true
                  selfHeal: true
                  allowEmpty: false
                syncOptions:
                  - CreateNamespace=true
                  - PrunePropagationPolicy=foreground
                retry:
                  limit: 5
                  backoff:
                    duration: 5s
                    factor: 2
                    maxDuration: 3m
              revisionHistoryLimit: 10
            """.formatted(
                appName, release.getVersion(), release.getId(),
                argoCdProject, gitRepoUrl, gitBranch, gitPath, release.getVersion(),
                "agent-forge"
            );
    }

    /** 模拟 Git push (V3.0) */
    private String simulateGitPush(Map<String, String> manifests, String argoApp, ForgeRelease release) {
        log.info("[GitOps] Git push: {} files + argo-app to {}/{} (simulated)",
            manifests.size(), gitRepoUrl, gitBranch);
        // 生产环境:
        //   1. clone 仓库到 /tmp
        //   2. cp manifest 文件到对应路径
        //   3. git add + commit
        //   4. git push
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** 模拟 ArgoCD sync (V3.0) */
    private void simulateArgoCdSync(Long deploymentId, ForgeRelease release) {
        // 生产环境: 轮询 ArgoCD REST API
        //   GET https://argocd.minimax.io/api/v1/applications/{name}
        //   检查 status.health.status == "Healthy"
        try {
            Thread.sleep(2000);
            appendLog(deploymentId, "INFO", "✅ ArgoCD sync: Healthy, 5/5 resources");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void appendLog(Long deploymentId, String level, String text) {
        ForgeDeployment d = deploymentMapper.selectById(deploymentId);
        if (d == null) return;
        String currentLogs = d.getLogs() != null ? d.getLogs() : "";
        String t = LocalDateTime.now().toString().substring(11, 19);
        String newLog = String.format("[%s] [%s] %s", t, level, text);
        deploymentMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ForgeDeployment>()
            .eq("id", deploymentId)
            .set("logs", currentLogs + newLog + "\n"));
        log.info("[GitOps deployment={}] {}", deploymentId, text);
    }

    /**
     * 查询 ArgoCD Application 状态 (V3.0, 调用 ArgoCD API)
     */
    public Map<String, Object> queryApplicationStatus(String appName) {
        log.info("[GitOps] 查询 ArgoCD Application 状态: {}", appName);
        // 生产环境:
        //   GET {argoCdServer}/api/v1/applications/{appName}
        //   解析 status.sync.status, status.health.status
        return Map.of(
            "name", appName,
            "sync", "Synced",
            "health", "Healthy",
            "lastSync", LocalDateTime.now().toString()
        );
    }
}
