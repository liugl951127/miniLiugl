package com.minimax.ai.eval;

// Jackson: JSON 解析
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
// KnowledgeRetriever: 知识检索器 (被评测对象)
import com.minimax.ai.knowledge.KnowledgeRetriever;
// PostConstruct: Spring 初始化回调
import jakarta.annotation.PostConstruct;
// Lombok 注解
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Spring 资源加载
import org.springframework.core.io.ClassPathResource;
// Spring Bean
import org.springframework.stereotype.Service;

// IO
import java.io.InputStream;
// NIO
import java.nio.charset.StandardCharsets;
// 时间
import java.time.LocalDateTime;
// 集合
import java.util.*;
// Stream
import java.util.stream.Collectors;

/**
 * V6.1 评测服务 (EvalService)
 *
 * <h2>核心功能</h2>
 * <ol>
 *   <li>加载 regression-set.json 测试集</li>
 *   <li>跑评测 (调用 KnowledgeRetriever 检索)</li>
 *   <li>评分 (4 维规则: must_contain / expected_keywords / must_not_contain / expected_score)</li>
 *   <li>生成报告 (按类别聚合 + 失败用例)</li>
 * </ol>
 *
 * <h2>评分规则</h2>
 * <pre>
 *   passed = (must_contain 全命中)
 *         && (must_not_contain 零触发)
 *         && (expected_keywords 至少 1 命中 或 字段为空)
 *         && (expected_score 满足阈值 或 字段为空)
 * </pre>
 *
 * <h2>报告结构</h2>
 * <ul>
 *   <li>总览: total / passed / failed / passRate / avgScore / avgLatencyMs</li>
 *   <li>类别统计: 按 category 分组,每组 (total, passed, passRate)</li>
 *   <li>失败详情: 每个失败用例的 (question, answer, score, reason)</li>
 * </ul>
 *
 * <h2>用法</h2>
 * <pre>
 *   // Spring 注入
 *   {@literal @}Autowired EvalService evalService;
 *   EvalReport report = evalService.runEvaluation();
 *   log.info(report.summary());
 *
 *   // CLI
 *   java ... com.minimax.ai.eval.EvalRunner eval/regression-set.json
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvalService {

    // ============== 依赖 ==============
    // 被评测对象 (知识检索器)
    private final KnowledgeRetriever knowledgeRetriever;

    // Jackson JSON 解析器
    private final ObjectMapper mapper = new ObjectMapper();

    // ============== 配置 ==============
    /**
     * 默认评测集路径 (classpath)
     */
    public static final String DEFAULT_EVAL_SET = "eval/regression-set.json";

    // ============== 缓存 ==============
    /**
     * 加载后的用例缓存
     * 避免每次跑都重新读 JSON
     */
    private List<EvalCase> cachedCases = new ArrayList<>();

    // ============== 初始化 ==============
    /**
     * Spring 初始化: 自动加载默认评测集
     */
    @PostConstruct
    public void loadDefault() {
        try {
            loadEvalSet(DEFAULT_EVAL_SET);
        } catch (Exception e) {
            // 失败仅警告, 不影响主流程
            log.warn("加载默认评测集失败: {}", e.getMessage());
        }
    }

    // ============== 加载 API ==============
    /**
     * 从 classpath 加载评测集
     *
     * <h2>JSON 格式</h2>
     * <pre>
     * {
     *   "version": "1.0.0",
     *   "test_cases": [
     *     {
     *       "id": "java-001",
     *       "category": "编程/Java",
     *       "question": "Java 是什么",
     *       "expected_keywords": ["面向对象", "Sun"],
     *       "must_contain": ["Java"],
     *       "must_not_contain": ["不知道"],
     *       "expected_score": 0.7,
     *       "tags": ["java"]
     *     }
     *   ]
     * }
     * </pre>
     *
     * @param classpathPath classpath 相对路径
     * @return 加载的用例数
     */
    public int loadEvalSet(String classpathPath) throws Exception {
        ClassPathResource resource = new ClassPathResource(classpathPath);
        if (!resource.exists()) {
            log.warn("评测集 {} 不存在", classpathPath);
            return 0;
        }
        try (InputStream in = resource.getInputStream()) {
            // 显式 UTF-8 防止中文乱码
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            // 解析 JSON
            Map<String, Object> data = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> rawCases = (List<Map<String, Object>>) data.get("test_cases");
            if (rawCases == null) {
                log.warn("评测集 {} 缺少 test_cases 字段", classpathPath);
                return 0;
            }
            // 转换为 EvalCase 列表
            cachedCases = rawCases.stream()
                    .map(this::parseCase)   // map 转换
                    .filter(EvalCase::isValid)  // 过滤非法
                    .collect(Collectors.toList());
            log.info("已加载评测集 {}: {} 个用例", classpathPath, cachedCases.size());
            return cachedCases.size();
        }
    }

    /**
     * 把 raw Map 转为 EvalCase
     * 处理字段缺失 + 类型转换 (List/Number → EvalCase 类型)
     */
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

    // ============== 跑评测 ==============
    /**
     * 跑评测 (用 cachedCases)
     */
    public EvalReport runEvaluation() {
        return runEvaluation(cachedCases);
    }

    /**
     * 跑评测 (用指定 cases)
     *
     * <h2>算法</h2>
     * <ol>
     *   <li>遍历每个 EvalCase, 调 evaluate() 单条评测</li>
     *   <li>统计: total / passed / failed</li>
     *   <li>计算平均分 / 平均延迟</li>
     *   <li>按 category 分组聚合 (passRate)</li>
     *   <li>收集失败用例</li>
     *   <li>返回 EvalReport</li>
     * </ol>
     *
     * @param cases 评测用例列表
     * @return EvalReport
     */
    public EvalReport runEvaluation(List<EvalCase> cases) {
        // 防御: 空用例
        if (cases == null || cases.isEmpty()) {
            log.warn("评测集为空");
            return EvalReport.builder()
                    .reportId(LocalDateTime.now().toString())
                    .total(0).passed(0).failed(0)
                    .passRate(0.0)
                    .build();
        }
        log.info("开始评测: {} 个用例", cases.size());

        // 计时
        LocalDateTime start = LocalDateTime.now();
        long startMs = System.currentTimeMillis();

        // 跑所有用例 (串行)
        List<EvalResult> results = cases.stream()
                .map(this::evaluate)
                .collect(Collectors.toList());

        // 总耗时
        long totalMs = System.currentTimeMillis() - startMs;
        LocalDateTime end = LocalDateTime.now();

        // ====== 聚合统计 ======
        int total = results.size();
        int passed = (int) results.stream().filter(r -> r.passed).count();
        // 平均分 (null 跳过)
        double avgScore = results.stream()
                .filter(r -> r.score != null)
                .mapToDouble(r -> r.score)
                .average().orElse(0.0);
        // 平均延迟
        double avgLatency = results.stream()
                .mapToLong(r -> r.latencyMs)
                .average().orElse(0.0);

        // ====== 类别统计 ======
        // group by category
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

        // ====== 失败详情 ======
        List<EvalResult> failures = results.stream()
                .filter(r -> !r.passed)
                .collect(Collectors.toList());

        // 构造报告
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

        // 输出
        log.info(report.summary());
        log.info("按类别: {}", catStats.values().stream()
                .map(s -> String.format("%s(%d/%d=%.0f%%)",
                        s.category, s.passed, s.total, s.passRate * 100))
                .collect(Collectors.joining(", ")));

        return report;
    }

    // ============== 单条评测 ==============
    /**
     * 评测单条
     *
     * <h2>评分规则</h2>
     * <ol>
     *   <li><b>must_contain</b>: 全部命中才算过</li>
     *   <li><b>must_not_contain</b>: 任一出现即失败</li>
     *   <li><b>expected_keywords</b>: 至少 1 命中 (空数组不强制)</li>
     *   <li><b>expected_score</b>: 分数 >= 阈值</li>
     * </ol>
     *
     * @param testCase 评测用例
     * @return EvalResult (含详细命中/未命中)
     */
    public EvalResult evaluate(EvalCase testCase) {
        long startMs = System.currentTimeMillis();
        try {
            // 调 KnowledgeRetriever 检索
            KnowledgeRetriever.RetrievalResult rr = knowledgeRetriever.retrieve(
                    testCase.getQuestion(), testCase.getSessionId());

            // 计时
            long latency = System.currentTimeMillis() - startMs;
            // 答案 (可能为 null)
            String answer = rr.response != null ? rr.response : "";
            Double score = rr.score;

            // ====== 详细匹配 ======
            // must_contain 命中情况
            List<String> hitMust = new ArrayList<>();
            List<String> missMust = new ArrayList<>();
            if (testCase.getMustContain() != null) {
                for (String kw : testCase.getMustContain()) {
                    if (answer.contains(kw)) hitMust.add(kw);
                    else missMust.add(kw);
                }
            }
            // expected_keywords 命中
            List<String> hitKw = new ArrayList<>();
            if (testCase.getExpectedKeywords() != null) {
                for (String kw : testCase.getExpectedKeywords()) {
                    if (answer.contains(kw)) hitKw.add(kw);
                }
            }
            // must_not_contain 触发
            List<String> triggeredForbidden = new ArrayList<>();
            if (testCase.getMustNotContain() != null) {
                for (String kw : testCase.getMustNotContain()) {
                    if (answer.contains(kw)) triggeredForbidden.add(kw);
                }
            }

            // ====== 判定 ======
            boolean passed = true;
            StringBuilder reason = new StringBuilder();
            // 规则 1: must_contain 必须全命中
            if (!missMust.isEmpty()) {
                passed = false;
                reason.append("缺少关键词: ").append(missMust).append("; ");
            }
            // 规则 2: must_not_contain 零触发
            if (!triggeredForbidden.isEmpty()) {
                passed = false;
                reason.append("触发禁用词: ").append(triggeredForbidden).append("; ");
            }
            // 规则 3: expected_keywords 至少 1 命中
            if (testCase.getExpectedKeywords() != null && !testCase.getExpectedKeywords().isEmpty()
                    && hitKw.isEmpty()) {
                passed = false;
                reason.append("无期望关键词命中; ");
            }
            // 规则 4: expected_score 阈值
            if (testCase.getExpectedScore() != null && score != null
                    && score < testCase.getExpectedScore()) {
                passed = false;
                reason.append("分数 ").append(String.format("%.3f", score))
                        .append(" < 期望 ").append(testCase.getExpectedScore()).append("; ");
            }

            // 构造结果
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
            // 异常也算失败
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

    // ============== 过滤跑 ==============
    /**
     * 按 tag 跑 (只跑包含指定 tag 的用例)
     */
    public EvalReport runByTag(String tag) {
        List<EvalCase> filtered = cachedCases.stream()
                .filter(c -> c.getTags() != null && c.getTags().contains(tag))
                .collect(Collectors.toList());
        return runEvaluation(filtered);
    }

    /**
     * 按类别跑
     */
    public EvalReport runByCategory(String category) {
        List<EvalCase> filtered = cachedCases.stream()
                .filter(c -> category.equals(c.getCategory()))
                .collect(Collectors.toList());
        return runEvaluation(filtered);
    }

    /**
     * 获取当前缓存的用例 (给 Runner 用)
     */
    public List<EvalCase> getCachedCases() { return cachedCases; }
}
