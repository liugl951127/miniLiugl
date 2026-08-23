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

/** 边缘部署 (V4.1) */
@Component
@RequiredArgsConstructor
@Slf4j
public class EdgeStrategy implements DeploymentStrategy {

    private final DeploymentLogService logService;
    private final ForgeDeploymentMapper deploymentMapper;
    private final ForgeManifestMapper manifestMapper;
    private final ForgeAgentMapper agentMapper;
    private final ManifestGeneratorService manifestGenerator;

    @Value("${agent-forge.edge.target:}")
    private String edgeTarget;

    @Override public String name() { return "edge"; }

    @Override
    public void execute(ForgeRelease release, ForgeDeployment deployment) {
        List<ForgeAgent> agents = agentMapper.selectList(
            new QueryWrapper<ForgeAgent>().eq("release_id", release.getId()).orderByAsc("sort_order"));
        Map<String, Object> cfg = Map.of(
            "replicas", release.getReplicas() != null ? release.getReplicas() : 1,
            "registry", release.getImageRegistry() != null ? release.getImageRegistry() : ""
        );

        logService.append(deployment.getId(), "INFO", "BUILD", "生成边缘部署脚本 (agents=" + agents.size() + ")");
        Map<String, String> manifests = manifestGenerator.generateAll(agents, cfg, ManifestGeneratorService.Target.EDGE, release.getVersion());
        for (var e : manifests.entrySet()) {
            manifestMapper.insert(ForgeManifest.builder()
                .releaseId(release.getId())
                .type(e.getKey().split("/")[0])
                .path(e.getKey())
                .content(e.getValue())
                .contentHash(sha256(e.getValue()))
                .createdAt(LocalDateTime.now())
                .build());
        }
        logService.append(deployment.getId(), "INFO", "BUILD", "✅ 生成 " + manifests.size() + " 个 edge script");

        if (edgeTarget == null || edgeTarget.isBlank()) {
            logService.append(deployment.getId(), "WARN", "DEPLOY",
                "⚠️  需配置 agent-forge.edge.target (SSH 地址) 才能真部署到边缘");
        } else {
            logService.append(deployment.getId(), "INFO", "DEPLOY",
                "📌 边缘目标: " + edgeTarget + " (V4.1 未集成 SSH, 待 V5.0)");
        }

        deploymentMapper.update(null, new UpdateWrapper<ForgeDeployment>()
            .eq("id", deployment.getId())
            .set("status", "DEPLOYING")
            .set("current_stage", "DEPLOY"));
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
