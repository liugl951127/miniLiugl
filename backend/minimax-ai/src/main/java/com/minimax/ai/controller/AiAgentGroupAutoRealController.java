package com.minimax.ai.controller;

import com.minimax.ai.framework.group.AutoAgentGroupGenerator;
import com.minimax.ai.framework.group.AutoAgentGroupGenerator.GeneratedGroup;
import com.minimax.ai.framework.group.AutoAgentGroupGenerator.GroupTemplate;
import com.minimax.ai.framework.group.GroupMember;
import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI Agent Group Auto 真实业务控制器 (V6.8.13)
 *
 * <h2>职责</h2>
 * 一句话生成智能体群: 调用 AutoAgentGroupGenerator 实现真正的 AI 生成逻辑。
 *
 * <h2>接口</h2>
 * POST /api/v1/ai/agent-group/auto/generate-auto  — 一句话生成
 * POST /api/v1/ai/agent-group/auto/template     — 按模板生成
 * GET  /api/v1/ai/agent-group/auto/templates    — 列出模板
 * POST /api/v1/ai/agent-group/auto/run         — 运行群组
 *
 * @author Mavis
 * @since V6.6
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/agent-group/auto")
@RequiredArgsConstructor
public class AiAgentGroupAutoRealController {

    private final AutoAgentGroupGenerator generator;

    // ============ 一句话生成 ============

    /**
     * 一句话生成智能体群
     *
     * 请求: { oneLiner: "帮我处理邮件，能自动分类、生成回复草稿、定时发送" }
     * 响应: { name, description, strategy, agents[], workflow[] }
     */
    @PostMapping("/generate-auto")
    public Result<Map<String, Object>> generate(@RequestBody Map<String, Object> body) {
        String oneLiner = body.get("oneLiner") != null
                ? body.get("oneLiner").toString()
                : "";
        log.info("[AutoGroup] 一句话生成: {}", oneLiner);
        try {
            GeneratedGroup group = generator.generate(oneLiner);
            return Result.ok(toFrontendFormat(group, oneLiner));
        } catch (Exception e) {
            log.error("[AutoGroup] 生成失败: {}", e.getMessage(), e);
            return Result.fail("生成失败: " + e.getMessage());
        }
    }

    // ============ 按模板生成 ============

    /**
     * 按模板生成智能体群
     *
     * 请求: { template: "CODER_TEAM", description: "写一个爬虫" }
     */
    @PostMapping("/template")
    public Result<Map<String, Object>> fromTemplate(@RequestBody Map<String, Object> body) {
        String template = body.get("template") != null
                ? body.get("template").toString()
                : "WRITING_TEAM";
        String description = body.get("description") != null
                ? body.get("description").toString()
                : "";
        log.info("[AutoGroup] 按模板生成: template={}", template);
        try {
            GeneratedGroup group = generator.generateFromTemplate(template, description);
            return Result.ok(toFrontendFormat(group, description));
        } catch (Exception e) {
            log.error("[AutoGroup] 模板生成失败: {}", e.getMessage(), e);
            return Result.fail("模板生成失败: " + e.getMessage());
        }
    }

    // ============ 列出模板 ============

    /**
     * 列出所有可用模板
     */
    @GetMapping("/templates")
    public Result<Map<String, Object>> listTemplates() {
        Map<String, GroupTemplate> templates = generator.listTemplates();
        Map<String, Object> result = new LinkedHashMap<>();
        for (var e : templates.entrySet()) {
            GroupTemplate t = e.getValue();
            result.put(e.getKey(), Map.of(
                    "name", t.name(),
                    "description", t.description(),
                    "strategy", t.strategy().name(),
                    "roleCount", t.roles().size()
            ));
        }
        return Result.ok(result);
    }

    // ============ 运行群组 ============

    /**
     * 运行智能体群
     *
     * 请求: { groupId: "grp-xxx", input: "..." }
     */
    @PostMapping("/run")
    public Result<Map<String, Object>> run(@RequestBody Map<String, Object> body) {
        String groupId = body.get("groupId") != null
                ? body.get("groupId").toString()
                : "";
        log.info("[AutoGroup] 运行群组: {}", groupId);
        // TODO: 对接真正的 Agent 执行引擎 (GroupOrchestrator)
        return Result.ok(Map.of(
                "runId", "run-" + System.currentTimeMillis(),
                "groupId", groupId,
                "status", "queued",
                "message", "群组已加入执行队列"
        ));
    }

