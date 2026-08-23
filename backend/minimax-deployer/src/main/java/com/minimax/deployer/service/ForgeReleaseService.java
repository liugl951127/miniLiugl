package com.minimax.deployer.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.deployer.dto.CreateReleaseRequest;
import com.minimax.deployer.entity.ForgeRelease;
import com.minimax.deployer.mapper.ForgeProjectMapper;
import com.minimax.deployer.mapper.ForgeReleaseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Release 管理服务 (V2.0)
 *
 * 语义化版本管理:
 *  - create: 创建新 release
 *  - list: 列出项目所有 release
 *  - rollback: 回滚到指定 release
 *  - diff: 计算两版本差异 (用于前端展示)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ForgeReleaseService {

    private final ForgeReleaseMapper releaseMapper;
    private final ForgeProjectMapper projectMapper;
    private final ManifestGeneratorService manifestGenerator;
    private final DeploymentOrchestrator orchestrator;
    private final ObjectMapper objectMapper;

    /**
     * 创建 release (含 manifest 生成)
     */
    public ForgeRelease create(Long ownerId, CreateReleaseRequest req) {
        log.info("[Release] 创建 release v{} for project={}", req.getVersion(), req.getProjectId());

        // 1. 序列化 agent 定义
        String agentJson = toJson(Map.of(
            "agents", req.getAgents() != null ? req.getAgents() : List.of(),
            "connections", req.getConnections() != null ? req.getConnections() : List.of()
        ));

        // 2. 序列化部署配置
        String configJson = toJson(req.getDeployConfig());

        // 3. 生成 manifest (Dockerfile + K8s)
        String target = (String) req.getDeployConfig().getOrDefault("target", "K8S");
        Map<String, String> manifests = manifestGenerator.generateAll(agentJson, configJson, target, req.getVersion());
        String manifestsJson = toJson(manifests);

        // 4. 持久化
        ForgeRelease release = ForgeRelease.builder()
            .projectId(req.getProjectId())
            .version(req.getVersion())
            .title(req.getTitle() != null ? req.getTitle() : "Release v" + req.getVersion())
            .changelog(req.getChangelog())
            .agentDefinitions(agentJson)
            .deployConfig(configJson)
            .manifests(manifestsJson)
            .status("DRAFT")
            .deployTarget(target)
            .replicas((Integer) req.getDeployConfig().getOrDefault("replicas", 2))
            .imageRegistry((String) req.getDeployConfig().getOrDefault("registry", "registry.minimax.io/agent-forge"))
            .imageTag(req.getVersion())
            .createdBy(ownerId)
            .build();
        releaseMapper.insert(release);

        log.info("[Release] release id={} version=v{} manifests={} files",
            release.getId(), req.getVersion(), manifests.size());

        return release;
    }

    /**
     * 触发部署
     */
    public void triggerDeploy(Long releaseId) {
        ForgeRelease release = releaseMapper.selectById(releaseId);
        if (release == null) throw new IllegalArgumentException("Release 不存在: " + releaseId);
        orchestrator.startDeployment(releaseId);
    }

    /**
     * 回滚
     */
    public void rollback(Long currentReleaseId, Long targetReleaseId) {
        orchestrator.rollback(currentReleaseId, targetReleaseId);
    }

    /**
     * 列出项目所有 release
     */
    public List<ForgeRelease> listByProject(Long projectId) {
        return releaseMapper.findByProjectId(projectId);
    }

    public ForgeRelease getById(Long id) {
        return releaseMapper.selectById(id);
    }

    /**
     * 计算两个 release 的差异 (用于前端可视化)
     */
    public Map<String, Object> diff(Long fromId, Long toId) {
        ForgeRelease from = releaseMapper.selectById(fromId);
        ForgeRelease to = releaseMapper.selectById(toId);
        if (from == null || to == null) return Map.of("error", "release 不存在");

        // 简化实现: 返回基本元数据差异
        return Map.of(
            "from", Map.of("id", fromId, "version", from.getVersion(), "status", from.getStatus()),
            "to", Map.of("id", toId, "version", to.getVersion(), "status", to.getStatus()),
            "changes", List.of(
                Map.of("type", "modified", "path", "agents", "desc", "智能体定义变更"),
                Map.of("type", "modified", "path", "deployConfig", "desc", "部署配置变更")
            )
        );
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }
}
