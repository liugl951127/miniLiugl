package com.minimax.ai.marketplace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.ai.entity.AgentGroup;
import com.minimax.ai.framework.group.GroupMember;
import com.minimax.ai.framework.group.GroupResult;
import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 智能体群组 REST API (V3.0.3)
 *
 * <p>API 列表 (统一 /api/v1/ai/group 前缀):
 * <ul>
 *   <li>POST /create            创建群组</li>
 *   <li>GET  /list              列出所有群组</li>
 *   <li>GET  /{groupId}         查群组详情</li>
 *   <li>POST /{groupId}/run     执行任务</li>
 *   <li>GET  /strategies/list   列出策略</li>
 *   <li>GET  /agents/list       列出可用 Agent</li>
 *   <li>POST /quick-create      快速创建 (用预置模板)</li>
 * </ul>
 */
@Tag(name = "智能体群组")
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/group")
@RequiredArgsConstructor
public class AgentGroupController {

    private final AgentGroupService service;
    private final ObjectMapper json = new ObjectMapper();

    @PostConstruct
    public void init() {
        service.init();
    }

    /**
     * 创建群组
     */
    @Operation(summary = "创建智能体群组")
    @PostMapping("/create")
    public Result<AgentGroup> create(@RequestBody Map<String, Object> body) {
        // 1. 取参数
        String name = (String) body.get("name");
        String description = (String) body.getOrDefault("description", "");
        String strategy = (String) body.getOrDefault("strategy", "PIPELINE");
        Long ownerId = body.get("ownerId") == null ? null : ((Number) body.get("ownerId")).longValue();
        String tags = (String) body.getOrDefault("tags", "");
        // 2. 解析 members
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> membersRaw = (List<Map<String, Object>>) body.get("members");
        if (membersRaw == null || membersRaw.isEmpty()) {
            return Result.fail(400, "members 不能为空");
        }
        try {
            List<GroupMember> members = json.readValue(json.writeValueAsString(membersRaw),
                    new TypeReference<List<GroupMember>>() {});
            // 3. 创建
            AgentGroup group = service.createGroup(name, description, strategy, members, ownerId, tags);
            return Result.ok(group);
        } catch (Exception e) {
            return Result.fail(500, "创建群组失败: " + e.getMessage());
        }
    }

    /**
     * 列出所有群组
     */
    @Operation(summary = "列出所有群组")
    @GetMapping("/list")
    public Result<List<AgentGroup>> list() {
        return Result.ok(service.listAll());
    }

    /**
     * 查群组详情
     */
    @Operation(summary = "查群组详情")
    @GetMapping("/{groupId}")
    public Result<AgentGroup> get(@PathVariable String groupId) {
        AgentGroup g = service.findByGroupId(groupId);
        if (g == null) return Result.fail(404, "群组不存在");
        return Result.ok(g);
    }

    /**
     * 执行任务
     */
    @Operation(summary = "执行群组任务")
    @PostMapping("/{groupId}/run")
    public Result<GroupResult> run(@PathVariable String groupId,
                                   @RequestBody Map<String, String> body) {
        String subject = body.get("subject");
        String input = body.getOrDefault("input", "");
        GroupResult result = service.executeTask(groupId, subject, input);
        return Result.ok(result);
    }

    /**
     * 列出策略
     */
    @Operation(summary = "列出协作策略")
    @GetMapping("/strategies/list")
    public Result<List<Map<String, String>>> strategies() {
        return Result.ok(service.listStrategies());
    }

    /**
     * 列出可用 Agent
     */
    @Operation(summary = "列出可用 Agent")
    @GetMapping("/agents/list")
    public Result<List<Map<String, String>>> agents() {
        return Result.ok(service.listAgents());
    }

    /**
     * 快速创建 (用预置模板)
     */
    @Operation(summary = "快速创建 (用预置模板)")
    @PostMapping("/quick-create")
    public Result<AgentGroup> quickCreate(@RequestBody Map<String, Object> body) {
        // 1. 模板名
        String template = (String) body.getOrDefault("template", "writing-team");
        Long ownerId = body.get("ownerId") == null ? null : ((Number) body.get("ownerId")).longValue();
        // 2. 选模板
        switch (template) {
            case "writing-team":
                return Result.ok(service.createGroup(
                        "写作团队",
                        "1 个 manager + 2 个 writer + 1 个 reviewer",
                        "PIPELINE",
                        List.of(
                                GroupMember.manager("g", "mgr"),
                                GroupMember.worker("g", "echo-writer", "writer"),
                                GroupMember.worker("g", "echo-summarizer", "summarizer"),
                                GroupMember.critic("g", "echo-reviewer")
                        ),
                        ownerId,
                        "writing,team"
                ));
            case "debate-panel":
                return Result.ok(service.createGroup(
                        "辩论小组",
                        "3 个 debater 并行辩论, 3 轮收敛",
                        "DEBATE",
                        List.of(
                                GroupMember.manager("g", "mgr"),
                                GroupMember.worker("g", "echo-analyzer", "analyzer"),
                                GroupMember.worker("g", "echo-writer", "writer"),
                                GroupMember.worker("g", "echo-coder", "coder")
                        ),
                        ownerId,
                        "debate,analysis"
                ));
            case "vote-council":
                return Result.ok(service.createGroup(
                        "投票委员会",
                        "5 个 agent 独立投票, 多数决",
                        "VOTE",
                        List.of(
                                GroupMember.manager("g", "mgr"),
                                GroupMember.worker("g", "echo-analyzer", "analyzer"),
                                GroupMember.worker("g", "echo-writer", "writer"),
                                GroupMember.worker("g", "echo-coder", "coder"),
                                GroupMember.worker("g", "echo-summarizer", "summarizer"),
                                GroupMember.worker("g", "echo-translator", "translator")
                        ),
                        ownerId,
                        "vote,council"
                ));
            case "swarm-mesh":
                return Result.ok(service.createGroup(
                        "群智网",
                        "所有 agent 并行贡献, manager 聚合",
                        "SWARM",
                        List.of(
                                GroupMember.manager("g", "mgr"),
                                GroupMember.worker("g", "echo-analyzer", "analyzer"),
                                GroupMember.worker("g", "echo-writer", "writer"),
                                GroupMember.worker("g", "echo-coder", "coder")
                        ),
                        ownerId,
                        "swarm,mesh"
                ));
            default:
                return Result.fail(400, "未知模板: " + template);
        }
    }
}