    // ============ 内部工具 ============

    /**
     * 将内部 GeneratedGroup 转换为前端期望的格式
     */
    private Map<String, Object> toFrontendFormat(GeneratedGroup group, String description) {
        // 1. Agent 列表
        List<Map<String, Object>> agents = new ArrayList<>();
        List<String> workflow = new ArrayList<>();
        for (GroupMember m : group.members()) {
            String displayName = displayName(m.getAgentName(), m.getRole().name());
            agents.add(Map.of(
                    "name", displayName,
                    "role", m.getRole().name(),
                    "tools", inferTools(m.getAgentName(), m.getCapability()),
                    "prompt", buildPrompt(m.getAgentName(), m.getCapability(), description)
            ));
            workflow.add(displayName);
        }

        // 2. 工作流：MANAGER → WORKER(s) → CRITIC
        List<String> ordered = new ArrayList<>();
        group.members().stream()
                .sorted(Comparator.comparingInt(GroupMember::getOrder))
                .forEach(m -> ordered.add(displayName(m.getAgentName(), m.getRole().name())));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupId", group.groupId());
        result.put("name", group.name());
        result.put("description", group.description());
        result.put("strategy", group.strategy().name());
        result.put("agents", agents);
        result.put("workflow", ordered);
        result.put("template", group.template() != null ? group.template().name() : "");
        result.put("createdAt", group.createdAt());
        return result;
    }

    /** 根据 persona 生成可读的中文名 */
    private String displayName(String persona, String role) {
        if (persona == null) return role;
        return switch (persona.toLowerCase()) {
            case "writer-manager", "manager"           -> "📋 任务管理器";
            case "outliner"                             -> "📝 大纲生成员";
            case "drafter"                              -> "✍️ 正稿撰写员";
            case "polisher", "judge"                   -> "🔍 审核评论员";
            case "lead-analyst", "research-lead"       -> "📊 分析主管";
            case "data-analyst"                         -> "💾 数据分析师";
            case "market-analyst"                       -> "🏪 市场分析师";
            case "risk-analyst"                        -> "⚠️ 风险分析师";
            case "debate-moderator"                    -> "⚖️ 辩论主持人";
            case "pro-agent"                           -> "✅ 正方代理";
            case "con-agent"                           -> "❌ 反方代理";
            case "vote-chair"                          -> "🗳️ 投票主席";
            case "voter-1", "voter-2", "voter-3"     -> "🗳️ 投票成员";
            case "tech-lead"                           -> "👨‍💻 技术负责人";
            case "designer"                            -> "🎨 架构设计师";
            case "developer"                           -> "💻 开发工程师";
            case "code-reviewer"                       -> "🔍 代码评审员";
            case "explorer-1", "explorer-2", "explorer-3" -> "🔎 探索成员";
            default -> "🤖 " + persona;
        };
    }

    /** 根据 persona 推断工具集 */
    private List<String> inferTools(String persona, String capability) {
        if (persona == null) return List.of();
        String p = persona.toLowerCase();
        if (p.contains("analyst") || p.contains("data"))
            return List.of("data-query", "chart-gen", "calc");
        if (p.contains("code") || p.contains("dev") || p.contains("tech"))
            return List.of("code-exec", "git", "search");
        if (p.contains("writer") || p.contains("draft") || p.contains("polish"))
            return List.of("doc-write", "search");
        if (p.contains("research") || p.contains("explorer"))
            return List.of("web-search", "read-doc", "summarize");
        if (p.contains("debate") || p.contains("vote"))
            return List.of("search", "calc", "summarize");
        return List.of("search", "calc");
    }

    /** 构建 Agent 的 system prompt */
    private String buildPrompt(String persona, String capability, String task) {
        String cap = capability != null ? capability : "general";
        if (task == null || task.isBlank()) task = "用户请求";
        return String.format(
                "你是一个 %s。你的职责是 %s。当前任务: %s。请专注于你的职责，完成后汇报结果。",
                persona, cap, task
        );
    }
}
