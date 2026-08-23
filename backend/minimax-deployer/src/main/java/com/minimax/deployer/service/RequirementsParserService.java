package com.minimax.deployer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.deployer.dto.ParseRequirementsRequest;
import com.minimax.deployer.dto.ParseRequirementsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 需求解析服务 (V4.1)
 *
 * V4.1 改动:
 *  - System prompt 从 application.yml 注入 (@Value), 不再硬编码
 *  - 单 LlmClient 调用, 失败 usedFallback=true
 *  - 持久化: 解析阶段不写子表 (V4.1 修复双写), 只在 createRelease 时写
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RequirementsParserService {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    /** V4.1: System prompt 从 application.yml 注入, 改 prompt 不用重编译 */
    @Value("${agent-forge.parser.system-prompt}")
    private String systemPrompt;

    public ParseRequirementsResponse parse(ParseRequirementsRequest req) {
        long start = System.currentTimeMillis();
        String content = req.getContent();
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("需求内容不能为空");
        }

        Optional<String> llmResp = llmClient.chat(
            content.length() > 2000 ? content.substring(0, 2000) + "..." : content,
            systemPrompt
        );

        ParseRequirementsResponse resp = llmResp.map(this::parseLlmJson)
            .orElseGet(() -> ruleBasedParse(content));

        resp.setDurationMs(System.currentTimeMillis() - start);
        resp.setModel(llmClient.getModel());
        resp.setUsedFallback(llmResp.isEmpty());
        resp.setTotalTokens(content.length() / 4);
        log.info("[Parser] done, usedFallback={}, duration={}ms", resp.isUsedFallback(), resp.getDurationMs());
        return resp;
    }

    private ParseRequirementsResponse parseLlmJson(String raw) {
        String json = raw.trim();
        if (json.startsWith("```")) json = json.replaceAll("^```(?:json)?\\s*", "").replaceAll("```\\s*$", "").trim();
        int first = json.indexOf('{'), last = json.lastIndexOf('}');
        if (first >= 0 && last > first) json = json.substring(first, last + 1);

        try {
            JsonNode root = objectMapper.readTree(json);
            Map<String, Object> extracted = new LinkedHashMap<>();
            extracted.put("projectType", root.path("projectType").asText("通用 · 智能助手"));
            extracted.put("scenario", root.path("scenario").asText(""));
            extracted.put("features", asStringList(root.path("features")));
            extracted.put("scale", root.path("scale").asText("日均 1000+ 会话"));
            extracted.put("compliance", asStringList(root.path("compliance")));
            extracted.put("integrations", asStringList(root.path("integrations")));

            List<Map<String, Object>> agents = new ArrayList<>();
            for (JsonNode a : root.path("agents")) {
                Map<String, Object> am = new LinkedHashMap<>();
                am.put("name", a.path("name").asText("智能体"));
                am.put("role", a.path("role").asText(""));
                am.put("emoji", a.path("emoji").asText("🤖"));
                am.put("desc", a.path("desc").asText(""));
                am.put("color", a.path("color").asText("linear-gradient(135deg, #6366f1, #8b5cf6)"));
                am.put("tools", asStringList(a.path("tools")));
                am.put("model", a.path("model").asText(llmClient.getModel()));
                agents.add(am);
            }
            if (agents.isEmpty()) agents.add(defaultAgent());

            return ParseRequirementsResponse.builder()
                .extracted(extracted)
                .agents(agents)
                .workflow(asWorkflowList(root.path("workflow")))
                .build();
        } catch (Exception e) {
            log.warn("[Parser] LLM JSON 解析失败, 降级规则: {}", e.getMessage());
            return ruleBasedParse(raw);
        }
    }

    private List<String> asStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node.isArray()) for (JsonNode n : node) list.add(n.asText());
        return list;
    }

    private List<Map<String, Object>> asWorkflowList(JsonNode node) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (node.isArray()) for (JsonNode n : node) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("step", n.path("step").asInt(list.size() + 1));
            m.put("name", n.path("name").asText(""));
            list.add(m);
        }
        return list;
    }

    private ParseRequirementsResponse ruleBasedParse(String content) {
        String industry = detectIndustry(content);
        List<Map<String, Object>> agents = recommendAgents(industry);
        Map<String, Object> extracted = new LinkedHashMap<>();
        extracted.put("projectType", industry + " · 智能助手");
        extracted.put("scenario", industry + " 场景");
        extracted.put("features", List.of("咨询", "推荐", "查询"));
        extracted.put("scale", "日均 1000+ 会话");
        extracted.put("compliance", List.of("通用数据安全"));
        extracted.put("integrations", List.of("Web API"));
        return ParseRequirementsResponse.builder()
            .extracted(extracted)
            .agents(agents)
            .workflow(buildWorkflow(agents))
            .build();
    }

    private String detectIndustry(String c) {
        String[][] kws = {
            {"教育", "学员,课程,学校,学生,培训"},
            {"电商", "订单,商品,购物,物流,退换货"},
            {"金融", "银行,风控,信贷,投资,理财"},
            {"医疗", "医院,问诊,症状,药品,健康"},
            {"客服", "咨询,投诉,服务"},
            {"开发", "代码,开发,编程,测试,review"}
        };
        for (String[] k : kws) for (String kw : k[1].split(",")) if (c.contains(kw)) return k[0];
        return "通用";
    }

    private List<Map<String, Object>> recommendAgents(String industry) {
        return switch (industry) {
            case "教育" -> List.of(
                agent("小课", "课程顾问", "📚", "课程咨询", "linear-gradient(135deg, #6366f1, #8b5cf6)"),
                agent("小助", "退费专员", "💰", "退费流程", "linear-gradient(135deg, #f59e0b, #ef4444)")
            );
            case "电商" -> List.of(
                agent("小购", "购物顾问", "🛒", "商品推荐", "linear-gradient(135deg, #f59e0b, #ef4444)"),
                agent("小售", "售后客服", "📦", "退换货", "linear-gradient(135deg, #10b981, #06b6d4)")
            );
            case "金融" -> List.of(
                agent("小风", "风控官", "🛡️", "风险评估", "linear-gradient(135deg, #1e293b, #475569)"),
                agent("小投", "投资顾问", "💹", "投资建议", "linear-gradient(135deg, #10b981, #059669)")
            );
            case "医疗" -> List.of(agent("小医", "问诊医生", "⚕️", "症状问诊", "linear-gradient(135deg, #ec4899, #f43f5e)"));
            case "开发" -> List.of(agent("小审", "代码审查", "💻", "PR 审查", "linear-gradient(135deg, #10b981, #06b6d4)"));
            default -> List.of(agent("小助", "通用助理", "🤖", "通用对话", "linear-gradient(135deg, #6366f1, #8b5cf6)"));
        };
    }

    private Map<String, Object> agent(String name, String role, String emoji, String desc, String color) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("name", name); a.put("role", role); a.put("emoji", emoji);
        a.put("desc", desc); a.put("color", color);
        a.put("tools", List.of("基础工具")); a.put("model", llmClient.getModel());
        return a;
    }

    private List<Map<String, Object>> buildWorkflow(List<Map<String, Object>> agents) {
        List<Map<String, Object>> flow = new ArrayList<>();
        flow.add(step(1, "用户提问"));
        flow.add(step(2, "意图识别"));
        for (var a : agents) flow.add(step(flow.size() + 1, a.get("name") + " 处理"));
        flow.add(step(flow.size() + 1, "回复用户"));
        return flow;
    }

    private Map<String, Object> defaultAgent() {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("name", "小助"); a.put("role", "通用助理"); a.put("emoji", "🤖");
        a.put("desc", "通用对话"); a.put("color", "linear-gradient(135deg, #6366f1, #8b5cf6)");
        a.put("tools", List.of("基础工具")); a.put("model", llmClient.getModel());
        return a;
    }

    private Map<String, Object> step(int n, String name) { return Map.of("step", n, "name", name); }
}
