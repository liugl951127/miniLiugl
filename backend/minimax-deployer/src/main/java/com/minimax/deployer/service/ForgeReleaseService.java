package com.minimax.deployer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.deployer.dto.CreateReleaseRequest;
import com.minimax.deployer.entity.*;
import com.minimax.deployer.mapper.*;
import com.minimax.deployer.state.ReleaseStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Release 管理服务 (V4.0)
 *
 * V4.0 重构: 拆分到子表
 *  - agents → forge_agent (1对多)
 *  - workflow → forge_workflow_step (1对多)
 *  - manifests → forge_manifest (1对多)
 *  - 状态走 ReleaseStateMachine
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ForgeReleaseService {

    private final ForgeReleaseMapper releaseMapper;
    private final ForgeProjectMapper projectMapper;
    private final ForgeAgentMapper agentMapper;
    private final ForgeWorkflowStepMapper workflowMapper;
    private final ManifestGeneratorService manifestGenerator;
    private final DeploymentService deploymentService;
    private final ReleaseStateMachine stateMachine;
    private final ObjectMapper objectMapper;

    @Transactional
    public ForgeRelease create(Long ownerId, CreateReleaseRequest req) {
        log.info("[Release] 创建 v{} for project={}", req.getVersion(), req.getProjectId());

        // 1. 创建 release 主记录
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
        log.info("[Release] id={} created", release.getId());

        // 2. 持久化 agents 到子表
        if (req.getAgents() != null) {
            int idx = 0;
            for (Map<String, Object> a : req.getAgents()) {
                agentMapper.insert(ForgeAgent.builder()
                    .releaseId(release.getId())
                    .projectId(req.getProjectId())
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

        // 3. 持久化 workflow 到子表
        if (req.getWorkflow() != null) {
            for (Map<String, Object> w : req.getWorkflow()) {
                workflowMapper.insert(ForgeWorkflowStep.builder()
                    .projectId(req.getProjectId())
                    .releaseId(release.getId())
                    .stepNo(((Number) w.getOrDefault("step", 1)).intValue())
                    .name((String) w.getOrDefault("name", ""))
                    .type("agent")
                    .remark(null)
                    .createdAt(LocalDateTime.now())
                    .build());
            }
        }

        // 4. 同步更新 project.current_release_id
        projectMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ForgeProject>()
            .eq("id", req.getProjectId())
            .set("current_release_id", release.getId())
            .set("updated_at", LocalDateTime.now()));

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
        if (o instanceof List<?> l) {
            return l.stream().map(String::valueOf).toList();
        }
        return List.of(String.valueOf(o));
    }
}
