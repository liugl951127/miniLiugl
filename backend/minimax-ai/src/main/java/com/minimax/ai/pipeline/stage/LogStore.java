package com.minimax.ai.pipeline.stage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.ai.entity.PipelineLog;
import com.minimax.ai.mapper.PipelineLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 阶段 12: 会话日志存储 (V2.8.5)
 *
 * <h3>职责</h3>
 * 把一次完整 pipeline 执行的所有信息持久化到 DB.
 *
 * <h3>性能优化</h3>
 * <ul>
 *   <li>异步写入 (@Async) 不阻塞主流程</li>
 *   <li>失败不抛异常 (best-effort)</li>
 *   <li>统一 JSON 序列化各阶段耗时</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogStore {

    private final PipelineLogMapper logMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 异步存储日志
     */
    @Async("pipelineLogExecutor")
    public void storeAsync(LogPayload payload) {
        try {
            store(payload);
        } catch (Exception e) {
            log.error("[stage-12/log] async store failed", e);
        }
    }

    /**
     * 同步存储
     */
    public void store(LogPayload p) {
        try {
            PipelineLog row = new PipelineLog();
            row.setSessionId(p.sessionId);
            row.setUserId(p.userId);
            row.setClientIp(p.clientIp);
            row.setInputText(truncate(p.inputText, 2000));
            row.setInputModality(p.inputModality);
            row.setIntent(p.intent);
            row.setOutputText(truncate(p.outputText, 4000));
            row.setOutputTokens(p.outputTokens);
            row.setComputeDevice(p.computeDevice);
            row.setComputeMode(p.computeMode);
            row.setTotalCostMs(p.totalCostMs);
            row.setStageCosts(toJson(p.stageCosts));
            row.setRiskLevel(p.riskLevel);
            row.setNeedsReview(p.needsReview);
            row.setRagHits(p.ragHits);
            row.setToolCalls(p.toolCalls);
            row.setErrorMessage(p.errorMessage);
            row.setCreatedAt(LocalDateTime.now());
            logMapper.insert(row);
            log.debug("[stage-12/log] saved pipeline log id={}, session={}", row.getId(), p.sessionId);
        } catch (Exception e) {
            log.error("[stage-12/log] store failed", e);
        }
    }

    private String toJson(Map<String, Long> map) {
        if (map == null || map.isEmpty()) return "{}";
        try { return objectMapper.writeValueAsString(map); }
        catch (JsonProcessingException e) { return "{}"; }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    /** 日志 DTO */
    @lombok.Data
    public static class LogPayload {
        public String sessionId;
        public Long userId;
        public String clientIp;
        public String inputText;
        public String inputModality;
        public String intent;
        public String outputText;
        public Integer outputTokens;
        public String computeDevice;
        public String computeMode;
        public Long totalCostMs;
        public Map<String, Long> stageCosts = new LinkedHashMap<>();
        public String riskLevel;
        public Boolean needsReview;
        public Integer ragHits;
        public Integer toolCalls;
        public String errorMessage;
    }
}
