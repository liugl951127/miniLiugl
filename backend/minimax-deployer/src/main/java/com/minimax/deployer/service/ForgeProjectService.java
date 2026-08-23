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
 * Forge 项目服务 (V4.1)
 *
 * V4.1 关键改动: parse 阶段只持久化 Project 主表 + 暂存 agents/workflow
 * 到 ParseRequirementsResponse (返回给前端, 不入库), 只在 createRelease 时写子表。
 *
 * 避免 V4.0 的 "forge_agent 双写" 问题 (parse 时写一行 + release 时再写一行)。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ForgeProjectService {

    private final ForgeProjectMapper projectMapper;
    private final ForgeAgentMapper agentMapper;
    private final ForgeWorkflowStepMapper workflowMapper;
    private final RequirementsParserService parserService;

    /**
     * 创建项目 + LLM 解析
     * - 主表 forge_project: 写一次
     * - 子表 forge_agent / forge_workflow_step: 不写! (V4.1 修复, 避免双写)
     * - 返回 parsed 响应, 前端展示用
     */
    @Transactional
    public CreateResult createWithParsed(String source, String content, String documentName,
                                          String templateCode, String llmModel, Long ownerId) {
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

    /** 列出 release 的智能体 (V4.1: 改按 release_id 查, 不是 project_id) */
    public List<ForgeAgent> listAgentsByRelease(Long releaseId) {
        return agentMapper.selectList(
            new QueryWrapper<ForgeAgent>().eq("release_id", releaseId).orderByAsc("sort_order"));
    }

    /** 列出 release 的工作流 */
    public List<ForgeWorkflowStep> listWorkflowByRelease(Long releaseId) {
        return workflowMapper.selectList(
            new QueryWrapper<ForgeWorkflowStep>().eq("release_id", releaseId).orderByAsc("step_no"));
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

    public record CreateResult(ForgeProject project, ParseRequirementsResponse parsed) {}
}
