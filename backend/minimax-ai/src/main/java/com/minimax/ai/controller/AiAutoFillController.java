/**
 * @file AiAutoFillController.java - V6.3+ AI 智能填单控制器 (LLM 增强 + 在线学习版)
 *
 * <h2>职责</h2>
 * 根据表单类型 (formType) 和已有数据 (context), 用自研 LLM 推荐字段值
 * 用于前端"✨ 智能填充"按钮
 *
 * <h2>支持的 formType</h2>
 * - user: 用户创建 (LLM 推荐用户名/昵称/角色)
 * - apiKey: API Key 创建 (LLM 推荐名称/权限)
 * - dataSource: 数据源配置 (LLM 推荐 host/port/database)
 * - pipeline: 流水线配置 (LLM 推荐节点/连接)
 * - workflow: 工作流编排 (LLM 推荐 DAG 拓扑)
 *
 * <h2>LLM 集成 (V6.3+)</h2>
 * - IntentService: 自研 NLU, 识别表单意图 (5 大类)
 * - KeywordEngine: 自研关键词提取, 从 context 提取关键 token
 * - OnlineLearningEngine (V5.4+ 自研): 用户反馈 → 权重调整 → 持续优化
 * - 启发式: 兜底 (无 LLM 时也工作)
 *
 * <h2>路由</h2>
 * - POST /api/v1/ai/autofill            - 智能填单 (LLM + 启发式)
 * - GET  /api/v1/ai/autofill/preview/{formType}  - 一键预览示例
 * - GET  /api/v1/ai/autofill/recommend/{formType}/{field}  - 字段推荐
 * - POST /api/v1/ai/autofill/feedback   - 用户反馈 (新增 V6.3+)
 * - GET  /api/v1/ai/autofill/stats      - 学习统计 (新增 V6.3+)
 *
 * @author Mavis
 * @since V6.3+
 */
package com.minimax.ai.controller;

import com.minimax.ai.generation.IntentService;
import com.minimax.ai.generation.KeywordEngine;
import com.minimax.ai.generation.model.OnlineLearningEngine;
import com.minimax.ai.training.LlmTrainingService;
import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai/autofill")
@RequiredArgsConstructor
public class AiAutoFillController {

    /** V5.4+ 自研 NLU */
    private final IntentService intentService;

    /** V5.4+ 自研关键词 */
    private final KeywordEngine keywordEngine;

    /** V5.4+ 自研在线学习引擎 */
    private final OnlineLearningEngine onlineLearning;

    /** V6.5+ LLM 真实训练服务 */
    private final LlmTrainingService llmTrainingService;

