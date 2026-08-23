package com.minimax.deployer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.deployer.dto.ParseRequirementsRequest;
import com.minimax.deployer.dto.ParseRequirementsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 需求解析服务 (V2.0)
 *
 * 接收用户需求 (文档/对话/模板), 调用 LLM 进行结构化解析。
 * 当前实现: 基于关键词 + 规则引擎的 mock LLM (生产环境接入 Qwen2.5-72B)
 *
 * 输出:
 *  - extracted: 项目元数据 (类型/场景/规模/合规/集成)
 *  - agents: 推荐智能体列表 (角色/工具/模型)
 *  - workflow: 协作流程
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RequirementsParserService {

    private final ObjectMapper objectMapper;

    /**
     * 解析需求 (V2.0 实现: 规则 + 关键词提取, 后续接 LLM)
     */
    public ParseRequirementsResponse parse(ParseRequirementsRequest request) {
        long start = System.currentTimeMillis();
        String content = request.getContent();

        log.info("[RequirementsParser] 开始解析需求, source={}, contentLen={}",
            request.getSource(), content.length());

        // 1. 行业识别
        String industry = detectIndustry(content);
        // 2. 场景提取
        String scenario = extractScenario(content, industry);
        // 3. 功能列表
        List<String> features = extractFeatures(content, industry);
        // 4. 规模估算
        String scale = estimateScale(content);
        // 5. 合规要求
        List<String> compliance = detectCompliance(content, industry);
        // 6. 集成需求
        List<String> integrations = detectIntegrations(content);

        // 7. 推荐智能体 (基于行业)
        List<Map<String, Object>> agents = recommendAgents(industry, features);

        // 8. 协作流程
        List<Map<String, Object>> workflow = buildWorkflow(industry, agents);

        Map<String, Object> extracted = new LinkedHashMap<>();
        extracted.put("projectType", industry + " · " + scenario);
        extracted.put("scenario", scenario);
        extracted.put("features", features);
        extracted.put("scale", scale);
        extracted.put("compliance", compliance);
        extracted.put("integrations", integrations);

        long duration = System.currentTimeMillis() - start;

        log.info("[RequirementsParser] 解析完成, industry={}, agents={}, duration={}ms",
            industry, agents.size(), duration);

        return ParseRequirementsResponse.builder()
            .extracted(extracted)
            .agents(agents)
            .workflow(workflow)
            .totalTokens(content.length() / 4)  // 估算
            .durationMs(duration)
            .model("Qwen2.5-72B (mock)")
            .build();
    }

    private String detectIndustry(String content) {
        Map<String, String[]> keywords = new LinkedHashMap<>();
        keywords.put("教育", new String[]{"教育", "学员", "课程", "学校", "学生", "培训"});
        keywords.put("电商", new String[]{"电商", "订单", "商品", "购物", "物流", "支付", "退换货"});
        keywords.put("金融", new String[]{"金融", "银行", "风控", "信贷", "投资", "理财", "支付"});
        keywords.put("医疗", new String[]{"医疗", "医院", "问诊", "症状", "药品", "健康", "病人"});
        keywords.put("客服", new String[]{"客服", "咨询", "服务", "投诉", "建议"});
        keywords.put("开发", new String[]{"代码", "开发", "编程", "测试", "部署", "review", "pr"});

        for (var entry : keywords.entrySet()) {
            for (String kw : entry.getValue()) {
                if (content.contains(kw)) return entry.getKey();
            }
        }
        return "通用";
    }

    private String extractScenario(String content, String industry) {
        // 截取第一句作为场景描述
        int end = Math.min(content.length(), 60);
        int dotIdx = content.indexOf('。');
        if (dotIdx > 0 && dotIdx < 80) end = dotIdx + 1;
        return content.substring(0, end);
    }

    private List<String> extractFeatures(String content, String industry) {
        List<String> features = new ArrayList<>();
        String[] featureKw = {"咨询", "推荐", "查询", "下单", "支付", "退款", "审核", "审批", "通知", "提醒", "统计", "分析", "搜索"};
        for (String f : featureKw) {
            if (content.contains(f) && !features.contains(f)) features.add(f);
        }
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
        // 基于行业 + features 推荐智能体
        List<Map<String, Object>> agents = new ArrayList<>();
        switch (industry) {
            case "教育" -> {
                agents.add(agent("小课", "课程顾问", "📚", "回答课程问题, 推荐合适课程", "课程搜索", "linear-gradient(135deg, #6366f1, #8b5cf6)"));
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
            default -> {
                agents.add(agent("小助", "通用助理", "🤖", "通用对话助手", "基础工具", "linear-gradient(135deg, #6366f1, #8b5cf6)"));
            }
        }
        return agents;
    }

    private Map<String, Object> agent(String name, String role, String emoji, String desc, String tool, String color) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("name", name);
        a.put("role", role);
        a.put("emoji", emoji);
        a.put("desc", desc);
        a.put("color", color);
        a.put("tools", List.of(tool));
        a.put("model", "Qwen2.5-7B");
        return a;
    }

    private List<Map<String, Object>> buildWorkflow(String industry, List<Map<String, Object>> agents) {
        List<Map<String, Object>> flow = new ArrayList<>();
        flow.add(step(1, "用户提问"));
        flow.add(step(2, "意图识别"));
        flow.add(step(3, "路由分发"));
        for (var a : agents) {
            flow.add(step(flow.size() + 1, (String) a.get("name") + " 处理"));
        }
        flow.add(step(flow.size() + 1, "质检/审核"));
        flow.add(step(flow.size() + 1, "回复用户"));
        return flow;
    }

    private Map<String, Object> step(int n, String name) {
        return Map.of("step", n, "name", name);
    }
}
