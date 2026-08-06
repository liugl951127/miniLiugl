package com.minimax.ai.nlp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class NerExtractorTest {

    private NerExtractor ner;

    @BeforeEach
    void setUp() {
        ner = new NerExtractor();
    }

    @Test
    void testExtractCity() {
        List<NerExtractor.Entity> r = ner.extract("我从北京去上海");
        assertTrue(r.size() > 0, "应至少识别一个实体: " + r.size());
    }

    @Test
    void testExtractUrl() {
        List<NerExtractor.Entity> r = ner.extract("访问 https://www.example.com 看看");
        assertTrue(r.stream().anyMatch(e -> e.text.contains("example.com") && e.type.equals("URL")));
    }

    @Test
    void testExtractPhone() {
        List<NerExtractor.Entity> r = ner.extract("联系我 18812345678");
        assertTrue(r.stream().anyMatch(e -> e.text.equals("18812345678") && e.type.equals("PHONE")));
    }

    @Test
    void testExtractEmail() {
        List<NerExtractor.Entity> r = ner.extract("邮箱 user@example.com 收到回信");
        assertTrue(r.stream().anyMatch(e -> e.text.equals("user@example.com") && e.type.equals("EMAIL")));
    }

    @Test
    void testExtractTime() {
        List<NerExtractor.Entity> r = ner.extract("2026-08-06 15:30 出发");
        assertTrue(r.stream().anyMatch(e -> e.text.contains("2026-08-06") && e.type.equals("TIME")));
    }

    @Test
    void testExtractLanguage() {
        List<NerExtractor.Entity> r = ner.extract("用 Java 和 Python 开发");
        assertTrue(r.stream().anyMatch(e -> e.text.equals("Java") && e.type.equals("LANG")));
        assertTrue(r.stream().anyMatch(e -> e.text.equals("Python") && e.type.equals("LANG")));
    }

    @Test
    void testExtractEmpty() {
        List<NerExtractor.Entity> r = ner.extract("");
        assertTrue(r.isEmpty());
    }

    @Test
    void testExtractMixed() {
        List<NerExtractor.Entity> r = ner.extract("在 2026-08-06 去北京, 联系 18812345678");
        assertTrue(r.size() >= 3, "应有多个实体: " + r.size());
    }
}
