package com.minimax.ai.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.ai.knowledge.KnowledgeRetriever;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 评测服务 (EvalService)
 *
 * 核心功能:
 *   1. 加载 regression-set.json 测试集
 *   2. 跑评测 (调用 KnowledgeRetriever 检索)
 *   3. 评分 (must_contain / expected_keywords / must_not_contain / score 阈值)
 *   4. 生成报告 (按类别聚合 + 失败用例)
 *   5. 持久化报告 (用于 CI 集成)
 *
 * 用法:
 *   1. Spring 注入: EvalService evalService;
 *   2. evalService.runEvaluation() 返回 EvalReport
 *   3. CLI 模式: mvn exec:java -Dexec.mainClass=...EvalRunner
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvalService {

    private final KnowledgeRetriever knowledgeRetriever;

    private final ObjectMapper mapper = new ObjectMapper();

    /** 默认评测集路径 */
    public static final String DEFAULT_EVAL_SET = "eval/regression-set.json";

    private List<EvalCase> cachedCases = new ArrayList<>();

    @PostConstruct
    public void loadDefault() {
        try {
            loadEvalSet(DEFAULT_EVAL_SET);
        } catch (Exception e) {
            log.warn("加载默认评测集失败: {}", e.getMessage());
        }
    }

    /**
     * 加载评测集
     */
    public int loadEvalSet(String classpathPath) throws Exception {
        ClassPathResource resource = new ClassPathResource(classpathPath);
        if (!resource.exists()) {
            log.warn("评测集 {} 不存在", classpathPath);
            return 0;
        }
        try (InputStream in = resource.getInputStream()) {
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> data = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> rawCases = (List<Map<String, Object>>) data.get("test_cases");
            if (rawCases == null) {
                log.warn("评测集 {} 缺少 test_cases 字段", classpathPath);
                return 0;
            }
            cachedCases = rawCases.stream()
                    .map(this::parseCase)
                    .filter(EvalCase::isValid)
                    .collect(Collectors.toList());
            log.info("已加载评测集 {}: {} 个用例", classpathPath, cachedCases.size());
            return cachedCases.size();
        }
    }

    @SuppressWarnings("unchecked")
    private EvalCase parseCase(Map<String, Object> raw) {
        return EvalCase.builder()
                .id((String) raw.get("id"))
                .category((String) raw.getOrDefault("category", "未分类"))
                .question((String) raw.get("question"))
                .expectedKeywords((List<String>) raw.get("expected_keywords"))
                .mustContain((List<String>) raw.get("must_contain"))
                .mustNotContain((List<String>) raw.get("must_not_contain"))
                .expectedScore(raw.get("expected_score") != null
                        ? ((Number) raw.get("expected_score")).doubleValue() : null)
                .sessionId((String) raw.get("session_id"))
                .tags((List<String>) raw.get("tags"))
                .difficulty(raw.get("difficulty") != null
                        ? ((Number) raw.get("difficulty")).intValue() : null)
                .build();
    }

    /**
     * 跑评测 (用 cachedCases)
     */
    public EvalReport runEvaluation() {
        return runEvaluation(cachedCases);
    }

    /**
     * 跑评测 (用指定 cases)
     */
    public EvalReport runEvaluation(List<EvalCase> cases) {
        if (cases == null || cases.isEmpty()) {
            log.warn("评测集为空");
            return EvalReport.builder()
                    .reportId(LocalDateTime.now().toString())
                    .total(0).passed(0).failed(0)
                    .passRate(0.0)
                    .build();
        }
        log.info("开始评测: {} 个用例", cases.size());

        LocalDateTime start = LocalDateTime.now();
        long startMs = System.currentTimeMillis();

        List<EvalResult> results = cases.stream()
                .map(this::evaluate)
                .collect(Collectors.toList());

        long totalMs = System.currentTimeMillis() - startMs;
        LocalDateTime end = LocalDateTime.now();

        // 聚合
        int total = results.size();
        int passed = (int) results.stream().filter(r -> r.passed).count();
        double avgScore = results.stream()
                .filter(r -> r.score != null)
                .mapToDouble(r -> r.score)
                .average().orElse(0.0);
        double avgLatency = results.stream()
                .mapToLong(r -> r.latencyMs)
                .average().orElse(0.0);

        // 类别统计
        Map<String, List<EvalResult>> byCat = results.stream()
                .collect(Collectors.groupingBy(r -> r.category));
        Map<String, EvalReport.CategoryStat> catStats = new LinkedHashMap<>();
        byCat.forEach((cat, list) -> {
            int p = (int) list.stream().filter(r -> r.passed).count();
            catStats.put(cat, EvalReport.CategoryStat.builder()
                    .category(cat)
                    .total(list.size())
                    .passed(p)
                    .passRate(list.isEmpty() ? 0 : (double) p / list.size())
                    .build());
        });

        // 失败用例
        List<EvalResult> failures = results.stream()
                .filter(r -> !r.passed)
                .collect(Collectors.toList());

        EvalReport report = EvalReport.builder()
                .reportId(start.toString())
                .startedAt(start)
                .finishedAt(end)
                .totalMs(totalMs)
                .total(total)
                .passed(passed)
                .failed(total - passed)
                .passRate(total == 0 ? 0 : (double) passed / total)
                .avgScore(avgScore)
                .avgLatencyMs(avgLatency)
                .categoryStats(catStats)
                .failures(failures)
                .allResults(results)
                .build();

        log.info(report.summary());
        log.info("按类别: {}", catStats.values().stream()
                .map(s -> String.format("%s(%d/%d=%.0f%%)",
                        s.category, s.passed, s.total, s.passRate * 100))
                .collect(Collectors.joining(", ")));

        return report;
    }

    /**
     * 评测单条
     */
    public EvalResult evaluate(EvalCase testCase) {
        long startMs = System.currentTimeMillis();
        try {
            // 调 KnowledgeRetriever
            KnowledgeRetriever.RetrievalResult rr = knowledgeRetriever.retrieve(
                    testCase.getQuestion(), testCase.getSessionId());

            long latency = System.currentTimeMillis() - startMs;
            String answer = rr.response != null ? rr.response : "";
            Double score = rr.score;

            // 评分
            List<String> hitMust = new ArrayList<>();
            List<String> missMust = new ArrayList<>();
            if (testCase.getMustContain() != null) {
                for (String kw : testCase.getMustContain()) {
                    if (answer.contains(kw)) hitMust.add(kw);
                    else missMust.add(kw);
                }
            }
            List<String> hitKw = new ArrayList<>();
            if (testCase.getExpectedKeywords() != null) {
                for (String kw : testCase.getExpectedKeywords()) {
                    if (answer.contains(kw)) hitKw.add(kw);
                }
            }
            List<String> triggeredForbidden = new ArrayList<>();
            if (testCase.getMustNotContain() != null) {
                for (String kw : testCase.getMustNotContain()) {
                    if (answer.contains(kw)) triggeredForbidden.add(kw);
                }
            }

            // 判定
            boolean passed = true;
            StringBuilder reason = new StringBuilder();
            if (!missMust.isEmpty()) {
                passed = false;
                reason.append("缺少关键词: ").append(missMust).append("; ");
            }
            if (!triggeredForbidden.isEmpty()) {
                passed = false;
                reason.append("触发禁用词: ").append(triggeredForbidden).append("; ");
            }
            if (testCase.getExpectedKeywords() != null && !testCase.getExpectedKeywords().isEmpty()
                    && hitKw.isEmpty()) {
                passed = false;
                reason.append("无期望关键词命中; ");
            }
            if (testCase.getExpectedScore() != null && score != null
                    && score < testCase.getExpectedScore()) {
                passed = false;
                reason.append("分数 ").append(String.format("%.3f", score))
                        .append(" < 期望 ").append(testCase.getExpectedScore()).append("; ");
            }

            return EvalResult.builder()
                    .caseId(testCase.getId())
                    .category(testCase.getCategory())
                    .question(testCase.getQuestion())
                    .answer(answer)
                    .score(score)
                    .passed(passed)
                    .reason(passed ? null : reason.toString().trim())
                    .latencyMs(latency)
                    .hitMustContain(hitMust)
                    .missMustContain(missMust)
                    .hitKeywords(hitKw)
                    .triggeredForbidden(triggeredForbidden)
                    .build();
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startMs;
            log.warn("评测 {} 异常: {}", testCase.getId(), e.getMessage());
            return EvalResult.builder()
                    .caseId(testCase.getId())
                    .category(testCase.getCategory())
                    .question(testCase.getQuestion())
                    .answer("ERROR: " + e.getMessage())
                    .score(0.0)
                    .passed(false)
                    .reason("评测异常: " + e.getClass().getSimpleName())
                    .latencyMs(latency)
                    .build();
        }
    }

    /**
     * 评测指定 tag 的用例
     */
    public EvalReport runByTag(String tag) {
        List<EvalCase> filtered = cachedCases.stream()
                .filter(c -> c.getTags() != null && c.getTags().contains(tag))
                .collect(Collectors.toList());
        return runEvaluation(filtered);
    }

    /**
     * 评测指定类别的用例
     */
    public EvalReport runByCategory(String category) {
        List<EvalCase> filtered = cachedCases.stream()
                .filter(c -> category.equals(c.getCategory()))
                .collect(Collectors.toList());
        return runEvaluation(filtered);
    }

    public List<EvalCase> getCachedCases() { return cachedCases; }
}
