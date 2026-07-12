package com.minimax.ai.framework.group;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.Instant;

/**
 * 一句话自动生成智能体群 (V3.4.2 自研)
 *
 * <h3>功能</h3>
 * 输入一句话 (e.g. "写一份季报"), 平台自动:
 *   1. 识别意图 (写/分析/总结/规划/...)
 *   2. 选择模板 (writing-team / analyst-team / debate-panel / ...)
 *   3. 拼装 Agent 群 (MANAGER + N WORKER + CRITIC)
 *   4. 选择协作策略 (PIPELINE / VOTE / DEBATE / SWARM)
 *   5. 立即创建并可执行
 *
 * <h3>模板库 (内置 6 个)</h3>
 * <ul>
 *   <li>WRITING_TEAM  — 写作团队 (1 manager + 2 worker + 1 critic, PIPELINE)</li>
 *   <li>ANALYST_TEAM  — 分析团队 (1 manager + 3 worker, SWARM)</li>
 *   <li>DEBATE_PANEL  — 辩论小组 (1 manager + 2 worker + 1 critic, DEBATE)</li>
 *   <li>VOTE_COUNCIL  — 投票委员会 (1 manager + 3 worker, VOTE)</li>
 *   <li>CODER_TEAM    — 编码团队 (1 manager + 2 worker + 1 critic, PIPELINE)</li>
 *   <li>RESEARCH_TEAM — 研究团队 (1 manager + 3 worker, SWARM)</li>
 * </ul>
 *
 * <h3>复杂度</h3>
 * O(K + N) K=关键词数 N=Agent 数 (10-100 量级)
 */
@Slf4j
@Service
public class AutoAgentGroupGenerator {

    /** 群模板 (用 1 步可序列化的简单结构) */
    public record GroupTemplate(
            String name,
            String description,
            GroupStrategy strategy,
            List<TemplateRole> roles) {
    }

    public record TemplateRole(
            GroupRole role,
            String persona,
            String responsibility) {
    }

    /** 意图 → 模板映射 (按关键词优先级匹配) */
    private final Map<String, List<String>> intentToTemplate = new HashMap<>();
    {
        intentToTemplate.put("write", List.of("WRITING_TEAM"));
        intentToTemplate.put("writing", List.of("WRITING_TEAM"));
        intentToTemplate.put("draft", List.of("WRITING_TEAM"));
        intentToTemplate.put("report", List.of("WRITING_TEAM"));
        intentToTemplate.put("analyze", List.of("ANALYST_TEAM"));
        intentToTemplate.put("analysis", List.of("ANALYST_TEAM"));
        intentToTemplate.put("evaluate", List.of("DEBATE_PANEL"));
        intentToTemplate.put("compare", List.of("DEBATE_PANEL"));
        intentToTemplate.put("decide", List.of("VOTE_COUNCIL"));
        intentToTemplate.put("vote", List.of("VOTE_COUNCIL"));
        intentToTemplate.put("code", List.of("CODER_TEAM"));
        intentToTemplate.put("implement", List.of("CODER_TEAM"));
        intentToTemplate.put("research", List.of("RESEARCH_TEAM"));
        intentToTemplate.put("investigate", List.of("RESEARCH_TEAM"));
    }

    /** 中文意图 → 模板 (顺序敏感: 更具体的优先) */
    private static final List<Map.Entry<Pattern, String>> CN_INTENT_PATTERNS = List.of(
            Map.entry(Pattern.compile("编码|写代码|实现|开发|编程|脚本"), "CODER_TEAM"),
            Map.entry(Pattern.compile("分析|评估|调查|调研|洞察"), "ANALYST_TEAM"),
            Map.entry(Pattern.compile("辩论|对比|比较|讨论|争辩"), "DEBATE_PANEL"),
            Map.entry(Pattern.compile("投票|表决|决策|决定|评选"), "VOTE_COUNCIL"),
            Map.entry(Pattern.compile("研究|考察|探索|学术"), "RESEARCH_TEAM"),
            Map.entry(Pattern.compile("起草|撰写|草拟|写报告|作文|文案|总结"), "WRITING_TEAM"),
            Map.entry(Pattern.compile("写"), "WRITING_TEAM")
    );

    /** 模板库 */
    private final Map<String, GroupTemplate> templates = new ConcurrentHashMap<>();

    public AutoAgentGroupGenerator() {
        initTemplates();
    }

