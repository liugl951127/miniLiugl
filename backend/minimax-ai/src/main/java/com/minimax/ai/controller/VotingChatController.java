package com.minimax.ai.controller;

import com.minimax.ai.intent.MultiModelVotingService;
import com.minimax.ai.intent.MultiModelVotingService.VotingResult;
import com.minimax.ai.intent.MultiModelVotingService.VotingStrategy;
import com.minimax.common.result.Result;
import com.minimax.model.dto.ChatRequest;
import com.minimax.model.service.ModelService;
import com.minimax.model.vo.ChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V3.5.3+ AI 对话路由控制器 (Day 31).
 *
 * <p>在普通模型调用之上封装多模型投票层：
 * <ul>
 *   <li>高置信度 → 直接返回单模型答案</li>
 *   <li>低置信度 (confidence &lt; threshold) → 触发 {@link MultiModelVotingService} 多模型并行推理</li>
 *   <li>投票结果写入响应元数据（策略/耗时/一致率/各模型答案）</li>
 * </ul>
 *
 * <p>同时在 {@link MultiModelVotingService} 中调用本服务 /api/v1/models/chat 做多模型 HTTP 调用，
 * 因此本 Controller 的职责是：先调单模型预判，再决定是否投票。
 *
 * <p>端点:
 * <ul>
 *   <li>POST /api/v1/ai/chat          — 高置信走单模型，低置信自动触发投票</li>
 *   <li>POST /api/v1/ai/chat/voting  — 强制触发多模型投票</li>
 *   <li>GET  /api/v1/ai/chat/voting-info — 查询投票配置</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/chat")
@RequiredArgsConstructor
@Tag(name = "AI 投票对话", description = "多模型投票路由 — 高置信走单模型，低置信自动触发多模型投票")
public class VotingChatController {

    private final MultiModelVotingService votingService;
    private final RestTemplate restTemplate = new RestTemplate();

    // minimax-model 的 chat 端点（同机器内部调用）
    private static final String MODEL_CHAT_URL = "http://localhost:8083/api/v1/models/chat";

    // ============== 核心端点 ==============

