package com.minimax.deployer.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minimax.deployer.dto.ParseRequirementsResponse;
import com.minimax.deployer.entity.ForgeProject;
import com.minimax.deployer.mapper.ForgeProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Forge 项目服务 (V2.0)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ForgeProjectService {

    private final ForgeProjectMapper projectMapper;
    private final RequirementsParserService parserService;

    /**
     * 创建项目 (并触发需求解析)
     */
    public ForgeProject createWithParsed(String source, String content, String documentName, String templateCode, Long ownerId) {
        // 1. 调用解析服务
        ParseRequirementsResponse parsed = parserService.parse(
            com.minimax.deployer.dto.ParseRequirementsRequest.builder()
                .source(source)
                .content(content)
                .documentName(documentName)
                .templateCode(templateCode)
                .userId(ownerId)
                .build()
        );

        // 2. 持久化项目
        ForgeProject project = ForgeProject.builder()
            .name(extractProjectName(content, parsed))
            .industry(extractIndustry(parsed))
            .scenario((String) parsed.getExtracted().get("scenario"))
            .rawRequirements(content)
            .parsedRequirements(toJson(parsed.getExtracted()))
            .recommendedAgents(toJson(parsed.getAgents()))
            .status("ANALYZED")
            .ownerId(ownerId)
            .build();
        projectMapper.insert(project);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        log.info("[Project] 创建项目 id={} name={}", project.getId(), project.getName());
        return project;
    }

    /**
     * 分页查询用户项目
     */
    public Page<ForgeProject> listByOwner(Long ownerId, int pageNum, int pageSize) {
        return projectMapper.selectPage(
            new Page<>(pageNum, pageSize),
            new QueryWrapper<ForgeProject>()
                .eq("owner_id", ownerId)
                .orderByDesc("updated_at")
        );
    }

    public ForgeProject getById(Long id) {
        return projectMapper.selectById(id);
    }

    public List<ForgeProject> listAll(int limit) {
        return projectMapper.selectList(
            new QueryWrapper<ForgeProject>()
                .orderByDesc("updated_at")
                .last("LIMIT " + Math.min(limit, 100))
        );
    }

    public void delete(Long id) {
        projectMapper.deleteById(id);
    }

    private String extractProjectName(String content, ParseRequirementsResponse parsed) {
        // 简化: 取第一行作为项目名
        int newline = content.indexOf('\n');
        if (newline > 0 && newline < 30) return content.substring(0, newline).trim();
        return content.substring(0, Math.min(20, content.length())).trim();
    }

    private String extractIndustry(ParseRequirementsResponse parsed) {
        String type = (String) parsed.getExtracted().get("projectType");
        if (type == null) return "通用";
        return type.split(" ")[0];
    }

    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) { return "{}"; }
    }
}
