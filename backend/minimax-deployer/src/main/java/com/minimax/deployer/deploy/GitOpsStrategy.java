package com.minimax.deployer.deploy;

import com.minimax.deployer.entity.ForgeAgent;
import com.minimax.deployer.entity.ForgeDeployment;
import com.minimax.deployer.entity.ForgeManifest;
import com.minimax.deployer.entity.ForgeRelease;
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
import java.util.List;
import java.util.Map;

/** GitOps 部署 (V4.1) — 真实生成 ArgoCD CRD + 持久化到 forge_manifest */
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

    @Value("${agent-forge.gitops.repo-url:}")
    private String repoUrl;
    @Value("${agent-forge.gitops.branch:main}")
    private String branch;
    @Value("${agent-forge.gitops.path:agents/}")
    private String path;
    @Value("${agent-forge.argocd.server:}")
    private String argoServer;

    @Override public String name() { return "gitops"; }

    @Override
    public void execute(ForgeRelease release, ForgeDeployment deployment) {
        // 1. 加载 release 的 agent
        List<ForgeAgent> agents = agentMapper.selectList(
            new QueryWrapper<ForgeAgent>().eq("release_id", release.getId()).orderByAsc("sort_order"));
        Map<String, Object> cfg = Map.of(
            "replicas", release.getReplicas() != null ? release.getReplicas() : 2,
            "registry", release.getImageRegistry() != null ? release.getImageRegistry() : ""
        );

        // 2. 真实生成 K8s manifest (用 V4.1 重构后的 ManifestGenerator, 接收 List<ForgeAgent>)
        logService.append(deployment.getId(), "INFO", "BUILD", "生成 K8s manifest (agents=" + agents.size() + ")");
        Map<String, String> manifests = manifestGenerator.generateAll(agents, cfg, ManifestGeneratorService.Target.GITOPS, release.getVersion());

        for (var e : manifests.entrySet()) {
            persistManifest(release.getId(), e.getKey(), e.getValue());
        }
        logService.append(deployment.getId(), "INFO", "BUILD", "✅ 生成 " + manifests.size() + " 个 manifest (已落 forge_manifest 子表)");

        // 3. 渲染 ArgoCD Application CRD
        String argoYaml = renderArgoApp(release, deployment);
        persistManifest(release.getId(), "argocd/application.yaml", argoYaml);
        logService.append(deployment.getId(), "INFO", "BUILD", "✅ ArgoCD Application: " + deployment.getInstanceName() + ".yaml");

        // 4. 状态机推进
        stateMachine.fire(release.getId(), ReleaseStateMachine.Event.DEPLOY);
        // stateMachine.fire(release.getId(), ReleaseStateMachine.Event.READY);  // GitOps 真实同步待 V5.0

        // 5. WARN: 真实 git push / ArgoCD sync 未集成
        if (repoUrl == null || repoUrl.isBlank()) {
            logService.append(deployment.getId(), "WARN", "PUSH",
                "⚠️  需配置 agent-forge.gitops.repo-url 才能真推送");
        } else {
            logService.append(deployment.getId(), "INFO", "PUSH",
                "📌 Git push 目标: " + repoUrl + " (V4.1 未集成 JGit, 待 V5.0)");
        }
        if (argoServer == null || argoServer.isBlank()) {
            logService.append(deployment.getId(), "WARN", "DEPLOY",
                "⚠️  需配置 agent-forge.argocd.server 才能查 ArgoCD 状态");
        } else {
            logService.append(deployment.getId(), "INFO", "DEPLOY",
                "📌 ArgoCD Server: " + argoServer + " (V4.1 未集成 API 轮询, 待 V5.0)");
        }

        deploymentMapper.update(null, new UpdateWrapper<ForgeDeployment>()
            .eq("id", deployment.getId())
            .set("status", "DEPLOYING")
            .set("current_stage", "DEPLOY"));
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
                repoUrl.isBlank() ? "<configure agent-forge.gitops.repo-url>" : repoUrl,
                branch, this.path + release.getVersion());
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