    private void initTemplates() {
        // 1. 写作团队
        templates.put("WRITING_TEAM", new GroupTemplate(
                "WRITING_TEAM",
                "写作团队: 大纲 → 撰写 → 润色 → 审校",
                GroupStrategy.PIPELINE,
                List.of(
                        new TemplateRole(GroupRole.MANAGER, "writer-manager", "拆写作任务, 派发, 聚合"),
                        new TemplateRole(GroupRole.WORKER, "outliner", "生成大纲"),
                        new TemplateRole(GroupRole.WORKER, "drafter", "正文撰写"),
                        new TemplateRole(GroupRole.CRITIC, "polisher", "润色 + 审校")
                )
        ));
        // 2. 分析团队
        templates.put("ANALYST_TEAM", new GroupTemplate(
                "ANALYST_TEAM",
                "分析团队: 多个 Worker 并行分析, Manager 汇总",
                GroupStrategy.SWARM,
                List.of(
                        new TemplateRole(GroupRole.MANAGER, "lead-analyst", "拆分析维度, 汇总结论"),
                        new TemplateRole(GroupRole.WORKER, "data-analyst", "数据分析"),
                        new TemplateRole(GroupRole.WORKER, "market-analyst", "市场分析"),
                        new TemplateRole(GroupRole.WORKER, "risk-analyst", "风险分析")
                )
        ));
        // 3. 辩论小组
        templates.put("DEBATE_PANEL", new GroupTemplate(
                "DEBATE_PANEL",
                "辩论小组: 两 Worker 辩论, Critic 评出最优",
                GroupStrategy.DEBATE,
                List.of(
                        new TemplateRole(GroupRole.MANAGER, "debate-moderator", "主持, 分配正反方"),
                        new TemplateRole(GroupRole.WORKER, "pro-agent", "正方观点"),
                        new TemplateRole(GroupRole.WORKER, "con-agent", "反方观点"),
                        new TemplateRole(GroupRole.CRITIC, "judge", "评审辩论, 选最优")
                )
        ));
        // 4. 投票委员会
        templates.put("VOTE_COUNCIL", new GroupTemplate(
                "VOTE_COUNCIL",
                "投票委员会: 多 Worker 并行提议, 加权投票",
                GroupStrategy.VOTE,
                List.of(
                        new TemplateRole(GroupRole.MANAGER, "vote-chair", "汇总投票"),
                        new TemplateRole(GroupRole.WORKER, "voter-1", "投票者 1"),
                        new TemplateRole(GroupRole.WORKER, "voter-2", "投票者 2"),
                        new TemplateRole(GroupRole.WORKER, "voter-3", "投票者 3")
                )
        ));
        // 5. 编码团队
        templates.put("CODER_TEAM", new GroupTemplate(
                "CODER_TEAM",
                "编码团队: 设计 → 实现 → 评审",
                GroupStrategy.PIPELINE,
                List.of(
                        new TemplateRole(GroupRole.MANAGER, "tech-lead", "拆解技术任务"),
                        new TemplateRole(GroupRole.WORKER, "designer", "设计架构"),
                        new TemplateRole(GroupRole.WORKER, "developer", "写代码"),
                        new TemplateRole(GroupRole.CRITIC, "code-reviewer", "code review")
                )
        ));
        // 6. 研究团队
        templates.put("RESEARCH_TEAM", new GroupTemplate(
                "RESEARCH_TEAM",
                "研究团队: 多维探索, Manager 整合报告",
                GroupStrategy.SWARM,
                List.of(
                        new TemplateRole(GroupRole.MANAGER, "research-lead", "定方向, 汇报告"),
                        new TemplateRole(GroupRole.WORKER, "explorer-1", "路径 1 探索"),
                        new TemplateRole(GroupRole.WORKER, "explorer-2", "路径 2 探索"),
                        new TemplateRole(GroupRole.WORKER, "explorer-3", "路径 3 探索")
                )
        ));
    }

    /**
     * 一句话生成群组 (核心入口)
     *
     * @param oneLiner 一句话 (e.g. "写一份季报")
     * @return 群组定义 (含 name/strategy/members)
     */
    public GeneratedGroup generate(String oneLiner) {
        if (oneLiner == null || oneLiner.isBlank()) {
            // 默认 = 写作团队
            return generateFromTemplate("WRITING_TEAM", oneLiner);
        }
        // 1. 识别意图 → 选模板
        String templateName = pickTemplate(oneLiner);
        return generateFromTemplate(templateName, oneLiner);
    }

    /**
     * 直接用模板生成
     */
    public GeneratedGroup generateFromTemplate(String templateName, String description) {
        GroupTemplate tmpl = templates.get(templateName);
        if (tmpl == null) throw new IllegalArgumentException("未知模板: " + templateName);
        String gid = "grp-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
        // 拼装 GroupMember 列表
        List<GroupMember> members = new ArrayList<>();
        for (TemplateRole tr : tmpl.roles()) {
            String mid = "mem-" + UUID.randomUUID().toString().substring(0, 8);
            double w = switch (tr.role()) {
                case MANAGER -> 2.0;
                case CRITIC -> 1.5;
                case WORKER -> 1.0;
                case OBSERVER -> 0.0;
            };
            members.add(new GroupMember(mid, gid, tr.persona(), tr.role(), w, tr.responsibility(), 0));
        }
        log.info("[AutoGroup] 一句话 '{}' → 模板={}, 群={}, agent 数={}",
                description, templateName, gid, members.size());
        return new GeneratedGroup(
                gid,
                "Auto-" + templateName,
                description == null ? tmpl.description() : description,
                tmpl.strategy(),
                members,
                tmpl,
                Instant.now().toString()
        );
    }

    /**
     * 识别意图 → 选模板
     */
    private String pickTemplate(String oneLiner) {
        String lower = oneLiner.toLowerCase();
        // 1. 中文关键词
        for (Map.Entry<Pattern, String> e : CN_INTENT_PATTERNS) {
            Matcher m = e.getKey().matcher(oneLiner);
            if (m.find()) return e.getValue();
        }
        // 2. 英文关键词
        for (Map.Entry<String, List<String>> e : intentToTemplate.entrySet()) {
            if (lower.contains(e.getKey())) return e.getValue().get(0);
        }
        // 3. fallback = 写作
        return "WRITING_TEAM";
    }

    /**
     * 列出所有模板
     */
    public Map<String, GroupTemplate> listTemplates() {
        return Collections.unmodifiableMap(templates);
    }

    /**
     * 生成的群组
     */
    public record GeneratedGroup(
            String groupId,
            String name,
            String description,
            GroupStrategy strategy,
            List<GroupMember> members,
            GroupTemplate template,
            String createdAt) {
    }
}
