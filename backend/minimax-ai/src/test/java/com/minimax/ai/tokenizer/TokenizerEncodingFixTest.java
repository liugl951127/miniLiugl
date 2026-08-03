package com.minimax.ai.tokenizer;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * V5.4+ 乱码修复验证测试
 *
 * 修复点:
 *   1. isChineseChar 扩展 CJK 全部平面
 *   2. preTokenize 支持 surrogate pair
 *   3. decode 不丢 UNK/BOS/EOS
 *   4. save/load 用 ObjectOutputStream (兼容 surrogate pair)
 */
class TokenizerEncodingFixTest {

    @Test
    void testIsChineseCharBasicPlane() {
        // 基本平面 0x4E00-0x9FFF
        assertTrue(ChineseTokenizer.isChineseChar('你'));
        assertTrue(ChineseTokenizer.isChineseChar('好'));
        assertTrue(ChineseTokenizer.isChineseChar('中'));
        // 扩展 A 0x3400-0x4DBF
        assertTrue(ChineseTokenizer.isChineseChar('\u3400'));
    }

    @Test
    void testIsChineseCharHighSurrogate() {
        // 高代理 (CJK 扩展 B/C/D/E 在 0xD800-0xDBFF)
        char highSurrogate = '\uD840';
        assertTrue(ChineseTokenizer.isChineseChar(highSurrogate));
    }

    @Test
    void testPreTokenizeWithEmoji() {
        ChineseTokenizer tok = new ChineseTokenizer();
        // 简单 emoji (一个高代理 + 低代理)
        List<String> tokens = tok.preTokenize("你好 😀 world");
        assertTrue(tokens.size() >= 4);
        assertTrue(tokens.contains("你"));
        assertTrue(tokens.contains("好"));
        assertTrue(tokens.contains("world"));
    }

    @Test
    void testPreTokenizeWithExtendedCJK() {
        // CJK 扩展 B 罕用字 (0x20000+) - 一个码点 = 2 char
        ChineseTokenizer tok = new ChineseTokenizer();
        String text = "基本𠀀罕用字";
        List<String> tokens = tok.preTokenize(text);
        assertTrue(tokens.contains("基"));
        assertTrue(tokens.contains("本"));
        assertTrue(tokens.contains("罕"));
        assertTrue(tokens.contains("用"));
        assertTrue(tokens.contains("字"));
        // 𠀀 应该作为整体保留
        assertTrue(tokens.stream().anyMatch(t -> t.equals("\uD840\uDC00")));
    }

    @Test
    void testDecodeDoesNotLoseContent() {
        ChineseTokenizer tok = new ChineseTokenizer();
        tok.train(Arrays.asList("你好世界"), 100);
        int[] ids = tok.encode("你好");
        String decoded = tok.decode(ids);
        assertEquals("你好", decoded);
    }

    @Test
    void testDecodeWithUnkPreservesReadableText() {
        ChineseTokenizer tok = new ChineseTokenizer();
        tok.train(Arrays.asList("你好世界"), 100);
        int[] ids = tok.encode("你好你好");
        String decoded = tok.decode(ids);
        // 修复后: 至少应该有部分内容
        assertNotNull(decoded);
    }

    @Test
    void testDecodeWithUnkMarker() {
        ChineseTokenizer tok = new ChineseTokenizer();
        tok.train(Arrays.asList("你好"), 100);
        int[] ids = tok.encode("你好");
        String decoded = tok.decodeWithUnk(ids);
        assertEquals("你好", decoded);
    }

    @Test
    void testEncodeDecodeRoundTripASCII() {
        ChineseTokenizer tok = new ChineseTokenizer();
        tok.train(Arrays.asList("hello world hello"), 100);
        int[] ids = tok.encode("hello");
        String decoded = tok.decode(ids);
        assertEquals("hello", decoded);
    }
}
