package com.minimax.ai.controller;

import com.minimax.ai.entity.AgentGroup;
import com.minimax.ai.framework.group.AutoAgentGroupGenerator;
import com.minimax.ai.framework.group.AutoAgentGroupGenerator.GeneratedGroup;
import com.minimax.ai.framework.group.AutoAgentGroupGenerator.GroupTemplate;
import com.minimax.ai.marketplace.AgentGroupMapper;
import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Agent Group 真实业务控制器 (V6.5+)
 * 多 Agent 协作 - 真实数据版本
 *
 * <p>注意：templates/listTemplates 已迁移至 AiAgentGroupAutoRealController，
 * 本控制器保留 execute/generate/template/groups 等业务接口。</p>
 *
 * @author Mavis
 * @since V6.5
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/agent-group")
@RequiredArgsConstructor
public class AiAgentGroupRealController {

    private final AutoAgentGroupGenerator generator;
    private final AgentGroupMapper agentGroupMapper;

    /**
     * 自动执行 Agent 群任务
     */
    @PostMapping("/auto/execute")
    public Result<Map<String, Object>> autoExecute(@RequestBody Map<String, Object> body) {
        Long templateId = ((Number) body.getOrDefault("templateId", 0)).longValue();
        String task = (String) body.getOrDefault("task", "");
        log.info("[AgentGroup] 执行: template={} task={}", templateId, task);

        // 从数据库查询群组
        AgentGroup group = agentGroupMapper.selectById(templateId);
        if (group == null) {
            return Result.error("群组不存在: " + templateId);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("templateId", templateId);
        resp.put("groupName", group.getName());
        resp.put("task", task);
        resp.put("status", "running");
        resp.put("groupId", group.getGroupId());
        resp.put("startedAt", LocalDateTime.now());
        return Result.ok(resp);
    }

    /**
     * 一句话生成 Agent 组
     */
    @PostMapping("/auto/generate")
    public Result<GeneratedGroup> autoGenerate(@RequestBody Map<String, Object> body) {
        String oneLiner = (String) body.getOrDefault("oneLiner", "");
        log.info("[AgentGroup] 自动生成: {}", oneLiner);
        GeneratedGroup generated = generator.generate(oneLiner);
        return Result.ok(generated);
    }

    /**
     * 模板详情（从数据库查询）
     */
    @GetMapping("/auto/template/{id}")
    public Result<Map<String, Object>> template(@PathVariable Long id) {
        AgentGroup group = agentGroupMapper.selectById(id);
        if (group == null) {
            return Result.error("模板不存在: " + id);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", group.getId());
        result.put("groupId", group.getGroupId());
        result.put("name", group.getName());
        result.put("description", group.getDescription());
        result.put("strategy", group.getStrategy());
        result.put("status", group.getStatus());
        result.put("tags", group.getTags());
        result.put("runCount", group.getRunCount());
        result.put("lastRunAt", group.getLastRunAt());
        return Result.ok(result);
    }

    /**
     * Agent 群列表（从数据库查询）
     */
    @GetMapping("/groups")
    public Result<List<Map<String, Object>>> groups() {
        List<AgentGroup> groups = agentGroupMapper.selectList(null);
        List<Map<String, Object>> result = groups.stream().map(g -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", g.getId());
            m.put("groupId", g.getGroupId());
            m.put("name", g.getName());
            m.put("strategy", g.getStrategy());
            m.put("status", g.getStatus());
            m.put("runCount", g.getRunCount());
            return m;
        }).collect(Collectors.toList());
        return Result.ok(result);
    }
}
