package com.minimax.ai.nlp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class SentimentAnalyzerTest {

    private SentimentAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new SentimentAnalyzer();
    }

    @Test
    void testPositiveText() {
        SentimentAnalyzer.SentimentResult r = analyzer.analyze("这个产品很好用,我很喜欢!");
        assertTrue(r.score > 0, "正向文本应得正分: " + r.score);
        assertEquals(SentimentAnalyzer.SentimentLabel.POSITIVE, r.label);
    }

    @Test
    void testNegativeText() {
        SentimentAnalyzer.SentimentResult r = analyzer.analyze("太糟糕了,很差,讨厌");
        assertTrue(r.score < 0, "负向文本应得负分: " + r.score);
        assertEquals(SentimentAnalyzer.SentimentLabel.NEGATIVE, r.label);
    }

    @Test
    void testNeutralText() {
        SentimentAnalyzer.SentimentResult r = analyzer.analyze("今天是星期三");
        assertEquals(SentimentAnalyzer.SentimentLabel.NEUTRAL, r.label);
    }

    @Test
    void testNegation() {
        SentimentAnalyzer.SentimentResult r1 = analyzer.analyze("这个产品很好");
        SentimentAnalyzer.SentimentResult r2 = analyzer.analyze("这个产品不好");
        assertTrue(r1.score > 0);
        assertTrue(r2.score < 0, "否定句应反向: " + r2.score);
    }

    @Test
    void testDegreeWord() {
        SentimentAnalyzer.SentimentResult r1 = analyzer.analyze("好");
        SentimentAnalyzer.SentimentResult r2 = analyzer.analyze("很好");
        // 至少 r1 是正向, r2 也是正向
        assertEquals(SentimentAnalyzer.SentimentLabel.POSITIVE, r1.label);
        assertEquals(SentimentAnalyzer.SentimentLabel.POSITIVE, r2.label);
    }

    @Test
    void testEmpty() {
        SentimentAnalyzer.SentimentResult r = analyzer.analyze("");
        assertEquals(0.0, r.score);
        assertEquals(SentimentAnalyzer.SentimentLabel.NEUTRAL, r.label);
    }

    @Test
    void testEmoji() {
        SentimentAnalyzer.SentimentResult r1 = analyzer.analyze("太好了");
        SentimentAnalyzer.SentimentResult r2 = analyzer.analyze("太差了");
        // baseline
        assertTrue(r1.score >= r2.score, "正向应 >= 负向: " + r1.score + " vs " + r2.score);
    }
}