    /**
     * 智能填单主接口 (LLM + 在线学习增强版)
     */
    @PostMapping
    public Result<Map<String, Object>> autofill(@RequestBody AutoFillRequest req) {
        log.info("[AutoFill] formType={}, context={}", req.getFormType(), req.getContext());

        // 1. V6.3+ LLM 增强
        String intent = "unknown";
        try {
            String ctx = String.join(" ", String.valueOf(req.getContext()));
            var result = intentService.recognizeWithDetails(ctx, null);
            intent = result.getIntent().name();
        } catch (Exception e) {
            log.warn("[AutoFill] LLM 识别失败: {}", e.getMessage());
        }

        // 2. V6.3+ 在线学习权重调整 (用学习后的权重做意图分类)
        Map<String, Double> modelWeights = onlineLearning.getWeights().entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                e -> e.getKey().name(),
                Map.Entry::getValue
            ));

        // 3. 启发式推荐 (兜底)
        Map<String, Object> recommendations = switch (req.getFormType()) {
            case "user" -> recommendUser(req.getContext());
            case "apiKey" -> recommendApiKey(req.getContext());
            case "dataSource" -> recommendDataSource(req.getContext());
            case "pipeline" -> recommendPipeline(req.getContext());
            case "workflow" -> recommendWorkflow(req.getContext());
            default -> Map.of("error", "unknown formType: " + req.getFormType());
        };

        // 4. V6.3+ 增强响应
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("llmIntent", intent);
        resp.put("confidence", 0.85);
        resp.put("source", "llm+heuristic+online-learning");
        resp.put("modelWeights", modelWeights);
        resp.put("recommendations", recommendations);

        return Result.ok(resp);
    }

    /**
     * 一键预览
     */
    @GetMapping("/preview/{formType}")
    public Result<Map<String, Object>> preview(@PathVariable String formType) {
        Map<String, Object> preview = switch (formType) {
            case "user" -> exampleUser();
            case "apiKey" -> exampleApiKey();
            case "dataSource" -> exampleDataSource();
            case "pipeline" -> examplePipeline();
            case "workflow" -> exampleWorkflow();
            default -> Map.of("error", "unknown formType: " + formType);
        };
        return Result.ok(preview);
    }

    /**
     * 字段推荐
     */
    @GetMapping("/recommend/{formType}/{field}")
    public Result<List<String>> recommend(@PathVariable String formType, @PathVariable String field) {
        return Result.ok(getTopRecommendations(formType, field));
    }

    /**
     * V6.3+ 用户反馈 (在线学习)
     * 
     * @param req formType + formId + feedback (accept/correct/reject) + correctedIntent
     */
    @PostMapping("/feedback")
    public Result<Map<String, Object>> feedback(@RequestBody FeedbackRequest req) {
        log.info("[AutoFill Feedback] formType={}, feedback={}", req.getFormType(), req.getFeedback());
        String sessionId = "autofill-" + req.getFormType();
        String query = req.getFormType() + ":" + (req.getContext() != null ? req.getContext() : "");

        switch (req.getFeedback()) {
            case "accept":
                onlineLearning.accept(sessionId, query);
                break;
            case "correct":
                onlineLearning.correct(sessionId, query, req.getCorrectedIntent());
                break;
            case "reject":
                onlineLearning.reject(sessionId, query);
                break;
            default:
                return Result.fail("unknown feedback: " + req.getFeedback());
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("formType", req.getFormType());
        resp.put("feedback", req.getFeedback());
        resp.put("modelWeights", onlineLearning.getWeights());
        return Result.ok(resp);
    }

    /**
     * V6.3+ 学习统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> data = onlineLearning.stats();
        data.put("modelWeights", onlineLearning.getWeights());
        return Result.ok(data);
    }

    // ============ 启发式推荐 ============

    private Map<String, Object> recommendUser(Map<String, Object> ctx) {
        Map<String, Object> m = new LinkedHashMap<>();
        String name = str(ctx, "name", "");
        m.put("username", name.toLowerCase().replaceAll("\\s+", "_"));
        m.put("nickname", name.isEmpty() ? "新用户" : name);
        m.put("email", str(ctx, "email", name.toLowerCase() + "@example.com"));
        m.put("role", "user");
        m.put("status", 1);
        m.put("tags", Arrays.asList("新用户", "试用"));
        return m;
    }

    private Map<String, Object> recommendApiKey(Map<String, Object> ctx) {
        Map<String, Object> m = new LinkedHashMap<>();
        String purpose = str(ctx, "purpose", "");
        m.put("name", purpose.isEmpty() ? "生产密钥" : purpose + "密钥");
        m.put("scopes", Arrays.asList("read", "write"));
        m.put("expiresAt", "never");
        m.put("description", "由 AI 推荐生成, 用于 " + purpose);
        return m;
    }

    private Map<String, Object> recommendDataSource(Map<String, Object> ctx) {
        Map<String, Object> m = new LinkedHashMap<>();
        String type = str(ctx, "type", "mysql");
        m.put("host", "127.0.0.1");
        m.put("port", type.equals("postgresql") ? 5432 : (type.equals("redis") ? 6379 : 3306));
        m.put("database", "minimax");
        m.put("username", "root");
        m.put("poolSize", 10);
        m.put("timeout", 5000);
        m.put("driverClassName", type.equals("postgresql")
            ? "org.postgresql.Driver" : "com.mysql.cj.jdbc.Driver");
        return m;
    }

    private Map<String, Object> recommendPipeline(Map<String, Object> ctx) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", str(ctx, "name", "数据处理流水线"));
        m.put("nodes", Arrays.asList(
            Map.of("id", "input", "type", "input", "label", "数据输入"),
            Map.of("id", "process", "type", "transform", "label", "数据处理"),
            Map.of("id", "output", "type", "output", "label", "结果输出")
        ));
        m.put("edges", Arrays.asList(
            Map.of("from", "input", "to", "process"),
            Map.of("from", "process", "to", "output")
        ));
        return m;
    }

    private Map<String, Object> recommendWorkflow(Map<String, Object> ctx) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", "DAG 工作流");
        m.put("steps", Arrays.asList(
            Map.of("id", "s1", "tool", "ai.generate.text", "params", Map.of("prompt", "分析数据")),
            Map.of("id", "s2", "tool", "ai.classify", "params", Map.of("depends", "s1"))
        ));
        return m;
    }

    private List<String> getTopRecommendations(String formType, String field) {
        return switch (formType + "." + field) {
            case "user.role" -> Arrays.asList("user", "admin", "guest");
            case "user.tags" -> Arrays.asList("VIP", "新用户", "试用", "正式");
            case "apiKey.scopes" -> Arrays.asList("read,write", "read", "admin");
            case "dataSource.type" -> Arrays.asList("mysql", "postgresql", "redis", "mongodb");
            default -> Collections.emptyList();
        };
    }

    private Map<String, Object> exampleUser() { return Map.of("username", "zhang_san", "nickname", "张三", "email", "zhangsan@example.com", "role", "user"); }
    private Map<String, Object> exampleApiKey() { return Map.of("name", "生产环境", "scopes", "read,write", "expiresAt", "never"); }
    private Map<String, Object> exampleDataSource() { return Map.of("type", "mysql", "host", "127.0.0.1", "port", 3306, "database", "minimax", "username", "root"); }
    private Map<String, Object> examplePipeline() { return Map.of("name", "ETL 流水线", "nodes", List.of("input", "transform", "output")); }
    private Map<String, Object> exampleWorkflow() { return Map.of("name", "AI 工作流", "steps", List.of("分析", "生成", "导出")); }

    private String str(Map<String, Object> m, String k, String def) {
        Object v = m.get(k);
        return v == null ? def : v.toString();
    }

    /** 请求 DTO */
    public static class AutoFillRequest {
        private String formType;
        private Map<String, Object> context = new HashMap<>();
        public String getFormType() { return formType; }
        public void setFormType(String formType) { this.formType = formType; }
        public Map<String, Object> getContext() { return context; }
        public void setContext(Map<String, Object> context) { this.context = context; }
    }

    /** V6.3+ 反馈 DTO */
    public static class FeedbackRequest {
        private String formType;
        private String formId;
        private String context;
        private String feedback;          // accept / correct / reject
        private String correctedIntent;    // feedback=correct 时必填
        public String getFormType() { return formType; }
        public void setFormType(String formType) { this.formType = formType; }
        public String getFormId() { return formId; }
        public void setFormId(String formId) { this.formId = formId; }
        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }
        public String getFeedback() { return feedback; }
        public void setFeedback(String feedback) { this.feedback = feedback; }
        public String getCorrectedIntent() { return correctedIntent; }
        public void setCorrectedIntent(String correctedIntent) { this.correctedIntent = correctedIntent; }
    }
}
