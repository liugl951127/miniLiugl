package com.minimax.ai.pipeline;

import com.minimax.ai.generation.ConversationContext;
import com.minimax.ai.generation.IntentService;
import com.minimax.ai.pipeline.config.PipelineConfig;
import com.minimax.ai.pipeline.config.PipelineConfig.RiskLevel;
import com.minimax.ai.pipeline.config.PipelineConfig.Stage;
import com.minimax.ai.pipeline.stage.*;
import com.minimax.ai.pipeline.stage.ContextAssembler.AssembledContext;
import com.minimax.ai.pipeline.stage.FormatProcessor.FormatResult;
import com.minimax.ai.pipeline.stage.GatewayDispatcher.DispatchResult;
import com.minimax.ai.pipeline.stage.GatewayDispatcher.RawInput;
import com.minimax.ai.pipeline.stage.ModelInference.InferenceResult;
import com.minimax.ai.pipeline.stage.MultimodalParser.ParseResult;
import com.minimax.ai.pipeline.stage.RagToolAgentEnhancer.EnhancementResult;
import com.minimax.ai.pipeline.stage.RiskControl.RiskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pipeline 主执行器 (V2.8.5)
 *
 * <h3>13 阶段编排</h3>
 * <pre>
 *   1. USER_INPUT         (用户输入)
 *   2. GATEWAY_DISPATCH   (网关分发)
 *   3. MULTIMODAL_PARSE   (ASR/OCR)
 *   4. CONTEXT_ASSEMBLE   (上下文+系统提示)
 *   5. PRE_RISK           (前置风控)
 *   6. RAG_TOOL_AGENT     (RAG/工具/智能体)
 *   7. TOKENIZE           (分词转 Token)
 *   8. MODEL_GENERATE     (模型自回归生成)
 *   9. TOKEN_DECODE       (Token 解码)
 *   10. POST_RISK         (后置风控)
 *   11. FORMAT            (格式化整理)
 *   12. LOG_STORE         (存储会话日志)
 *   13. RETURN            (返回前端)
 * </pre>
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li>每阶段独立可测, 失败降级 (fallback)</li>
 *   <li>耗时/状态全部记录到 stageCosts map</li>
 *   <li>风控可阻断 (前置) 或标记 (后置)</li>
 *   <li>动态数据: 不造假, 真实跑各阶段</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PipelineExecutor {

    private final GatewayDispatcher gatewayDispatcher;
    private final MultimodalParser multimodalParser;
    private final ContextAssembler contextAssembler;
    private final RiskControl riskControl;
    private final RagToolAgentEnhancer ragEnhancer;
    private final Tokenizer tokenizer;
    private final ModelInference modelInference;
    private final FormatProcessor formatProcessor;
    private final LogStore logStore;
    private final ConversationContext conversationContext;
    private final IntentService intentService;

    /**
     * 执行完整 pipeline
     *
     * @param request 用户请求
     * @return pipeline 执行结果
     */
    public PipelineResult execute(PipelineRequest request) {
        long totalStart = System.currentTimeMillis();
        log.info("========== [pipeline] START session={} ==========", request.sessionId);

        PipelineResult result = new PipelineResult();
        result.sessionId = request.sessionId;
        result.stageCosts = new LinkedHashMap<>();
        result.stageStatuses = new LinkedHashMap<>();
        Map<String, Long> stageCosts = result.stageCosts;

        try {
            // ========== 阶段 1: USER_INPUT ==========
            stageStart(Stage.USER_INPUT, stageCosts);
            result.rawInput = request.toRawInput();
            stageEnd(Stage.USER_INPUT, stageCosts, result.stageStatuses, "OK");

            // ========== 阶段 2: GATEWAY_DISPATCH ==========
            DispatchResult dispatch = runStage(Stage.GATEWAY_DISPATCH,
                    () -> gatewayDispatcher.dispatch(result.rawInput),
                    stageCosts, result.stageStatuses, null);
            if (dispatch == null) return failResult(result, "GATEWAY_DISPATCH failed", totalStart);

            // ========== 阶段 3: MULTIMODAL_PARSE ==========
            ParseResult parsed = runStage(Stage.MULTIMODAL_PARSE,
                    () -> multimodalParser.parse(dispatch),
                    stageCosts, result.stageStatuses, ParseResult::new);
            result.parsed = parsed;

            // ========== 阶段 4: CONTEXT_ASSEMBLE ==========
            AssembledContext assembled = runStage(Stage.CONTEXT_ASSEMBLE,
                    () -> contextAssembler.assemble(parsed, request.sessionId),
                    stageCosts, result.stageStatuses, AssembledContext::new);
            result.context = assembled;

            // ========== 阶段 5: PRE_RISK ==========
            RiskResult preRisk = runStage(Stage.PRE_RISK,
                    () -> riskControl.preCheck(parsed.unifiedText),
                    stageCosts, result.stageStatuses, RiskResult::new);
            if (preRisk.blocked) {
                log.warn("[pipeline] blocked by pre-risk, level={}", preRisk.level);
                result.blocked = true;
                result.blockReason = "前置风控拦截: 命中敏感词 " + preRisk.hits;
                result.finalText = "很抱歉, 您的输入包含不当内容, 我无法处理. 如有疑问请联系人工客服.";
                result.riskLevel = preRisk.level;
                // 跳过后续, 走日志 + 返回
                storeLog(result, totalStart, preRisk.level, null, null);
                return result;
            }
            result.preRisk = preRisk;

            // ========== 阶段 6: RAG_TOOL_AGENT ==========
            EnhancementResult enhanced = runStage(Stage.RAG_TOOL_AGENT,
                    () -> ragEnhancer.enhance(assembled, parsed.unifiedText, request.sessionId),
                    stageCosts, result.stageStatuses, EnhancementResult::new);
            result.enhanced = enhanced;

            // 阶段 4 完成后, prompt 包含 retrieval/tool 段, 重新计算
            assembled.fullPrompt = rebuildPrompt(assembled.segments);

            // ========== 阶段 7: TOKENIZE ==========
            int[] tokens = runStage(Stage.TOKENIZE,
                    () -> tokenizer.encode(assembled.fullPrompt),
                    stageCosts, result.stageStatuses, () -> new int[0]);
            if (tokens == null) return failResult(result, "TOKENIZE failed", totalStart);
            result.inputTokens = tokens.length;

            // ========== 阶段 8: MODEL_GENERATE ==========
            InferenceResult inference = runStage(Stage.MODEL_GENERATE,
                    () -> modelInference.generate(tokens, PipelineConfig.MAX_GENERATE_TOKENS),
                    stageCosts, result.stageStatuses, InferenceResult::new);
            if (inference == null) return failResult(result, "MODEL_GENERATE failed", totalStart);
            result.inference = inference;
            result.computeDevice = inference.device;
            result.computeMode = inference.computeMode;

            // 记录到对话上下文
            if (request.sessionId != null && enhanced != null) {
                conversationContext.record(request.sessionId, parsed.unifiedText,
                        enhanced.intent, null);
                conversationContext.record(request.sessionId, inference.outputText,
                        enhanced.intent, null);
            }

            // ========== 阶段 9: TOKEN_DECODE (内嵌在 ModelInference 中) ==========
            stageStart(Stage.TOKEN_DECODE, stageCosts);
            // 已经完成 (ModelInference 已 decode)
            result.decodedText = inference.outputText;
            stageEnd(Stage.TOKEN_DECODE, stageCosts, result.stageStatuses, "OK");

            // ========== 阶段 10: POST_RISK ==========
            RiskResult postRisk = runStage(Stage.POST_RISK,
                    () -> riskControl.postCheck(inference.outputText),
                    stageCosts, result.stageStatuses, RiskResult::new);
            result.postRisk = postRisk;

            // ========== 阶段 11: FORMAT ==========
            FormatResult formatted = runStage(Stage.FORMAT,
                    () -> formatProcessor.format(inference),
                    stageCosts, result.stageStatuses, FormatResult::new);
            result.formatted = formatted;
            result.finalText = formatted.formattedText;

            // ========== 阶段 12: LOG_STORE ==========
            storeLog(result, totalStart, postRisk.level, enhanced, postRisk);

            // ========== 阶段 13: RETURN ==========
            stageStart(Stage.RETURN, stageCosts);
            result.totalCostMs = System.currentTimeMillis() - totalStart;
            result.success = true;
            stageEnd(Stage.RETURN, stageCosts, result.stageStatuses, "OK");
            log.info("========== [pipeline] END session={}, total={}ms, success={} ==========",
                    request.sessionId, result.totalCostMs, result.success);
            return result;
        } catch (Exception e) {
            log.error("[pipeline] unexpected error", e);
            result.errorMessage = e.getMessage();
            result.totalCostMs = System.currentTimeMillis() - totalStart;
            return result;
        }
    }

    /**
     * 通用阶段执行包装
     */
    private <T> T runStage(Stage stage, java.util.function.Supplier<T> supplier,
                            Map<String, Long> stageCosts,
                            Map<String, String> stageStatuses,
                            java.util.function.Supplier<T> fallback) {
        long start = System.currentTimeMillis();
        try {
            T out = supplier.get();
            long cost = System.currentTimeMillis() - start;
            stageCosts.put(stage.name(), cost);
            stageStatuses.put(stage.name(), "OK");
            log.info("[pipeline] stage {} OK, cost={}ms", stage.name(), cost);
            return out;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            stageCosts.put(stage.name(), cost);
            stageStatuses.put(stage.name(), "ERROR: " + e.getMessage());
            log.error("[pipeline] stage {} FAILED, cost={}ms", stage.name(), cost, e);
            return fallback != null ? fallback.get() : null;
        }
    }

    /** 阶段开始时间记录 */
    private void stageStart(Stage s, Map<String, Long> map) {
        map.put(s.name() + "::start", System.currentTimeMillis());
    }

    /** 阶段结束 + 耗时计算 */
    private void stageEnd(Stage s, Map<String, Long> map, Map<String, String> statuses, String status) {
        Long start = map.remove(s.name() + "::start");
        if (start != null) {
            map.put(s.name(), System.currentTimeMillis() - start);
        }
        statuses.put(s.name(), status);
    }

    /** 重建 prompt (阶段 6 注入 retrieval/tool 后) */
    private String rebuildPrompt(Map<String, String> segments) {
        StringBuilder sb = new StringBuilder();
        String[] order = {"system", "history", "retrieval", "tool", "user"};
        for (String key : order) {
            String v = segments.get(key);
            if (v == null || v.isEmpty()) continue;
            sb.append("<").append(key).append(">\n").append(v);
            if (!v.endsWith("\n")) sb.append("\n");
            sb.append("</").append(key).append(">\n\n");
        }
        return sb.toString();
    }

    /** 失败结果 */
    private PipelineResult failResult(PipelineResult r, String reason, long totalStart) {
        r.success = false;
        r.errorMessage = reason;
        r.finalText = "抱歉, 处理失败: " + reason;
        r.totalCostMs = System.currentTimeMillis() - totalStart;
        return r;
    }

    /** 持久化日志 */
    private void storeLog(PipelineResult r, long totalStart, RiskLevel level,
                          EnhancementResult enhanced, RiskResult postRisk) {
        try {
            LogStore.LogPayload p = new LogStore.LogPayload();
            p.sessionId = r.sessionId;
            p.inputText = r.rawInput != null ? r.rawInput.text : null;
            p.inputModality = r.parsed != null ? r.parsed.detectedModality.name() : null;
            p.intent = enhanced != null ? enhanced.intent.name() : null;
            p.outputText = r.finalText;
            p.outputTokens = r.inference != null ? r.inference.newTokens : 0;
            p.computeDevice = r.computeDevice;
            p.computeMode = r.computeMode;
            p.totalCostMs = System.currentTimeMillis() - totalStart;
            p.stageCosts = r.stageCosts;
            p.riskLevel = level != null ? level.name() : null;
            p.needsReview = postRisk != null && postRisk.needsReview;
            p.ragHits = enhanced != null ? enhanced.ragResults.size() : 0;
            p.toolCalls = enhanced != null ? enhanced.toolResults.size() : 0;
            p.errorMessage = r.errorMessage;
            logStore.store(p);
        } catch (Exception e) {
            log.warn("[pipeline] log store failed", e);
        }
    }

    // ========================================================
    // DTO
    // ========================================================

    /** 用户请求 */
    @lombok.Data
    public static class PipelineRequest {
        public String sessionId;
        public Long userId;
        public String text;
        public PipelineConfig.InputModality modality;
        public String fileData;
        public String fileName;
        public String clientIp;
        public Map<String, Object> preferences;

        public RawInput toRawInput() {
            RawInput r = new RawInput();
            r.text = this.text;
            r.modality = this.modality;
            r.fileData = this.fileData;
            r.fileName = this.fileName;
            r.clientIp = this.clientIp;
            r.preferences = this.preferences;
            return r;
        }
    }

    /** Pipeline 结果 */
    @lombok.Data
    public static class PipelineResult {
        public String sessionId;
        public boolean success;
        public boolean blocked;
        public String blockReason;
        public String finalText;
        public String errorMessage;
        public long totalCostMs;
        public String computeDevice;
        public String computeMode;
        public int inputTokens;
        public RiskLevel riskLevel;

        // 各阶段输出
        public RawInput rawInput;
        public DispatchResult dispatch;
        public ParseResult parsed;
        public AssembledContext context;
        public RiskResult preRisk;
        public EnhancementResult enhanced;
        public InferenceResult inference;
        public String decodedText;
        public RiskResult postRisk;
        public FormatResult formatted;

        // 元数据
        public Map<String, Long> stageCosts;
        public Map<String, String> stageStatuses;
    }
}
