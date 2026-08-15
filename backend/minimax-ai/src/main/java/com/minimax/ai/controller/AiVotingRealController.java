package com.minimax.ai.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.ai.entity.AiVotingRecord;
import com.minimax.ai.service.AiVotingService;
import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * AI Voting 真实业务控制器 (V6.8.10 Day 39)
 * 所有端点数据来自 DB，不再使用 mock
 *
 * @author Mavis
 * @since V6.6
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/voting")
@RequiredArgsConstructor
public class AiVotingRealController {

    private final AiVotingService votingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 触发投票 (保存投票记录)
     * Day 43: 支持 notifyEmail 投票结束邮件通知
     */
    @PostMapping
    public Result<Map<String, Object>> vote(@RequestBody Map<String, Object> body) {
        String text = (String) body.getOrDefault("text", "");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> votes = (List<Map<String, Object>>) body.getOrDefault("votes", List.of());
        String finalAnswer = (String) body.getOrDefault("final", "A");
        String strategy = (String) body.getOrDefault("strategy", "majority");
        String notifyEmail = (String) body.get("notifyEmail"); // Day 43

        // 计算一致率
        double agreement = calcAgreement(votes, finalAnswer);

        // 计算平均置信度
        double avgConf = votes.stream()
            .filter(v -> v.get("confidence") != null)
            .mapToDouble(v -> ((Number) v.get("confidence")).doubleValue())
            .average().orElse(0.0);

        // 保存记录
        AiVotingRecord record = new AiVotingRecord();
        record.setSessionId(UUID.randomUUID().toString().substring(0, 8));
        record.setUserId(1L);
        record.setUsername("system");
        record.setQuestion(text);
        record.setFinalAnswer(finalAnswer);
        record.setStrategy(strategy);
        record.setTotalVotes(votes.size());
        record.setAgreementRate(BigDecimal.valueOf(agreement));
        record.setModelVotes(toJson(votes));
        record.setDurationMs(0);
        record.setNotifyEmail(notifyEmail); // Day 43: 邮件通知
        votingService.saveVotingRecord(record);

        log.info("[Voting] text={}, votes={}, final={}, notifyEmail={}", text, votes.size(), finalAnswer, notifyEmail);
        return Result.ok(Map.of(
            "text", text,
            "votes", votes,
            "final", finalAnswer,
            "strategy", strategy,
            "agreement", agreement,
            "notifyEmail", notifyEmail != null ? notifyEmail : ""
        ));
    }

    /**
     * 投票历史
     */
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> history(@RequestParam(defaultValue = "20") int limit) {
        Map<String, Object> recordsData = votingService.getRecords(1, limit);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) recordsData.getOrDefault("records", List.of());
        return Result.ok(records);
    }

    /**
     * 投票统计汇总 (真实 DB)
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(votingService.getStats());
    }

    /**
     * 投票趋势 (真实 DB, 近7天)
     */
    @GetMapping("/stats/trend")
    public Result<List<Map<String, Object>>> trend() {
        return Result.ok(votingService.getTrend());
    }

    /**
     * 投票记录分页 (真实 DB)
     */
    @GetMapping("/records")
    public Result<Map<String, Object>> records(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(votingService.getRecords(page, size));
    }

    /**
     * 投票配置
     */
    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        return Result.ok(Map.of(
            "enabled", true,
            "threshold", 0.7,
            "models", List.of("gpt-4", "claude-3", "deepseek", "qwen"),
            "strategy", "majority"
        ));
    }

    /**
     * 重新投票 (Day 41)
     * 根据历史记录复制参数，供前端重新发起投票
     */
    @GetMapping("/duplicate/{recordId}")
    public Result<Map<String, Object>> duplicateVote(@PathVariable Long recordId) {
        return Result.ok(votingService.duplicateVote(recordId));
    }

    // ----- 工具方法 -----

    private double calcAgreement(List<Map<String, Object>> votes, String finalAnswer) {
        if (votes == null || votes.isEmpty()) return 0.0;
        long match = votes.stream()
            .filter(v -> finalAnswer.equals(String.valueOf(v.getOrDefault("answer", ""))))
            .count();
        return Math.round(match * 10000.0 / votes.size()) / 10000.0;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}