    /**
     * 智能对话：先单模型预览置信度，低则自动投票。
     *
     * <p>流程:
     * <pre>
     * 1. 调用 votingService.shouldVote(text, sessionId) 预判
     * 2. 高置信 → 直接调单模型
     * 3. 低置信 → 调 votingService.vote(text, sessionId) 触发多模型
     * 4. 合并结果元数据返回
     * </pre>
     */
    @Operation(summary = "智能对话（含自动投票）",
               description = "先单模型预览置信度，低于阈值自动触发多模型投票。返回 ChatResponse + votingMeta")
    @PostMapping
    public Result<Map<String, Object>> chat(@RequestBody VotingChatRequest request) {
        String text = request.getText();
        String sessionId = request.getSessionId();
        String model = request.getModel() != null ? request.getModel() : "MiniMax-Text-01";

        if (text == null || text.isBlank()) {
            return Result.fail(400, "text 不能为空");
        }

        long start = System.currentTimeMillis();

        // 预判：是否需要投票
        boolean needVote = votingService.shouldVote(text, sessionId);

        ChatResponse singleResp;
        VotingResult votingResult = null;

        if (needVote) {
            // 低置信：触发多模型投票
            log.info("[VotingChat] 低置信触发投票 text='{}'", text.length() > 40 ? text.substring(0, 40) : text);
            votingResult = votingService.vote(text, sessionId);
            singleResp = new ChatResponse(
                    votingResult.getConsensus(),
                    null, null, null,
                    "multi-model-voting",
                    votingResult.getElapsedMs() > 0 ? (int) votingResult.getElapsedMs() : null
            );
        } else {
            // 高置信：直接调单模型
            singleResp = callSingleModel(text, model);
        }

        long elapsed = System.currentTimeMillis() - start;

        // 打包响应（含投票元数据）
        Map<String, Object> meta = new HashMap<>();
        meta.put("confidence", needVote
                ? votingResult != null ? votingResult.getAgreementScore() : 0.0
                : 1.0);
        meta.put("votingTriggered", needVote);
        meta.put("totalElapsedMs", elapsed);

        if (needVote && votingResult != null) {
            meta.put("votingStrategy", votingResult.getStrategy().name());
            meta.put("votingElapsedMs", votingResult.getElapsedMs());
            meta.put("agreementScore", votingResult.getAgreementScore());
            meta.put("modelCount", votingResult.getAnswers().size());
            meta.put("votingMessage", votingResult.getMessage());
            // 各模型答案（用于前端展示）
            meta.put("modelAnswers", votingResult.getAnswers().stream()
                    .map(a -> Map.of(
                            "model", a.getModelName(),
                            "provider", a.getProvider(),
                            "answer", a.getAnswer() != null ? a.getAnswer() : "",
                            "error", a.getError() != null ? a.getError() : "",
                            "latencyMs", a.getLatencyMs()
                    ))
                    .toList());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("response", singleResp);
        body.put("meta", meta);
        return Result.ok(body);
    }

    /**
     * 强制多模型投票（绕过置信度预判）。
     */
    @Operation(summary = "强制多模型投票",
               description = "忽略置信度预判，强制触发多模型投票并返回各模型答案")
    @PostMapping("/voting")
    public Result<Map<String, Object>> forceVoting(@RequestBody VotingChatRequest request) {
        String text = request.getText();
        String sessionId = request.getSessionId();

        if (text == null || text.isBlank()) {
            return Result.fail(400, "text 不能为空");
        }

        VotingResult result = votingService.vote(text, sessionId);

        ChatResponse resp = new ChatResponse(
                result.getConsensus(),
                null, null, null,
                "forced-voting:" + result.getStrategy().name(),
                (int) result.getElapsedMs()
        );

        Map<String, Object> body = new HashMap<>();
        body.put("response", resp);
        body.put("voting", Map.of(
                "strategy", result.getStrategy().name(),
                "elapsedMs", result.getElapsedMs(),
                "agreementScore", result.getAgreementScore(),
                "message", result.getMessage(),
                "modelAnswers", result.getAnswers().stream()
                        .map(a -> Map.of(
                                "model", a.getModelName(),
                                "provider", a.getProvider(),
                                "answer", a.getAnswer() != null ? a.getAnswer() : "",
                                "error", a.getError() != null ? a.getError() : "",
                                "latencyMs", a.getLatencyMs()
                        ))
                        .toList()
        ));
        return Result.ok(body);
    }

    /**
     * 查询投票配置信息。
     */
    @Operation(summary = "查询投票配置")
    @GetMapping("/voting-info")
    public Result<Map<String, Object>> votingInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("votingEnabled", votingService.shouldVote("test", null));  // just checks flag
        info.put("threshold", 0.50);  // from @Value defaults
        info.put("strategy", VotingStrategy.CONFIDENCE_WEIGHTED.name());
        info.put("modelCount", 3);
        return Result.ok(info);
    }

    // ============== 内部方法 ==============

    /** 调用单模型（非投票路径） */
    private ChatResponse callSingleModel(String text, String model) {
        try {
            Map<String, Object> reqBody = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", text)),
                    "max_tokens", 800,
                    "temperature", 0.7
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(
                    MODEL_CHAT_URL, reqBody, Map.class);

            if (resp == null) return fallback(text);

            // 统一响应格式提取
            Object data = resp.get("data");
            if (data instanceof Map) {
                Map<String, Object> d = (Map<String, Object>) data;
                return new ChatResponse(
                        (String) d.get("content"),
                        toInt(d.get("prompt_tokens")),
                        toInt(d.get("completion_tokens")),
                        toInt(d.get("total_tokens")),
                        model,
                        null
                );
            }
            return fallback(text);

        } catch (Exception e) {
            log.warn("[VotingChat] single model call failed: {}", e.getMessage());
            return fallback(text);
        }
    }

    private ChatResponse fallback(String text) {
        return new ChatResponse(
                "(模型服务不可用) 你说的是：" + text,
                null, null, null,
                "fallback",
                null
        );
    }

    private Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return null; }
    }

    // ============== 请求 DTO ==============

    public static class VotingChatRequest {
        private String text;
        private String sessionId;
        private String model;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }
}
