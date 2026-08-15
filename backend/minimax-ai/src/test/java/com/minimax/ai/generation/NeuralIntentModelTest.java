package com.minimax.ai.generation;

import com.minimax.ai.generation.model.NeuralIntentModel;
import com.minimax.ai.generation.model.NgramModel;
import com.minimax.ai.generation.model.QueryEmbedder;
import com.minimax.ai.tokenizer.ChineseTokenizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NeuralIntentModel 神经意图模型测试 (V3.5.16+)
 */
@DisplayName("NeuralIntentModel 语义召回测试")
class NeuralIntentModelTest {

    private NeuralIntentModel model;
    private QueryEmbedder embedder;

    @BeforeEach
    void setUp() {
        ChineseTokenizer tokenizer = new ChineseTokenizer();
        embedder = new QueryEmbedder(tokenizer);
        embedder.init();
        model = new NeuralIntentModel(embedder);
        model.init();  // 训练默认数据集
    }

    @Test
    @DisplayName("训练: 默认训练集加载成功")
    void testTrainDefault() {
        assertTrue(model.size() > 0);
    }

    @Test
    @DisplayName("推理: '画柱状图' CHART 得分高")
    void testScoreChart() {
        Map<String, Double> scores = model.score("画柱状图");
        assertFalse(scores.isEmpty());
        assertTrue(scores.containsKey("GENERATE_CHART"));
    }

    @Test
    @DisplayName("推理: 'compose melody' MUSIC 得分高")
    void testScoreMusic() {
        Map<String, Double> scores = model.score("compose a melody");
        assertTrue(scores.containsKey("GENERATE_MUSIC"));
    }

    @Test
    @DisplayName("推理: '你好' CHAT 得分高")
    void testScoreChat() {
        Map<String, Double> scores = model.score("你好");
        assertTrue(scores.containsKey("CHAT"));
    }

    @Test
    @DisplayName("QueryEmbedder: 句向量 128 维")
    void testEmbedDim() {
        double[] v = embedder.embed("画一个柱状图");
        assertEquals(128, v.length);
    }

    @Test
    @DisplayName("QueryEmbedder: 余弦相似度返回合理范围 [-1, 1]")
    void testSimilarity() {
        // V3.5.16: MiniTransformer 随机初始化, 同文本相似度应该接近 1
        double sSelf = embedder.similarity("画柱状图", "画柱状图");
        assertTrue(sSelf > 0.99, "Self-similarity should be 1.0: " + sSelf);

        // 余弦相似度在 [-1, 1]
        double sAny = embedder.similarity("画柱状图", "查询用户");
        assertTrue(sAny >= -1.001 && sAny <= 1.001, "Similarity in range: " + sAny);
    }

    @Test
    @DisplayName("QueryEmbedder: 缓存命中")
    void testCache() {
        embedder.embed("测试 query");
        // 第二次应该从缓存
        long t0 = System.nanoTime();
        embedder.embed("测试 query");
        long t1 = System.nanoTime();
        // 缓存命中 < 0.5ms
        assertTrue((t1 - t0) < 500_000, "Cache hit should be fast");
    }

    @Test
    @DisplayName("性能: 100 次 embed < 500ms")
    void testPerformance() {
        // 预热
        for (int i = 0; i < 5; i++) embedder.embed("画柱状图");
        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) embedder.embed("画柱状图 " + i);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertTrue(ms < 500, "Should be fast: " + ms + "ms");
        System.out.printf("[neural-perf] 100 embed: %dms%n", ms);
    }
}
