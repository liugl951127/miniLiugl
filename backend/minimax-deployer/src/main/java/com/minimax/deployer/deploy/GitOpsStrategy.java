package com.minimax.deployer.deploy;

import com.minimax.deployer.entity.ForgeAgent;
import com.minimax.deployer.entity.ForgeDeployment;
import com.minimax.deployer.entity.ForgeManifest;
import com.minimax.deployer.entity.ForgeRelease;
import com.minimax.deployer.gitops.ArgoCdClient;
import com.minimax.deployer.gitops.GitOpsClient;
import com.minimax.deployer.mapper.ForgeAgentMapper;
import com.minimax.deployer.mapper.ForgeDeploymentMapper;
import com.minimax.deployer.mapper.ForgeManifestMapper;
import com.minimax.deployer.service.DeploymentLogService;
import com.minimax.deployer.service.ManifestGeneratorService;
import com.minimax.deployer.state.ReleaseStateMachine;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GitOps 部署 (V5.0) — 真做
 *
 * V4.1: 渲染 manifest + WARN "未集成"
 * V5.0: 真 clone+commit+push (JGit) + 真调 ArgoCD REST API
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GitOpsStrategy implements DeploymentStrategy {

    private final ReleaseStateMachine stateMachine;
    private final DeploymentLogService logService;
    private final ForgeDeploymentMapper deploymentMapper;
    private final ForgeManifestMapper manifestMapper;
    private final ForgeAgentMapper agentMapper;
    private final ManifestGeneratorService manifestGenerator;
    private final GitOpsClient gitOpsClient;
    private final ArgoCdClient argoCdClient;

    @Value("${agent-forge.gitops.repo-url:}")
    private String repoUrl;
    @Value("${agent-forge.gitops.branch:main}")
    private String branch;
    @Value("${agent-forge.gitops.path:agents/}")
    private String gitPath;

    @Override public String name() { return "gitops"; }

    @Override
    public void execute(ForgeRelease release, ForgeDeployment deployment) {
        // 1. 配置预检
        if (repoUrl == null || repoUrl.isBlank()) {
            throw new IllegalStateException("需配置 agent-forge.gitops.repo-url");
        }

        // 2. 加载 agents + 生成 manifests
        List<ForgeAgent> agents = agentMapper.selectList(
            new QueryWrapper<ForgeAgent>().eq("release_id", release.getId()).orderByAsc("sort_order"));
        Map<String, Object> cfg = Map.of(
            "replicas", release.getReplicas() != null ? release.getReplicas() : 2,
            "registry", release.getImageRegistry() != null ? release.getImageRegistry() : ""
        );

        logService.append(deployment.getId(), "INFO", "BUILD", "生成 K8s manifest (agents=" + agents.size() + ")");
        Map<String, String> manifests = manifestGenerator.generateAll(agents, cfg, ManifestGeneratorService.Target.GITOPS, release.getVersion());

        // 3. 渲染 ArgoCD Application CRD
        String argoYaml = renderArgoApp(release, deployment);
        Map<String, String> allFiles = new LinkedHashMap<>(manifests);
        allFiles.put("argocd/application.yaml", argoYaml);

        // 4. 落 forge_manifest 子表
        for (var e : allFiles.entrySet()) {
            persistManifest(release.getId(), e.getKey(), e.getValue());
        }
        logService.append(deployment.getId(), "INFO", "BUILD", "✅ 生成 " + allFiles.size() + " 个 manifest");

        // 5. 真 git push (V5.0)
        String basePath = gitPath + release.getVersion() + "/";
        try {
            String commitSha = gitOpsClient.pushManifests(repoUrl, branch, basePath, allFiles);
            logService.append(deployment.getId(), "INFO", "PUSH", "✅ Git push 成功: " + commitSha.substring(0, Math.min(8, commitSha.length())));
        } catch (GitOpsClient.GitOpsException e) {
            logService.append(deployment.getId(), "ERROR", "PUSH", "❌ Git push 失败: " + e.getMessage());
            throw new RuntimeException(e);
        }

        // 6. 状态机: BUILDING → DEPLOYING
        stateMachine.fire(release.getId(), ReleaseStateMachine.Event.DEPLOY);
        logService.append(deployment.getId(), "INFO", "DEPLOY", "状态机 BUILDING → DEPLOYING");

        // 7. 触发 ArgoCD sync (V5.0 真做)
        try {
            argoCdClient.triggerSync(deployment.getInstanceName());
            logService.append(deployment.getId(), "INFO", "DEPLOY", "✅ ArgoCD sync 触发");
        } catch (ArgoCdClient.ArgoCdException e) {
            logService.append(deployment.getId(), "WARN", "DEPLOY", "⚠️ ArgoCD sync 触发失败: " + e.getMessage());
            // 不抛 — Git push 成功就算基本完成, ArgoCD 状态走前端轮询
        }

        // 8. 轮询 ArgoCD 状态 (同步阻塞, 最多 60s)
        logService.append(deployment.getId(), "INFO", "DEPLOY", "等待 ArgoCD 同步 (最多 60s)...");
        ArgoCdClient.ApplicationStatus finalStatus = pollArgoCdStatus(deployment, deployment.getInstanceName(), 60_000);

        // 9. 状态机: DEPLOYING → HEALTHY → ACTIVE
        if (finalStatus != null && "Healthy".equalsIgnoreCase(finalStatus.health())) {
            stateMachine.fire(release.getId(), ReleaseStateMachine.Event.READY);
            stateMachine.fire(release.getId(), ReleaseStateMachine.Event.ACTIVATE);
            deploymentMapper.update(null, new UpdateWrapper<ForgeDeployment>()
                .eq("id", deployment.getId())
                .set("status", "RUNNING")
                .set("running_replicas", deployment.getDesiredReplicas())
                .set("current_stage", "DONE")
                .set("finished_at", LocalDateTime.now()));
            logService.append(deployment.getId(), "INFO", "HEALTH", "✅ ArgoCD " + finalStatus.health() + " (revision=" + finalStatus.revision() + ")");
        } else if (finalStatus != null) {
            // 状态不对, FAIL
            stateMachine.fire(release.getId(), ReleaseStateMachine.Event.FAIL,
                "ArgoCD health=" + finalStatus.health() + " sync=" + finalStatus.syncStatus());
            deploymentMapper.update(null, new UpdateWrapper<ForgeDeployment>()
                .eq("id", deployment.getId())
                .set("status", "DEGRADED")
                .set("current_stage", "HEALTH")
                .set("error_message", "ArgoCD " + finalStatus.health()));
            logService.append(deployment.getId(), "WARN", "HEALTH",
                "⚠️ ArgoCD 未健康: " + finalStatus.health() + " sync=" + finalStatus.syncStatus());
        } else {
            logService.append(deployment.getId(), "WARN", "DEPLOY", "ArgoCD 状态未确认, 标记 DEPLOYING (待前端轮询)");
            deploymentMapper.update(null, new UpdateWrapper<ForgeDeployment>()
                .eq("id", deployment.getId())
                .set("status", "DEPLOYING")
                .set("current_stage", "DEPLOY"));
        }
    }

    /** 同步轮询 ArgoCD, 直到 Healthy 或超时 */
    private ArgoCdClient.ApplicationStatus pollArgoCdStatus(ForgeDeployment deployment, String appName, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        ArgoCdClient.ApplicationStatus last = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                ArgoCdClient.ApplicationStatus s = argoCdClient.queryStatus(appName);
                last = s;
                if ("Healthy".equalsIgnoreCase(s.health()) && "Synced".equalsIgnoreCase(s.syncStatus())) {
                    return s;
                }
                logService.append(deployment.getId(), "INFO", "DEPLOY",
                    "⏳ ArgoCD " + appName + ": health=" + s.health() + " sync=" + s.syncStatus());
            } catch (ArgoCdClient.ArgoCdException e) {
                logService.append(deployment.getId(), "WARN", "DEPLOY", "ArgoCD 查询失败: " + e.getMessage());
            }
            try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        return last;
    }

    private void persistManifest(Long releaseId, String path, String content) {
        manifestMapper.insert(ForgeManifest.builder()
            .releaseId(releaseId)
            .type(path.split("/")[0])
            .path(path)
            .content(content)
            .contentHash(sha256(content))
            .createdAt(LocalDateTime.now())
            .build());
    }

    private String renderArgoApp(ForgeRelease release, ForgeDeployment deployment) {
        return """
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
                path: %s%s
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
                deployment.getInstanceName(), repoUrl, branch, gitPath, release.getVersion());
    }

    private String sha256(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) { return "n/a"; }
    }
}
