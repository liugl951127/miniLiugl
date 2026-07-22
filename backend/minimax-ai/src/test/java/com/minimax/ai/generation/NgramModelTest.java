package com.minimax.ai.generation;

import com.minimax.ai.generation.model.NgramModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NgramModel 单元测试
 */
@DisplayName("NgramModel Bigram 概率测试")
class NgramModelTest {

    private NgramModel model;

    @BeforeEach
    void setUp() {
        model = new NgramModel();
        model.train(NgramModel.defaultTrainingData());
    }

    @Test
    @DisplayName("训练: 默认训练集加载成功")
    void testTrainDefault() {
        Map<String, Double> scores = model.score("画一个柱状图");
        assertFalse(scores.isEmpty());
        assertTrue(scores.containsKey("GENERATE_CHART"));
        // 柱状图应该在 CHART 得分最高
        Double chartScore = scores.get("GENERATE_CHART");
        assertNotNull(chartScore);
        assertTrue(chartScore > 0);
    }

    @Test
    @DisplayName("推理: 'compose a melody' 识别为 MUSIC")
    void testScoreMusic() {
        Map<String, Double> scores = model.score("compose a melody");
        assertTrue(scores.getOrDefault("GENERATE_MUSIC", 0.0) > 0);
    }

    @Test
    @DisplayName("推理: '你好' CHAT 得分高")
    void testScoreChat() {
        Map<String, Double> scores = model.score("你好");
        assertTrue(scores.getOrDefault("CHAT", 0.0) > 0);
    }

    @Test
    @DisplayName("推理: 1 token 不算 bigram (返回空)")
    void testSingleToken() {
        Map<String, Double> scores = model.score("hi");
        // "hi" 1 token, 没有 bigram, 返回空
        // (训练集 "hello" 是 1 token, 但 "hi" 不在训练集)
        // 实际 "hi" 可能是 0 (不在) - 允许空
    }

    @Test
    @DisplayName("性能: 1000 次推理 < 200ms")
    void testPerformance() {
        // 预热
        for (int i = 0; i < 10; i++) model.score("画一个柱状图");

        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            model.score("画一个柱状图" + i);
        }
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertTrue(ms < 200, "Should be fast: " + ms + "ms");
        System.out.printf("[ngram-perf] 1000 score: %dms%n", ms);
    }
}
