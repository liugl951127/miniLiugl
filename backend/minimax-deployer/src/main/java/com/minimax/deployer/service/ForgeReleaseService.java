package com.minimax.deployer.service;

import com.minimax.deployer.dto.CreateReleaseRequest;
import com.minimax.deployer.entity.ForgeAgent;
import com.minimax.deployer.entity.ForgeProject;
import com.minimax.deployer.entity.ForgeRelease;
import com.minimax.deployer.entity.ForgeWorkflowStep;
import com.minimax.deployer.mapper.ForgeAgentMapper;
import com.minimax.deployer.mapper.ForgeProjectMapper;
import com.minimax.deployer.mapper.ForgeReleaseMapper;
import com.minimax.deployer.mapper.ForgeWorkflowStepMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Release 管理服务 (V4.1)
 *
 * V4.1 改动:
 *  - agents / workflow 只在 create 时写 forge_agent / forge_workflow_step 一次 (V4.0 双写修复)
 *  - Manifest 不在 create 时生成, 改在 deploy 时由 DeploymentService + ManifestGenerator 生成
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ForgeReleaseService {

    private final ForgeReleaseMapper releaseMapper;
    private final ForgeProjectMapper projectMapper;
    private final ForgeAgentMapper agentMapper;
    private final ForgeWorkflowStepMapper workflowMapper;
    private final DeploymentService deploymentService;

    @Transactional
    public ForgeRelease create(Long ownerId, CreateReleaseRequest req) {
        log.info("[Release] 创建 v{} for project={}", req.getVersion(), req.getProjectId());

        String target = req.getDeployConfig() != null
            ? (String) req.getDeployConfig().getOrDefault("target", "k8s") : "k8s";
        Integer replicas = req.getDeployConfig() != null
            ? (Integer) req.getDeployConfig().getOrDefault("replicas", 2) : 2;
        String registry = req.getDeployConfig() != null
            ? (String) req.getDeployConfig().getOrDefault("registry", "registry.minimax.io/agent-forge")
            : "registry.minimax.io/agent-forge";

        ForgeRelease release = ForgeRelease.builder()
            .projectId(req.getProjectId())
            .version(req.getVersion())
            .title(req.getTitle() != null ? req.getTitle() : "Release v" + req.getVersion())
            .changelog(req.getChangelog())
            .status("DRAFT")
            .deployTarget(target)
            .replicas(replicas)
            .imageRegistry(registry)
            .imageTag(req.getVersion())
            .createdBy(ownerId)
            .createdAt(LocalDateTime.now())
            .build();
        releaseMapper.insert(release);

        // 写 agents 到 forge_agent 子表 (V4.1: 只在 release 写一次, 解决 V4.0 双写)
        if (req.getAgents() != null) {
            int idx = 0;
            for (Map<String, Object> a : req.getAgents()) {
                agentMapper.insert(ForgeAgent.builder()
                    .releaseId(release.getId())
                    .name((String) a.getOrDefault("name", "智能体"))
                    .role((String) a.getOrDefault("role", ""))
                    .emoji((String) a.getOrDefault("emoji", "🤖"))
                    .description((String) a.getOrDefault("desc", ""))
                    .color((String) a.getOrDefault("color", "linear-gradient(135deg, #6366f1, #8b5cf6)"))
                    .tools(String.join(",", toStringList(a.get("tools"))))
                    .model((String) a.getOrDefault("model", "Qwen2.5-7B"))
                    .sortOrder(idx++)
                    .createdAt(LocalDateTime.now())
                    .build());
            }
        }

        // 写 workflow 到 forge_workflow_step
        if (req.getWorkflow() != null) {
            for (Map<String, Object> w : req.getWorkflow()) {
                workflowMapper.insert(ForgeWorkflowStep.builder()
                    .releaseId(release.getId())
                    .stepNo(((Number) w.getOrDefault("step", 1)).intValue())
                    .name((String) w.getOrDefault("name", ""))
                    .type("agent")
                    .build());
            }
        }

        // 同步 project.current_release_id
        projectMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ForgeProject>()
            .eq("id", req.getProjectId())
            .set("current_release_id", release.getId())
            .set("updated_at", LocalDateTime.now()));

        log.info("[Release] id={} created, {} agents, {} workflow steps",
            release.getId(),
            req.getAgents() != null ? req.getAgents().size() : 0,
            req.getWorkflow() != null ? req.getWorkflow().size() : 0);
        return release;
    }

    public Long deploy(Long releaseId) {
        return deploymentService.deploy(releaseId);
    }

    public List<ForgeRelease> listByProject(Long projectId) {
        return releaseMapper.findByProjectId(projectId);
    }

    public ForgeRelease getById(Long id) {
        return releaseMapper.selectById(id);
    }

    public Map<String, Object> diff(Long fromId, Long toId) {
        ForgeRelease from = releaseMapper.selectById(fromId);
        ForgeRelease to = releaseMapper.selectById(toId);
        if (from == null || to == null) return Map.of("error", "release 不存在");
        return Map.of(
            "from", Map.of("id", fromId, "version", from.getVersion(), "status", from.getStatus()),
            "to", Map.of("id", toId, "version", to.getVersion(), "status", to.getStatus()),
            "changes", List.of(
                Map.of("type", "modified", "path", "agents", "desc", "智能体定义变更"),
                Map.of("type", "modified", "path", "deployConfig", "desc", "部署配置变更")
            )
        );
    }

    private List<String> toStringList(Object o) {
        if (o == null) return List.of();
        if (o instanceof List<?> l) return l.stream().map(String::valueOf).toList();
        return List.of(String.valueOf(o));
    }
}
