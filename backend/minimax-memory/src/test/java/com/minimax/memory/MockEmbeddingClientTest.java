package com.minimax.memory;

import com.minimax.memory.embedding.MockEmbeddingClient;
import com.minimax.memory.longterm.VectorUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class MockEmbeddingClientTest {

    private MockEmbeddingClient client() {
        MockEmbeddingClient c = new MockEmbeddingClient();
        ReflectionTestUtils.setField(c, "dim", 64);
        return c;
    }

    @Test
    void embedProducesCorrectDim() {
        MockEmbeddingClient c = client();
        float[] v = c.embed("你好世界");
        assertEquals(64, v.length);
    }

    @Test
    void embedEmptyText() {
        MockEmbeddingClient c = client();
        float[] v1 = c.embed(null);
        assertEquals(64, v1.length);
        float[] v2 = c.embed("");
        assertEquals(64, v2.length);
    }

    @Test
    void sameTextProducesSameVector() {
        MockEmbeddingClient c = client();
        float[] v1 = c.embed("今天天气很好");
        float[] v2 = c.embed("今天天气很好");
        assertEquals(1.0, VectorUtils.cosine(v1, v2), 1e-6);
    }

    @Test
    void mockEmbeddingIsSemanticStable() {
        // Mock 算法的语义强度有限；不强求 "相关 > 不相关"，
        // 但保证：相同输入 → 完全相同向量（确定性）
        MockEmbeddingClient c = client();
        float[] a1 = c.embed("今天北京天气很好，适合出去玩");
        float[] a2 = c.embed("今天北京天气很好，适合出去玩");
        assertEquals(1.0, VectorUtils.cosine(a1, a2), 1e-6, "deterministic");

        // 不同输入能产生不同的向量
        float[] b = c.embed("Java 编程语言");
        assertTrue(VectorUtils.cosine(a1, b) < 1.0 - 1e-6,
                "different text should produce different vector");
    }

    @Test
    void vectorIsNormalized() {
        MockEmbeddingClient c = client();
        float[] v = c.embed("测试");
        double norm = 0;
        for (float x : v) norm += x * x;
        norm = Math.sqrt(norm);
        assertEquals(1.0, norm, 1e-5);
    }
}
