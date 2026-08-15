package com.minimax.ai.generation;

import com.minimax.ai.generation.model.OnlineLearningEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OnlineLearningEngine 在线学习测试
 */
@DisplayName("OnlineLearningEngine 反馈学习测试")
class OnlineLearningEngineTest {

    private OnlineLearningEngine engine;

    @BeforeEach
    void setUp() {
        engine = new OnlineLearningEngine();
    }

    @Test
    @DisplayName("初始权重正确")
    void testInitialWeights() {
        Map<OnlineLearningEngine.Model, Double> w = engine.getWeights();
        assertEquals(0.4, w.get(OnlineLearningEngine.Model.TF));
        assertEquals(0.3, w.get(OnlineLearningEngine.Model.NGRAM));
        assertEquals(0.2, w.get(OnlineLearningEngine.Model.SYNONYM));
        assertEquals(0.1, w.get(OnlineLearningEngine.Model.CONTEXT));
    }

    @Test
    @DisplayName("accept 强化胜出模型")
    void testAccept() {
        Map<OnlineLearningEngine.Model, Double> votes = new HashMap<>();
        votes.put(OnlineLearningEngine.Model.TF, 0.8);
        votes.put(OnlineLearningEngine.Model.NGRAM, 0.2);
        engine.recordVote("s1", "画柱状图", "CHART", votes);

        double before = engine.getWeights().get(OnlineLearningEngine.Model.TF);
        engine.accept("s1", "画柱状图");
        double after = engine.getWeights().get(OnlineLearningEngine.Model.TF);

        assertTrue(after > before, "TF weight should increase on accept");
        assertEquals(1L, engine.stats().get("acceptCount"));
    }

    @Test
    @DisplayName("correct 弱化原模型, 强化其他")
    void testCorrect() {
        Map<OnlineLearningEngine.Model, Double> votes = new HashMap<>();
        votes.put(OnlineLearningEngine.Model.TF, 0.8);
        votes.put(OnlineLearningEngine.Model.NGRAM, 0.2);
        engine.recordVote("s2", "画饼图", "CHART", votes);

        double tfBefore = engine.getWeights().get(OnlineLearningEngine.Model.TF);
        engine.correct("s2", "画饼图", "MUSIC");
        double tfAfter = engine.getWeights().get(OnlineLearningEngine.Model.TF);

        // TF 被弱化
        assertTrue(tfAfter < tfBefore, "TF should decrease on correct");
        assertEquals(1L, engine.stats().get("correctCount"));
    }

    @Test
    @DisplayName("reject 全部权重降低")
    void testReject() {
        Map<OnlineLearningEngine.Model, Double> votes = new HashMap<>();
        votes.put(OnlineLearningEngine.Model.TF, 0.5);
        engine.recordVote("s3", "x", "CHAT", votes);

        engine.reject("s3", "x");
        assertEquals(1L, engine.stats().get("rejectCount"));
        // TF 应该在合理范围 (没归零)
        double tf = engine.getWeights().get(OnlineLearningEngine.Model.TF);
        assertTrue(tf > 0.0, "TF should not be zero");
    }

    @Test
    @DisplayName("权重 clamp 到 [0.05, 0.8]")
    void testClamp() {
        // 强制 100 次 accept, TF 不应该 > 0.8
        Map<OnlineLearningEngine.Model, Double> votes = new HashMap<>();
        votes.put(OnlineLearningEngine.Model.TF, 1.0);
        engine.recordVote("s", "q", "CHART", votes);
        for (int i = 0; i < 100; i++) {
            engine.accept("s", "q");
        }
        assertTrue(engine.getWeights().get(OnlineLearningEngine.Model.TF) <= 0.8);
    }

    @Test
    @DisplayName("reset 恢复初始权重")
    void testReset() {
        engine.resetWeights();
        assertEquals(0.4, engine.getWeights().get(OnlineLearningEngine.Model.TF));
    }
}
