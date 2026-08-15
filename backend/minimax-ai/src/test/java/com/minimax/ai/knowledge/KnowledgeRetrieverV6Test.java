package com.minimax.ai.knowledge;

import com.minimax.ai.embedding.SimpleEmbedding;
import com.minimax.ai.model.MiniTransformer;
import com.minimax.ai.tokenizer.ChineseTokenizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class KnowledgeRetrieverV6Test {

    private KnowledgeRetriever retriever;

    @BeforeEach
    void setUp() {
        ChineseTokenizer tokenizer = new ChineseTokenizer();
        MiniTransformer transformer = new MiniTransformer(8192, 64, 4, 2, 128);
        SimpleEmbedding embedding = new SimpleEmbedding(tokenizer, transformer);
        retriever = new KnowledgeRetriever(embedding, tokenizer);
        retriever.init();
    }

    @Test
    void testExactMatch() {
        KnowledgeRetriever.RetrievalResult r = retriever.retrieve("你好");
        assertNotNull(r.response);
        assertTrue(r.score >= 0.85, "精确匹配应 >= 0.85: " + r.score);
    }

    @Test
    void testContainsMatch() {
        KnowledgeRetriever.RetrievalResult r = retriever.retrieve("什么是 Java 编程语言");
        assertNotNull(r.response);
        assertTrue(r.score > 0.4, "包含匹配: " + r.score);
    }

    @Test
    void testKeywordMatch() {
        KnowledgeRetriever.RetrievalResult r = retriever.retrieve("Java 是什么");
        assertNotNull(r.response);
        assertTrue(r.response.contains("Java") || r.response.contains("面向对象"),
                "应含 Java 相关内容: " + r.response);
    }

    @Test
    void testSynonym() {
        // "退款" 在语料中, "退货" 是同义词
        KnowledgeRetriever.RetrievalResult r1 = retriever.retrieve("怎么退款");
        KnowledgeRetriever.RetrievalResult r2 = retriever.retrieve("怎么退货");
        assertNotNull(r1.response);
        assertNotNull(r2.response);
    }

    @Test
    void testTopCandidates() {
        KnowledgeRetriever.RetrievalResult r = retriever.retrieve("Python");
        assertNotNull(r.candidates);
        assertTrue(r.candidates.size() <= 3);
    }

    @Test
    void testNoMatch() {
        KnowledgeRetriever.RetrievalResult r = retriever.retrieve("xyzabc12345");
        assertTrue(r.score < 0.5, "完全无关应 < 0.5: " + r.score);
    }

    @Test
    void testEmpty() {
        KnowledgeRetriever.RetrievalResult r = retriever.retrieve("");
        assertEquals(0.0, r.score);
    }

    @Test
    void testFinance() {
        KnowledgeRetriever.RetrievalResult r = retriever.retrieve("什么是基金");
        assertNotNull(r.response);
        assertNotNull(r.response);  // 金融类只要非空即可
    }

    @Test
    void testHealth() {
        KnowledgeRetriever.RetrievalResult r = retriever.retrieve("如何预防感冒");
        assertNotNull(r.response);
        assertNotNull(r.response);
    }

    @Test
    void testLaw() {
        KnowledgeRetriever.RetrievalResult r = retriever.retrieve("劳动法规定的工作时间");
        assertNotNull(r.response);
    }

    @Test
    void testMultiTurn() {
        retriever.retrieve("Java 是什么", "session-1");
        KnowledgeRetriever.RetrievalResult r = retriever.retrieve("它有什么特点", "session-1");
        assertNotNull(r.response);
    }

    @Test
    void testSize() {
        int size = retriever.size();
        assertTrue(size > 100, "应 > 100 知识条目: " + size);
    }
}
