package com.minimax.ai.intent;

import com.minimax.ai.generation.KeywordEngine;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 多模型投票服务 (Day 30).
 *
 * <p>当 IntentConfidenceScorer 返回的 confidence &lt; LOW_CONF_THRESHOLD (0.50) 时，
 * 自动触发多模型并行推理，最终通过投票/共识机制返回最可靠的答案。
 *
 * <h3>投票策略</h3>
 * <ul>
 *   <li><b>MAJORITY</b>: 多模型中相同答案超过半数 → 直接返回</li>
 *   <li><b>CONFIDENCE_WEIGHTED</b>: 按各模型能力分加权，累加得分最高者</li>
 *   <li><b>LLM_JUDGE</b>: 各模型答案交给 MiniMax-Text-03 做最终裁决（最可靠但最贵）</li>
 * </ul>
 *
 * <h3>并行策略</h3>
 * <ul>
 *   <li>多模型同时发起 HTTP 调用 (CompletableFuture.allOf)</li>
 *   <li>单模型超时: 10s (可配置)</li>
 *   <li>总超时: 30s (可配置)</li>
 *   <li>允许部分模型失败 (至少 N 个成功才投票)</li>
 * </ul>
 *
 * <p>配置项:
 * <pre>
 * minimax.ai.voting.enabled=true
 * minimax.ai.voting.threshold=0.50
 * minimax.ai.voting.model-count=3
 * minimax.ai.voting.strategy=CONFIDENCE_WEIGHTED
 * minimax.ai.voting.timeout-ms=10000
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MultiModelVotingService {

    private final IntentConfidenceScorer confidenceScorer;
    private final SmartModelRouter modelRouter;
    private final KeywordEngine keywordEngine;

    /** model-service 的 base URL (同模块调用) */
    @Value("${minimax.ai.model-service-url:http://localhost:8082}")
    private String modelServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /** 投票线程池 (复用避免频繁创建) */
    private final ExecutorService votingPool = Executors.newFixedThreadPool(4);

    // ============== 配置 ==============

    @Value("${minimax.ai.voting.enabled:true}")
    private boolean votingEnabled;

    @Value("${minimax.ai.voting.threshold:0.50}")
    private double threshold;

    @Value("${minimax.ai.voting.model-count:3}")
    private int modelCount;

    @Value("${minimax.ai.voting.strategy:CONFIDENCE_WEIGHTED}")
    private VotingStrategy strategy;

    @Value("${minimax.ai.voting.timeout-ms:10000}")
    private long modelTimeoutMs;

    // ============== 公开 API ==============

    /**
     * 判断是否需要触发多模型投票。
     *
     * @param text      用户输入
     * @param sessionId 会话 ID
     * @return 需要投票 → true; 单模型足够 → false
     */
    public boolean shouldVote(String text, String sessionId) {
        if (!votingEnabled) return false;
        IntentConfidenceScorer.IntentWithConfidence iwc = confidenceScorer.recognize(text, sessionId);
        return iwc.getConfidence() < threshold;
    }

    /**
     * 执行多模型投票。
     *
     * @param text      用户问题（需要多模型回答的核心问题）
     * @param sessionId 会话 ID
     * @return 投票结果
     */
    public VotingResult vote(String text, String sessionId) {
        if (!votingEnabled) {
            return new VotingResult(List.of(), null, VotingStrategy.NONE, 0,
                    "voting disabled", 0);
        }

        long start = System.currentTimeMillis();
        IntentConfidenceScorer.IntentWithConfidence iwc = confidenceScorer.recognize(text, sessionId);

        // 选 N 个不同模型并行回答
        List<ModelAnswer> answers = invokeMultipleModels(text, modelCount);

        long elapsed = System.currentTimeMillis() - start;

        // 过滤掉失败的回答
        List<ModelAnswer> successAnswers = answers.stream()
                .filter(a -> a.answer != null && !a.answer.isBlank())
                .toList();

        if (successAnswers.size() < 2) {
            // 成功的模型太少，fallback 到置信度最高的单模型
            log.warn("[Voting] too few successful answers ({}), fallback to single model", successAnswers.size());
            return new VotingResult(successAnswers, pickBestSingle(successAnswers),
                    VotingStrategy.NONE, elapsed,
                    "fallback: insufficient models", iwc.getConfidence());
        }

        // 投票
        String consensus = resolveConsensus(successAnswers);
        double agreementScore = calcAgreementScore(successAnswers, consensus);

        log.info("[Voting] '{}' → {} answers in {}ms, strategy={}, consensus='{}...' (agreement={:.2f})",
                text.length() > 40 ? text.substring(0, 40) : text,
                successAnswers.size(), elapsed, strategy,
                consensus.length() > 60 ? consensus.substring(0, 60) : consensus,
                agreementScore);

        return new VotingResult(successAnswers, consensus, strategy, elapsed,
                "ok", agreementScore);
    }

    /**
     * 快捷方法：先判断是否需要投票，如果需要则执行。
     *
     * @param text      用户输入
     * @param sessionId 会话 ID
     * @param singleModelAnswer 单模型已给出的答案（用于 fallback）
     * @return 投票结果或单模型答案
     */
    public VotingResult voteIfNeeded(String text, String sessionId, String singleModelAnswer) {
        if (!shouldVote(text, sessionId)) {
            return new VotingResult(List.of(), singleModelAnswer, VotingStrategy.NONE, 0,
                    "high-confidence, no vote", 1.0);
        }
        return vote(text, sessionId);
    }

    // ============== 核心：并行多模型调用 ==============

    /**
     * 并行调用 N 个不同模型回答同一问题。
     */
    private List<ModelAnswer> invokeMultipleModels(String text, int count) {
        List<SmartModelRouter.Model> models = selectVotingModels(count);

        List<CompletableFuture<ModelAnswer>> futures = models.stream()
                .map(model -> CompletableFuture.supplyAsync(
                        () -> callModel(text, model), votingPool))
                .toList();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(
                    modelTimeoutMs * count + 5000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("[Voting] parallel invocation interrupted: {}", e.getMessage());
        }

        return futures.stream()
                .map(f -> {
                    try { return f.getNow(new ModelAnswer(null, null, null, "timeout", 0)); }
                    catch (Exception e) { return new ModelAnswer(null, null, null, e.getMessage(), 0); }
                })
                .toList();
    }

    /**
     * 从模型池中选 N 个不同类型的模型。
     */
    private List<SmartModelRouter.Model> selectVotingModels(int count) {
        List<SmartModelRouter.Model> pool = new ArrayList<>();
        // 自研选 2 个
        pool.add(SmartModelRouter.Model.MINIMAX_TEXT_01);
        pool.add(SmartModelRouter.Model.MINIMAX_TEXT_02);
        // 外部有 key 才加
        if (modelRouter.hasOpenAiKey()) {
            pool.add(SmartModelRouter.Model.GPT4O_MINI);
        }
        if (modelRouter.hasDeepSeekKey()) {
            pool.add(SmartModelRouter.Model.DEEPSEEK_CHAT);
        }
        // 始终加 Text-03（复杂任务）
        if (count >= 4) pool.add(SmartModelRouter.Model.MINIMAX_TEXT_03);

        return pool.subList(0, Math.min(count, pool.size()));
    }

    /**
     * 调用单个模型获取回答（通过 HTTP 到 model-service）。
     */
    private ModelAnswer callModel(String text, SmartModelRouter.Model model) {
        long t0 = System.currentTimeMillis();
        try {
            String prompt = buildVotingPrompt(text, model);
            // 调用本地 model-service
            Map<String, Object> body = Map.of(
                    "model", model.getName(),
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "max_tokens", 500,
                    "temperature", 0.3
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(
                    modelServiceUrl + "/models/chat",
                    body, Map.class);

            long elapsed = System.currentTimeMillis() - t0;
            String answer = extractAnswer(resp);
            double score = model.getCapabilityScore() * 1.0; // 能力分作为权重基础

            return new ModelAnswer(model.getName(), model.getProvider(), answer,
                    null, elapsed, score);

        } catch (Exception e) {
            log.warn("[Voting] model {} failed: {}", model.getName(), e.getMessage());
            return new ModelAnswer(model.getName(), model.getProvider(), null,
                    e.getMessage(), System.currentTimeMillis() - t0, 0);
        }
    }

    /** 从 model-service 响应提取文本 */
    @SuppressWarnings("unchecked")
    private String extractAnswer(Map<String, Object> resp) {
        if (resp == null) return null;
        Object content = resp.get("content");
        if (content instanceof String) return (String) content;
        if (content instanceof Map) return (String) ((Map<String, Object>) content).get("text");
        return String.valueOf(content);
    }

    /** 构造投票专用 prompt（让模型给出简洁答案，方便比较） */
    private String buildVotingPrompt(String text, SmartModelRouter.Model model) {
        return String.format("""
                请简洁回答以下问题（50字以内，直接给出答案，不要解释）：

                问题：%s

                答案：
                """, text);
    }

    // ============== 投票共识算法 ==============

    /**
     * 根据投票策略解析共识答案。
     */
    private String resolveConsensus(List<ModelAnswer> answers) {
        return switch (strategy) {
            case MAJORITY -> majorityVote(answers);
            case CONFIDENCE_WEIGHTED -> weightedVote(answers);
            case LLM_JUDGE -> llmJudge(answers);
            case NONE -> answers.isEmpty() ? "" : answers.get(0).answer;
        };
    }

    /** 多数投票：相同答案出现次数最多的 */
    private String majorityVote(List<ModelAnswer> answers) {
        Map<String, Long> count = answers.stream()
                .filter(a -> a.answer != null)
                .collect(Collectors.groupingBy(
                        a -> normalizeForVote(a.answer), Collectors.counting()));
        return count.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(answers.get(0).answer);
    }

    /** 加权投票：各模型能力分作为权重 */
    private String weightedVote(List<ModelAnswer> answers) {
        Map<String, Double> scoreMap = new HashMap<>();
        for (ModelAnswer a : answers) {
            if (a.answer == null) continue;
            String key = normalizeForVote(a.answer);
            scoreMap.merge(key, a.modelScore, Double::sum);
        }
        return scoreMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(answers.get(0).answer);
    }

    /**
     * LLM 裁决：用 MiniMax-Text-03 综合各模型答案，选最优。
     * 成本最高但最可靠。
     */
    private String llmJudge(List<ModelAnswer> answers) {
        StringBuilder sb = new StringBuilder("以下是多个模型对同一问题的回答：\n\n");
        for (int i = 0; i < answers.size(); i++) {
            ModelAnswer a = answers.get(i);
            sb.append(String.format("模型%d [%s]: %s\n\n",
                    i + 1, a.modelName, a.answer != null ? a.answer : "(无回答)"));
        }
        sb.append("请综合分析，给出最准确、最完整的答案（100字以内）：");

        try {
            Map<String, Object> body = Map.of(
                    "model", SmartModelRouter.Model.MINIMAX_TEXT_03.getName(),
                    "messages", List.of(Map.of("role", "user", "content", sb.toString())),
                    "max_tokens", 300,
                    "temperature", 0.1
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(
                    modelServiceUrl + "/models/chat",
                    body, Map.class);
            return extractAnswer(resp);
        } catch (Exception e) {
            log.warn("[Voting] LLM judge failed: {}", e.getMessage());
            return majorityVote(answers); // fallback
        }
    }

    /** 计算共识一致率 */
    private double calcAgreementScore(List<ModelAnswer> answers, String consensus) {
        if (answers.isEmpty()) return 0;
        long matches = answers.stream()
                .filter(a -> a.answer != null && normalizeForVote(a.answer).equals(normalizeForVote(consensus)))
                .count();
        return Math.round(matches * 100.0 / answers.size()) / 100.0;
    }

    /** 标准化答案用于比较（去空格/标点/大小写） */
    private String normalizeForVote(String answer) {
        if (answer == null) return "";
        return answer.strip()
                .toLowerCase()
                .replaceAll("[，。！？、；：""''']", "")
                .replaceAll("\\s+", "")
                .substring(0, Math.min(answer.length(), 80));
    }

    private String pickBestSingle(List<ModelAnswer> answers) {
        return answers.stream()
                .filter(a -> a.answer != null)
                .max(Comparator.comparingDouble(a -> a.modelScore))
                .map(a -> a.answer)
                .orElse("");
    }

    // ============== 内部数据类 ==============

    @Getter
    public static class ModelAnswer {
        final String modelName;
        final String provider;
        final String answer;         // 模型原始回答
        final String error;
        final long latencyMs;
        final double modelScore;     // 能力分（用于加权）

        public ModelAnswer(String modelName, String provider, String answer,
                          String error, long latencyMs, double modelScore) {
            this.modelName = modelName;
            this.provider = provider;
            this.answer = answer;
            this.error = error;
            this.latencyMs = latencyMs;
            this.modelScore = modelScore;
        }

        public boolean isSuccess() { return answer != null && error == null; }
    }

    @Getter
    public static class VotingResult {
        /** 各模型回答详情 */
        private final List<ModelAnswer> answers;
        /** 共识答案 */
        private final String consensus;
        /** 使用的策略 */
        private final VotingStrategy strategy;
        /** 耗时 ms */
        private final long elapsedMs;
        /** 状态信息 */
        private final String message;
        /** 一致率 */
        private final double agreementScore;

        public VotingResult(List<ModelAnswer> answers, String consensus,
                           VotingStrategy strategy, long elapsedMs,
                           String message, double agreementScore) {
            this.answers = List.copyOf(answers);
            this.consensus = consensus != null ? consensus : "";
            this.strategy = strategy;
            this.elapsedMs = elapsedMs;
            this.message = message;
            this.agreementScore = Math.max(0.0, Math.min(1.0, agreementScore));
        }
    }

    public enum VotingStrategy {
        MAJORITY,            // 多数投票
        CONFIDENCE_WEIGHTED, // 加权投票（默认）
        LLM_JUDGE,           // LLM 最终裁决
        NONE                 // 未投票
    }
}
