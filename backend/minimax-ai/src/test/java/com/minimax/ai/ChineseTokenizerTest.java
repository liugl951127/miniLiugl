package com.minimax.ai;

import com.minimax.ai.tokenizer.ChineseTokenizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BPE 中文分词器测试 (V2.6)
 */
class ChineseTokenizerTest {

    @Test
    void testPreTokenizeChinese() {
        ChineseTokenizer tk = new ChineseTokenizer();
        List<String> tokens = tk.preTokenize("你好世界");
        assertEquals(4, tokens.size(), "中文每个字 1 个 token");
        assertTrue(tokens.contains("你"));
        assertTrue(tokens.contains("好"));
    }

    @Test
    void testPreTokenizeEnglish() {
        ChineseTokenizer tk = new ChineseTokenizer();
        List<String> tokens = tk.preTokenize("Hello World");
        // "Hello" "World"
        assertTrue(tokens.contains("hello"));
        assertTrue(tokens.contains("world"));
        assertEquals(2, tokens.size());
    }

    @Test
    void testPreTokenizeMixed() {
        ChineseTokenizer tk = new ChineseTokenizer();
        List<String> tokens = tk.preTokenize("Java 是一种编程语言");
        // "java" "是" "一" "种" "编" "程" "语" "言"
        assertTrue(tokens.size() >= 8, "至少 8 个 token");
    }

    @Test
    void testPreTokenizeNumber() {
        ChineseTokenizer tk = new ChineseTokenizer();
        List<String> tokens = tk.preTokenize("价格 99 元");
        assertTrue(tokens.contains("99"), "数字应作为独立 token");
    }

    @Test
    void testEncodeDecode() {
        ChineseTokenizer tk = new ChineseTokenizer();
        // 训练一下
        tk.train(List.of("你好", "世界", "Java", "Spring"), 100);

        String text = "你好 Java";
        int[] ids = tk.encode(text);
        assertTrue(ids.length > 0);

        String decoded = tk.decode(ids);
        // 解码应保留内容
        assertFalse(decoded.isEmpty());
    }

    @Test
    void testSpecialTokens() {
        ChineseTokenizer tk = new ChineseTokenizer();
        assertEquals(0, ChineseTokenizer.PAD);
        assertEquals(1, ChineseTokenizer.UNK);
        assertEquals(2, ChineseTokenizer.BOS);
        assertEquals(3, ChineseTokenizer.EOS);

        int[] encoded = tk.encodeForTraining("测试");
        assertEquals(ChineseTokenizer.BOS, encoded[0], "第一个是 BOS");
        assertEquals(ChineseTokenizer.EOS, encoded[encoded.length - 1], "最后一个是 EOS");
    }

    @Test
    void testIsChineseChar() {
        assertTrue(ChineseTokenizer.isChineseChar('中'));
        assertTrue(ChineseTokenizer.isChineseChar('国'));
        assertFalse(ChineseTokenizer.isChineseChar('a'));
        assertFalse(ChineseTokenizer.isChineseChar('1'));
    }

    @Test
    void testIsPunctuation() {
        assertTrue(ChineseTokenizer.isPunctuation('，'));
        assertTrue(ChineseTokenizer.isPunctuation('!'));
        assertTrue(ChineseTokenizer.isPunctuation('?'));
        assertFalse(ChineseTokenizer.isPunctuation('中'));
    }
}
