package com.minimax.deployer.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minimax.deployer.dto.ParseRequirementsRequest;
import com.minimax.deployer.dto.ParseRequirementsResponse;
import com.minimax.deployer.entity.ForgeAgent;
import com.minimax.deployer.entity.ForgeProject;
import com.minimax.deployer.entity.ForgeWorkflowStep;
import com.minimax.deployer.mapper.ForgeAgentMapper;
import com.minimax.deployer.mapper.ForgeProjectMapper;
import com.minimax.deployer.mapper.ForgeWorkflowStepMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Forge 项目服务 (V4.0)
 *
 * V4.0 清理:
 *  - 删 parsed_requirements / recommended_agents 字符串
 *  - agents → forge_agent 子表
 *  - workflow → forge_workflow_step 子表
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ForgeProjectService {

    private final ForgeProjectMapper projectMapper;
    private final ForgeAgentMapper agentMapper;
    private final ForgeWorkflowStepMapper workflowMapper;
    private final RequirementsParserService parserService;

    @Transactional
    public CreateResult createWithParsed(String source, String content, String documentName, String templateCode, String llmModel, Long ownerId) {
        ParseRequirementsResponse parsed = parserService.parse(
            ParseRequirementsRequest.builder()
                .source(source).content(content)
                .documentName(documentName).templateCode(templateCode)
                .userId(ownerId).llmModel(llmModel)
                .build()
        );

        ForgeProject project = ForgeProject.builder()
            .name(extractProjectName(content))
            .industry(extractIndustry(parsed))
            .scenario((String) parsed.getExtracted().getOrDefault("scenario", ""))
            .rawRequirements(content)
            .status("ANALYZED")
            .ownerId(ownerId)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        projectMapper.insert(project);

        // agents → forge_agent 子表
        if (parsed.getAgents() != null) {
            int idx = 0;
            for (Map<String, Object> a : parsed.getAgents()) {
                agentMapper.insert(ForgeAgent.builder()
                    .projectId(project.getId())
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

        // workflow → forge_workflow_step 子表
        if (parsed.getWorkflow() != null) {
            for (Map<String, Object> w : parsed.getWorkflow()) {
                workflowMapper.insert(ForgeWorkflowStep.builder()
                    .projectId(project.getId())
                    .stepNo(((Number) w.getOrDefault("step", 1)).intValue())
                    .name((String) w.getOrDefault("name", ""))
                    .type("agent")
                    .createdAt(LocalDateTime.now())
                    .build());
            }
        }

        log.info("[Project] 创建项目 id={} name={} usedFallback={}",
            project.getId(), project.getName(), parsed.isUsedFallback());
        return new CreateResult(project, parsed);
    }

    public Page<ForgeProject> listByOwner(Long ownerId, int pageNum, int pageSize) {
        return projectMapper.selectPage(new Page<>(pageNum, pageSize),
            new QueryWrapper<ForgeProject>().eq("owner_id", ownerId).orderByDesc("updated_at"));
    }

    public ForgeProject getById(Long id) { return projectMapper.selectById(id); }
    public void delete(Long id) { projectMapper.deleteById(id); }

    public List<ForgeAgent> listAgents(Long projectId) {
        return agentMapper.selectList(new QueryWrapper<ForgeAgent>().eq("project_id", projectId).orderByAsc("sort_order"));
    }

    public List<ForgeWorkflowStep> listWorkflow(Long projectId) {
        return workflowMapper.selectList(new QueryWrapper<ForgeWorkflowStep>().eq("project_id", projectId).orderByAsc("step_no"));
    }

    private String extractProjectName(String content) {
        int nl = content.indexOf('\n');
        if (nl > 0 && nl < 30) return content.substring(0, nl).trim();
        return content.substring(0, Math.min(20, content.length())).trim();
    }

    private String extractIndustry(ParseRequirementsResponse parsed) {
        String type = (String) parsed.getExtracted().get("projectType");
        if (type == null) return "通用";
        return type.split(" ")[0];
    }

    private List<String> toStringList(Object o) {
        if (o == null) return List.of();
        if (o instanceof List<?> l) return l.stream().map(String::valueOf).toList();
        return List.of(String.valueOf(o));
    }

    /** 创建结果包装 */
    public record CreateResult(ForgeProject project, ParseRequirementsResponse parsed) {}
}
