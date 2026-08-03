package com.minimax.ai.knowledge;

import com.minimax.ai.embedding.SimpleEmbedding;
import com.minimax.ai.tokenizer.ChineseTokenizer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KnowledgeRetrieverTest {

    private KnowledgeRetriever newRetriever() {
        ChineseTokenizer tokenizer = new ChineseTokenizer();
        com.minimax.ai.model.MiniTransformer transformer =
                new com.minimax.ai.model.MiniTransformer(8192, 128, 4, 2, 128);
        SimpleEmbedding embedding = new SimpleEmbedding(tokenizer, transformer);
        KnowledgeRetriever r = new KnowledgeRetriever(embedding, tokenizer);
        r.init();
        return r;
    }

    @Test
    void testExactMatch() {
        KnowledgeRetriever r = newRetriever();
        KnowledgeRetriever.RetrievalResult res = r.retrieve("你好");
        assertNotNull(res);
        assertNotNull(res.response);
        assertTrue(res.score >= 0.95, "exact match score should >= 0.95: " + res.score);
    }

    @Test
    void testContainsMatch() {
        KnowledgeRetriever r = newRetriever();
        KnowledgeRetriever.RetrievalResult res = r.retrieve("你能告诉我 Java 是什么吗?");
        assertNotNull(res);
        assertTrue(res.score >= 0.5, "contains match score should >= 0.5: " + res.score);
    }

    @Test
    void testNoMatchReturnsGuide() {
        KnowledgeRetriever r = newRetriever();
        KnowledgeRetriever.RetrievalResult res = r.retrieve("火星上有外星人吗");
        assertNotNull(res);
        assertTrue(res.response.contains("抱歉") || res.response.contains("没有"),
                "no match should return guide, actual: " + res.response);
    }

    @Test
    void testEmptyQuestion() {
        KnowledgeRetriever r = newRetriever();
        KnowledgeRetriever.RetrievalResult res = r.retrieve("");
        assertNotNull(res);
    }

    @Test
    void testNullQuestion() {
        KnowledgeRetriever r = newRetriever();
        KnowledgeRetriever.RetrievalResult res = r.retrieve(null);
        assertNotNull(res);
    }

    @Test
    void testReadiness() {
        KnowledgeRetriever r = newRetriever();
        assertTrue(r.isReady());
        assertTrue(r.size() >= 10, "should have at least 10 items: " + r.size());
    }
}
