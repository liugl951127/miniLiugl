package com.bank.dualrecord.quality.llm;

import com.bank.dualrecord.quality.prompt.QualityCheckPrompt;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * AI 智能质检服务
 *
 * <p>基于 LLM 对双录结果做综合评分
 *
 * @author Mavis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQualityService {

    private final LlmProviderFactory llmFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行 AI 质检
     */
    public QualityCheckResult check(QualityCheckRequest request) {
        log.info("开始 AI 质检: orderId={}, sessionId={}", request.getOrderId(), request.getSessionId());

        // 1. 构造 Prompt
        String systemPrompt = QualityCheckPrompt.SYSTEM;
        String userPrompt = QualityCheckPrompt.build(
            request.getOrderId(),
            request.getProductName(),
            request.getScriptText(),
            request.getAsrText(),
            request.getNodeResultsJson()
        );

        // 2. 调用 LLM
        LlmProvider provider = llmFactory.getProvider();
        log.info("使用 LLM Provider: {}", provider.getId());
        String rawResponse = provider.complete(systemPrompt, userPrompt);

        // 3. 解析 JSON
        QualityCheckResult result = parseResult(rawResponse);
        result.setLlmProvider(provider.getId());
        result.setRawResponse(rawResponse);

        log.info("AI 质检完成: orderId={}, verdict={}, score={}",
            request.getOrderId(), result.getVerdict(), result.getTotalScore());

        return result;
    }

    /**
     * 解析 LLM 返回的 JSON
     */
    private QualityCheckResult parseResult(String raw) {
        try {
            // 提取 JSON 部分(LLM 可能带其他文字)
            int jsonStart = raw.indexOf('{');
            int jsonEnd = raw.lastIndexOf('}');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String json = raw.substring(jsonStart, jsonEnd + 1);
                return objectMapper.readValue(json, QualityCheckResult.class);
            }
            return objectMapper.readValue(raw, QualityCheckResult.class);
        } catch (Exception e) {
            log.error("解析 LLM 响应失败: {}", raw, e);
            QualityCheckResult fallback = new QualityCheckResult();
            fallback.setVerdict("REVIEW");
            fallback.setTotalScore(60);
            fallback.setAiComment("LLM 响应解析失败,需人工复检: " + e.getMessage());
            return fallback;
        }
    }

    @Data
    public static class QualityCheckRequest {
        private String orderId;
        private String sessionId;
        private String productName;
        private String scriptText;
        private String asrText;
        private String nodeResultsJson;
    }

    @Data
    public static class QualityCheckResult {
        private String verdict;            // HIGH_PASS / PASS / REVIEW / FAIL
        private double totalScore;
        private double scriptScore;
        private double riskScore;
        private double confirmScore;
        private double avScore;
        private double flowScore;
        private List<String> missingNodes;
        private List<String> riskItems;
        private List<String> positiveItems;
        private String aiComment;
        private double confidence;
        private String llmProvider;
        private String rawResponse;
    }
}
