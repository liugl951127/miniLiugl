package com.minimax.deployer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
 * 需求解析服务 (V3.0)
 *
 * V3.0 升级: 接入真实 LLM (Qwen2.5 via minimax-ai)
 *  - 主用: minimax-ai 服务的本地 Qwen2.5 (ONNX)
 *  - 兜底: 规则引擎 (V2.0 mock, 关键词 + 行业识别)
 *
 * 工作流程:
 *  1. 构造 prompt (system + user)
 *  2. 调用 LlmClientService.chat()
 *  3. 解析 LLM JSON 响应 (或失败时降级到规则引擎)
 *  4. 包装为 ParseRequirementsResponse
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RequirementsParserService {

    private final ObjectMapper objectMapper;
    private final LlmClientService llmClient;

    @Value("${agent-forge.parser.use-llm:true}")
    private boolean useLlm;

    @Value("${agent-forge.parser.llm-model:qwen2.5-0.5b-instruct}")
    private String defaultModel;

    /** 解析 prompt system 模板 */
    private static final String SYSTEM_PROMPT = """
        你是一名资深的 AI 解决方案架构师, 专精企业级智能体 (Agent) 编排。
        你的任务: 解析用户的需求描述, 输出严格的 JSON 对象, 用于自动生成多智能体群。

        ## 输出要求
        1. 必须输出合法 JSON, 不要有 markdown 代码块标记
        2. 字段说明:
           - projectType: 项目类型, 格式 "<行业> · <场景>"
           - scenario: 一句话场景描述
           - features: 核心功能列表, 数组
           - scale: 预期规模描述
           - compliance: 合规要求列表, 数组
           - integrations: 集成需求列表, 数组
           - agents: 推荐智能体列表, 每个包含 {name, role, emoji, desc, tools, model, color}
           - workflow: 协作流程步骤, 数组, 每项 {step, name}

        ## 行业知识库
        - 教育: 课程咨询/退费/学习规划/质检 → 智能体: 课程顾问/退费专员/规划师/质检员
        - 电商: 商品推荐/退换货/物流/评价 → 智能体: 购物顾问/售后/物流/评价分析
        - 金融: 风控/投资/合规 → 智能体: 风控官/投资顾问/合规审核
        - 医疗: 问诊/导诊/健康 → 智能体: 问诊医生/导诊护士/健康顾问
        - 开发: 代码审查/测试/规范 → 智能体: 代码审查/测试生成/规范专家
        - 客服: 通用咨询/问题解答 → 智能体: 智能助理

        ## 颜色池 (agents[].color)
        - 紫蓝: linear-gradient(135deg, #6366f1, #8b5cf6)
        - 黄红: linear-gradient(135deg, #f59e0b, #ef4444)
        - 青绿: linear-gradient(135deg, #10b981, #06b6d4)
        - 粉红: linear-gradient(135deg, #ec4899, #f43f5e)
        - 深灰: linear-gradient(135deg, #1e293b, #475569)
        - 紫粉: linear-gradient(135deg, #8b5cf6, #ec4899)

        ## 例子
        输入: "在线教育平台 7×24 小时智能客服, 处理学员咨询、退费、课程推荐, 涉及未成年保护"
        输出: {"projectType":"教育 · 智能客服","scenario":"在线教育 7×24h 智能客服","features":["课程咨询","退费处理","推荐"],"scale":"日均 5000+","compliance":["个人信息保护法","未成年保护"],"integrations":["CRM","工单系统","支付系统"],"agents":[{"name":"小课","role":"课程顾问","emoji":"📚","desc":"回答课程问题","tools":["课程搜索"],"model":"Qwen2.5-7B","color":"linear-gradient(135deg, #6366f1, #8b5cf6)"}],"workflow":[{"step":1,"name":"用户提问"},{"step":2,"name":"意图识别"}]}

        只输出 JSON, 不要其他文字。
        """;

    /**
     * 解析需求
     */
    public ParseRequirementsResponse parse(ParseRequirementsRequest request) {
        long start = System.currentTimeMillis();
        String content = request.getContent();
        log.info("[Parser] 开始解析, source={}, contentLen={}, useLlm={}",
            request.getSource(), content.length(), useLlm);

        // 1. 尝试 LLM 解析
        if (useLlm) {
            Optional<ParseRequirementsResponse> llmResult = tryLlmParse(content);
            if (llmResult.isPresent()) {
                ParseRequirementsResponse r = llmResult.get();
                r.setDurationMs(System.currentTimeMillis() - start);
                r.setModel(defaultModel);
                r.setTotalTokens(content.length() / 4);
                log.info("[Parser] LLM 解析成功, duration={}ms", r.getDurationMs());
                return r;
            }
            log.warn("[Parser] LLM 解析失败, 降级到规则引擎");
        }

        // 2. 规则引擎兜底
        return ruleBasedParse(content, start);
    }

    /** LLM 解析尝试 */
    private Optional<ParseRequirementsResponse> tryLlmParse(String content) {
        // 截断过长的需求 (避免 token 超限)
        String truncated = content.length() > 2000 ? content.substring(0, 2000) + "..." : content;
        String userPrompt = "请解析以下需求:\n\n" + truncated;

        Optional<String> responseOpt = llmClient.chat(userPrompt, SYSTEM_PROMPT, defaultModel);
        if (responseOpt.isEmpty()) return Optional.empty();

        String raw = responseOpt.get();
        // LLM 偶尔会带 markdown 代码块, 清理
        String json = raw.trim();
        if (json.startsWith("```")) {
            json = json.replaceAll("^```(?:json)?\\s*", "").replaceAll("```\\s*$", "").trim();
        }
        // 提取第一段 JSON
        int firstBrace = json.indexOf('{');
        int lastBrace = json.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            json = json.substring(firstBrace, lastBrace + 1);
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            Map<String, Object> extracted = new LinkedHashMap<>();
            extracted.put("projectType", root.path("projectType").asText("通用 · 智能助手"));
            extracted.put("scenario", root.path("scenario").asText(content.substring(0, Math.min(50, content.length()))));
            extracted.put("features", jsonArrayToList(root.path("features")));
            extracted.put("scale", root.path("scale").asText("日均 1000+ 会话"));
            extracted.put("compliance", jsonArrayToList(root.path("compliance")));
            extracted.put("integrations", jsonArrayToList(root.path("integrations")));

            List<Map<String, Object>> agents = new ArrayList<>();
            for (JsonNode a : root.path("agents")) {
                Map<String, Object> am = new LinkedHashMap<>();
                am.put("name", a.path("name").asText("智能体"));
                am.put("role", a.path("role").asText(""));
                am.put("emoji", a.path("emoji").asText("🤖"));
                am.put("desc", a.path("desc").asText(""));
                am.put("color", a.path("color").asText("linear-gradient(135deg, #6366f1, #8b5cf6)"));
                am.put("tools", jsonArrayToList(a.path("tools")));
                am.put("model", a.path("model").asText("Qwen2.5-7B"));
                agents.add(am);
            }
            if (agents.isEmpty()) agents.add(defaultAgent());

            List<Map<String, Object>> workflow = new ArrayList<>();
            for (JsonNode w : root.path("workflow")) {
                Map<String, Object> wm = new LinkedHashMap<>();
                wm.put("step", w.path("step").asInt(workflow.size() + 1));
                wm.put("name", w.path("name").asText(""));
                workflow.add(wm);
            }
            if (workflow.isEmpty()) workflow = buildWorkflow("", agents);

            return Optional.of(ParseRequirementsResponse.builder()
                .extracted(extracted)
                .agents(agents)
                .workflow(workflow)
                .build());
        } catch (JsonProcessingException e) {
            log.warn("[Parser] JSON 解析失败: {}, raw={}", e.getMessage(), json.substring(0, Math.min(200, json.length())));
            return Optional.empty();
        }
    }

    private List<String> jsonArrayToList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node.isArray()) for (JsonNode n : node) list.add(n.asText());
        return list;
    }

    /** 规则引擎 (V2.0 fallback) */
    private ParseRequirementsResponse ruleBasedParse(String content, long start) {
        log.info("[Parser] 使用规则引擎 (V2.0 fallback)");
        String industry = detectIndustry(content);
        String scenario = extractScenario(content, industry);
        List<String> features = extractFeatures(content, industry);
        String scale = estimateScale(content);
        List<String> compliance = detectCompliance(content, industry);
        List<String> integrations = detectIntegrations(content);
        List<Map<String, Object>> agents = recommendAgents(industry, features);
        List<Map<String, Object>> workflow = buildWorkflow(industry, agents);

        Map<String, Object> extracted = new LinkedHashMap<>();
        extracted.put("projectType", industry + " · " + scenario);
        extracted.put("scenario", scenario);
        extracted.put("features", features);
        extracted.put("scale", scale);
        extracted.put("compliance", compliance);
        extracted.put("integrations", integrations);

        return ParseRequirementsResponse.builder()
            .extracted(extracted)
            .agents(agents)
            .workflow(workflow)
            .totalTokens(content.length() / 4)
            .durationMs(System.currentTimeMillis() - start)
            .model("Qwen2.5-72B (rule-engine-fallback)")
            .build();
    }

    private Map<String, Object> defaultAgent() {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("name", "小助");
        a.put("role", "通用助理");
        a.put("emoji", "🤖");
        a.put("desc", "通用对话助手");
        a.put("color", "linear-gradient(135deg, #6366f1, #8b5cf6)");
        a.put("tools", List.of("基础工具"));
        a.put("model", "Qwen2.5-7B");
        return a;
    }

    private String detectIndustry(String content) {
        Map<String, String[]> keywords = new LinkedHashMap<>();
        keywords.put("教育", new String[]{"教育", "学员", "课程", "学校", "学生", "培训"});
        keywords.put("电商", new String[]{"电商", "订单", "商品", "购物", "物流", "支付", "退换货"});
        keywords.put("金融", new String[]{"金融", "银行", "风控", "信贷", "投资", "理财", "支付"});
        keywords.put("医疗", new String[]{"医疗", "医院", "问诊", "症状", "药品", "健康", "病人"});
        keywords.put("客服", new String[]{"客服", "咨询", "服务", "投诉", "建议"});
        keywords.put("开发", new String[]{"代码", "开发", "编程", "测试", "部署", "review", "pr"});
        for (var entry : keywords.entrySet())
            for (String kw : entry.getValue())
                if (content.contains(kw)) return entry.getKey();
        return "通用";
    }

    private String extractScenario(String content, String industry) {
        int end = Math.min(content.length(), 60);
        int dotIdx = content.indexOf('。');
        if (dotIdx > 0 && dotIdx < 80) end = dotIdx + 1;
        return content.substring(0, end);
    }

    private List<String> extractFeatures(String content, String industry) {
        List<String> features = new ArrayList<>();
        String[] featureKw = {"咨询", "推荐", "查询", "下单", "支付", "退款", "审核", "审批", "通知", "提醒", "统计", "分析", "搜索"};
        for (String f : featureKw) if (content.contains(f) && !features.contains(f)) features.add(f);
        if (features.isEmpty()) features.addAll(Arrays.asList("咨询", "推荐", "查询"));
        return features;
    }

    private String estimateScale(String content) {
        if (content.contains("百万") || content.contains("100万")) return "日均 10万+ 会话, 峰值 1000+ 并发";
        if (content.contains("十万") || content.contains("10万")) return "日均 5万+ 会话, 峰值 500+ 并发";
        if (content.contains("万")) return "日均 5000+ 会话, 峰值 200 并发";
        return "日均 1000+ 会话, 峰值 50 并发";
    }

    private List<String> detectCompliance(String content, String industry) {
        List<String> result = new ArrayList<>();
        if (content.contains("个人信息") || content.contains("隐私")) result.add("个人信息保护法 (PIPL)");
        if (content.contains("未成年") || content.contains("儿童") || industry.equals("教育")) result.add("未成年保护");
        if (content.contains("金融") || content.contains("银行") || industry.equals("金融")) result.add("金融行业规范");
        if (content.contains("医疗") || industry.equals("医疗")) result.add("医疗数据合规");
        if (content.contains("等保") || content.contains("等级保护")) result.add("网络安全等级保护");
        if (result.isEmpty()) result.add("通用数据安全");
        return result;
    }

    private List<String> detectIntegrations(String content) {
        List<String> result = new ArrayList<>();
        if (content.contains("CRM")) result.add("CRM 系统");
        if (content.contains("ERP")) result.add("ERP 系统");
        if (content.contains("微信")) result.add("微信生态");
        if (content.contains("钉钉")) result.add("钉钉");
        if (content.contains("Slack")) result.add("Slack");
        if (content.contains("邮件") || content.contains("email")) result.add("邮件系统");
        if (result.isEmpty()) result.add("Web API");
        return result;
    }

    private List<Map<String, Object>> recommendAgents(String industry, List<String> features) {
        List<Map<String, Object>> agents = new ArrayList<>();
        switch (industry) {
            case "教育" -> {
                agents.add(agent("小课", "课程顾问", "📚", "回答课程相关问题, 推荐合适课程", "课程搜索", "linear-gradient(135deg, #6366f1, #8b5cf6)"));
                agents.add(agent("小助", "退费专员", "💰", "处理退费流程, 解释政策", "订单查询", "linear-gradient(135deg, #f59e0b, #ef4444)"));
                agents.add(agent("小审", "质检员", "🔍", "监控对话质量", "情感分析", "linear-gradient(135deg, #ec4899, #f43f5e)"));
            }
            case "电商" -> {
                agents.add(agent("小购", "购物顾问", "🛒", "推荐商品, 处理订单", "商品搜索", "linear-gradient(135deg, #f59e0b, #ef4444)"));
                agents.add(agent("小售", "售后客服", "📦", "退换货, 物流查询", "物流接口", "linear-gradient(135deg, #10b981, #06b6d4)"));
                agents.add(agent("小评", "评价分析", "⭐", "分析用户评价", "NLP", "linear-gradient(135deg, #8b5cf6, #ec4899)"));
            }
            case "金融" -> {
                agents.add(agent("小风", "风控官", "🛡️", "欺诈检测, 风险评估", "征信接口", "linear-gradient(135deg, #1e293b, #475569)"));
                agents.add(agent("小投", "投资顾问", "💹", "投资建议, 资产配置", "行情接口", "linear-gradient(135deg, #10b981, #059669)"));
                agents.add(agent("小审", "合规审核", "⚖️", "KYC, 反洗钱", "身份核验", "linear-gradient(135deg, #6366f1, #8b5cf6)"));
            }
            case "医疗" -> {
                agents.add(agent("小医", "问诊医生", "⚕️", "症状问诊, 初步诊断", "医学知识库", "linear-gradient(135deg, #ec4899, #f43f5e)"));
                agents.add(agent("小护", "导诊护士", "💊", "导诊, 用药指导", "药品库", "linear-gradient(135deg, #10b981, #06b6d4)"));
            }
            case "开发" -> {
                agents.add(agent("小审", "代码审查", "💻", "PR 审查, 规范检查", "Git API", "linear-gradient(135deg, #10b981, #06b6d4)"));
                agents.add(agent("小测", "测试生成", "🧪", "生成单元测试", "测试框架", "linear-gradient(135deg, #8b5cf6, #ec4899)"));
            }
            default -> agents.add(agent("小助", "通用助理", "🤖", "通用对话助手", "基础工具", "linear-gradient(135deg, #6366f1, #8b5cf6)"));
        }
        return agents;
    }

    private Map<String, Object> agent(String name, String role, String emoji, String desc, String tool, String color) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("name", name); a.put("role", role); a.put("emoji", emoji);
        a.put("desc", desc); a.put("color", color);
        a.put("tools", List.of(tool)); a.put("model", "Qwen2.5-7B");
        return a;
    }

    private List<Map<String, Object>> buildWorkflow(String industry, List<Map<String, Object>> agents) {
        List<Map<String, Object>> flow = new ArrayList<>();
        flow.add(step(1, "用户提问"));
        flow.add(step(2, "意图识别"));
        flow.add(step(3, "路由分发"));
        for (var a : agents) flow.add(step(flow.size() + 1, a.get("name") + " 处理"));
        flow.add(step(flow.size() + 1, "质检/审核"));
        flow.add(step(flow.size() + 1, "回复用户"));
        return flow;
    }

    private Map<String, Object> step(int n, String name) {
        return Map.of("step", n, "name", name);
    }
}
