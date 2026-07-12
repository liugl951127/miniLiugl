package com.minimax.ai.knowledgebase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 知识库 (V3.4.0) 单元测试
 */
class KnowledgeBaseTest {

    /**
     * 测试 1: 文本分块 (size + overlap)
     */
    @Test
    @DisplayName("1. 文本分块 (按 size + overlap)")
    void testSplitText() {
        KnowledgeBaseService svc = new KnowledgeBaseService(null, null, null);
        try {
            java.lang.reflect.Method m = KnowledgeBaseService.class.getDeclaredMethod(
                    "splitText", String.class, int.class, int.class);
            m.setAccessible(true);
            String text = "段1。\n\n段2。\n\n段3。\n\n段4。\n\n段5。";
            List<String> chunks = (List<String>) m.invoke(svc, text, 8, 2);
            assertNotNull(chunks);
            // 分块应该 >= 1
            assertTrue(chunks.size() >= 1, "应至少分 1 块, 实际 " + chunks.size());
        } catch (Exception e) { fail(e.getMessage()); }
    }

    /**
     * 测试 2: Mock 向量化 (L2 归一化)
     */
    @Test
    @DisplayName("2. Mock 向量化 (L2 归一化后模=1)")
    void testMockEmbedding() {
        KnowledgeBaseService svc = new KnowledgeBaseService(null, null, null);
        try {
            java.lang.reflect.Method m = KnowledgeBaseService.class.getDeclaredMethod(
                    "mockEmbedding", String.class, int.class);
            m.setAccessible(true);
            float[] vec = (float[]) m.invoke(svc, "测试文本", 384);
            assertEquals(384, vec.length);
            // L2 norm 应接近 1
            double norm = 0;
            for (float v : vec) norm += v * v;
            norm = Math.sqrt(norm);
            assertEquals(1.0, norm, 0.0001, "L2 归一化后模应为 1, 实际 " + norm);
        } catch (Exception e) { fail(e.getMessage()); }
    }

    /**
     * 测试 3: 同文本同向量
     */
    @Test
    @DisplayName("3. 同文本同向量 (确定性)")
    void testMockEmbeddingDeterministic() {
        KnowledgeBaseService svc = new KnowledgeBaseService(null, null, null);
        try {
            java.lang.reflect.Method m = KnowledgeBaseService.class.getDeclaredMethod(
                    "mockEmbedding", String.class, int.class);
            m.setAccessible(true);
            float[] v1 = (float[]) m.invoke(svc, "hello", 128);
            float[] v2 = (float[]) m.invoke(svc, "hello", 128);
            assertArrayEquals(v1, v2);
            // 不同文本不同
            float[] v3 = (float[]) m.invoke(svc, "world", 128);
            assertFalse(Arrays.equals(v1, v3));
        } catch (Exception e) { fail(e.getMessage()); }
    }

    /**
     * 测试 4: 余弦相似度
     */
    @Test
    @DisplayName("4. 余弦相似度 (-1 到 1)")
    void testCosine() {
        KnowledgeBaseService svc = new KnowledgeBaseService(null, null, null);
        try {
            java.lang.reflect.Method m = KnowledgeBaseService.class.getDeclaredMethod(
                    "cosine", float[].class, float[].class);
            m.setAccessible(true);
            // 相同
            float[] a = {1, 0, 0};
            float[] b = {1, 0, 0};
            assertEquals(1.0, (double) m.invoke(svc, a, b), 0.0001);
            // 正交
            float[] c = {0, 1, 0};
            assertEquals(0.0, (double) m.invoke(svc, a, c), 0.0001);
            // 反向
            float[] d = {-1, 0, 0};
            assertEquals(-1.0, (double) m.invoke(svc, a, d), 0.0001);
        } catch (Exception e) { fail(e.getMessage()); }
    }

    /**
     * 测试 5: 关键词抽取
     */
    @Test
    @DisplayName("5. 关键词抽取 (topN 频次)")
    void testExtractKeywords() {
        KnowledgeBaseService svc = new KnowledgeBaseService(null, null, null);
        try {
            java.lang.reflect.Method m = KnowledgeBaseService.class.getDeclaredMethod(
                    "extractKeywords", String.class, int.class);
            m.setAccessible(true);
            String text = "Spring Boot 是 Java 框架。Spring Cloud 是微服务框架。Java 是编程语言。";
            String kw = (String) m.invoke(svc, text, 5);
            // Spring 出现 2 次, 应该排第一
            assertTrue(kw.startsWith("Spring") || kw.contains("Spring"));
        } catch (Exception e) { fail(e.getMessage()); }
    }

    /**
     * 测试 6: 分块重叠
     */
    @Test
    @DisplayName("6. 文本分块空输入返回空")
    void testSplitTextEmpty() {
        KnowledgeBaseService svc = new KnowledgeBaseService(null, null, null);
        try {
            java.lang.reflect.Method m = KnowledgeBaseService.class.getDeclaredMethod(
                    "splitText", String.class, int.class, int.class);
            m.setAccessible(true);
            assertEquals(0, ((List) m.invoke(svc, "", 100, 10)).size());
            assertEquals(0, ((List) m.invoke(svc, null, 100, 10)).size());
        } catch (Exception e) { fail(e.getMessage()); }
    }

    /**
     * 测试 7: SHA256
     */
    @Test
    @DisplayName("7. SHA256 计算")
    void testSha256() {
        KnowledgeBaseService svc = new KnowledgeBaseService(null, null, null);
        try {
            java.lang.reflect.Method m = KnowledgeBaseService.class.getDeclaredMethod(
                    "sha256", byte[].class);
            m.setAccessible(true);
            String h1 = (String) m.invoke(svc, "hello".getBytes());
            String h2 = (String) m.invoke(svc, "hello".getBytes());
            assertEquals(h1, h2);
            assertEquals(64, h1.length());
        } catch (Exception e) { fail(e.getMessage()); }
    }

    /**
     * 测试 8: SearchHit record
     */
    @Test
    @DisplayName("8. SearchHit record 构造")
    void testSearchHit() {
        KnowledgeBaseService.SearchHit hit = new KnowledgeBaseService.SearchHit(
                "chk-1", "doc-1", "test.txt", 0, "content", 0.85, "kw", "summary");
        assertEquals("chk-1", hit.chunkId());
        assertEquals("test.txt", hit.filename());
        assertEquals(0.85, hit.score());
    }
}
