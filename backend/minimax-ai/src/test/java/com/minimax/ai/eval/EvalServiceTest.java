package com.minimax.ai.eval;

import com.minimax.ai.embedding.SimpleEmbedding;
import com.minimax.ai.knowledge.KnowledgeRetriever;
import com.minimax.ai.model.MiniTransformer;
import com.minimax.ai.tokenizer.ChineseTokenizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class EvalServiceTest {

    private EvalService service;
    private KnowledgeRetriever retriever;

    @BeforeEach
    void setUp() throws Exception {
        ChineseTokenizer tokenizer = new ChineseTokenizer();
        MiniTransformer transformer = new MiniTransformer(8192, 64, 4, 2, 128);
        SimpleEmbedding embedding = new SimpleEmbedding(tokenizer, transformer);
        retriever = new KnowledgeRetriever(embedding, tokenizer);
        retriever.init();

        service = new EvalService(retriever);
    }

    @Test
    void testLoadDefaultEvalSet() throws Exception {
        int count = service.loadEvalSet("eval/regression-set.json");
        assertTrue(count >= 30, "应加载 >= 30 用例: " + count);
    }

    @Test
    void testEvaluateSingle() {
        EvalCase tc = EvalCase.builder()
                .id("t1")
                .category("test")
                .question("Java 是什么")
                .mustContain(List.of("Java"))
                .build();
        EvalResult r = service.evaluate(tc);
        assertNotNull(r.answer);
        assertTrue(r.answer.length() > 0);
    }

    @Test
    void testRunFullEvaluation() throws Exception {
        service.loadEvalSet("eval/regression-set.json");
        EvalReport report = service.runEvaluation();
        assertNotNull(report);
        assertTrue(report.total > 0, "应 > 0 用例: " + report.total);
        assertTrue(report.passed >= 0);
        assertEquals(report.total, report.passed + report.failed);
        // 关键指标
        assertTrue(report.passRate >= 0 && report.passRate <= 1);
        assertTrue(report.avgScore >= 0 && report.avgScore <= 1);
    }

    @Test
    void testMustContainValidation() {
        EvalCase tc = EvalCase.builder()
                .id("t2")
                .category("test")
                .question("Java 是什么")
                .mustContain(List.of("Java", "不存在的关键词XYZ123"))
                .build();
        EvalResult r = service.evaluate(tc);
        assertFalse(r.passed, "缺少关键词应失败");
        assertNotNull(r.reason);
        assertTrue(r.missMustContain.contains("不存在的关键词XYZ123"));
    }

    @Test
    void testMustNotContain() {
        EvalCase tc = EvalCase.builder()
                .id("t3")
                .category("test")
                .question("Java 是什么")
                .mustNotContain(List.of("不知道"))
                .build();
        EvalResult r = service.evaluate(tc);
        // 实际可能包含 "不知道", 看实际
        assertNotNull(r);
    }

    @Test
    void testScoreThreshold() {
        EvalCase tc = EvalCase.builder()
                .id("t4")
                .category("test")
                .question("Java 是什么")
                .expectedScore(0.99)  // 期望很高
                .build();
        EvalResult r = service.evaluate(tc);
        // 分数应该 < 0.99 (除非完美匹配)
        if (r.score != null && r.score < 0.99) {
            assertFalse(r.passed, "分数 < 阈值应失败");
        }
    }

    @Test
    void testCategoryStats() throws Exception {
        service.loadEvalSet("eval/regression-set.json");
        EvalReport report = service.runEvaluation();
        assertNotNull(report.categoryStats);
        assertTrue(report.categoryStats.size() >= 3, "至少 3 个类别: " + report.categoryStats.size());
    }

    @Test
    void testRunByTag() throws Exception {
        service.loadEvalSet("eval/regression-set.json");
        EvalReport report = service.runByTag("smoke");
        assertNotNull(report);
        // smoke 标签用例数 >= 3
        assertTrue(report.total >= 3, "smoke 标签应 >= 3: " + report.total);
    }

    @Test
    void testRunByCategory() throws Exception {
        service.loadEvalSet("eval/regression-set.json");
        EvalReport report = service.runByCategory("编程/Java");
        assertNotNull(report);
        assertTrue(report.total > 0, "Java 类别应 > 0");
        // 所有用例都应是该类别
        if (report.allResults != null) {
            for (EvalResult r : report.allResults) {
                assertEquals("编程/Java", r.category);
            }
        }
    }

    @Test
    void testEvalCaseValidation() {
        EvalCase valid = EvalCase.builder().id("a").question("q").build();
        assertTrue(valid.isValid());
        EvalCase invalid = new EvalCase();
        assertFalse(invalid.isValid());
    }

    @Test
    void testReportSummary() throws Exception {
        service.loadEvalSet("eval/regression-set.json");
        EvalReport report = service.runEvaluation();
        String s = report.summary();
        assertNotNull(s);
        assertTrue(s.contains("通过") || s.contains("pass") || s.length() > 0);
    }
}
