package com.minimax.memory;

import com.minimax.memory.context.ContextBuilder;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContextBuilderTest {

    @Test
    void approxTokensCountsAscii() {
        // 4 ascii chars = 1 token
        assertEquals(1, ContextBuilder.approxTokens("abcd"));
        // 8 ascii chars = 2 tokens
        assertEquals(2, ContextBuilder.approxTokens("abcdefgh"));
    }

    @Test
    void approxTokensCountsCjk() {
        // 4 CJK chars = 4 tokens
        assertEquals(4, ContextBuilder.approxTokens("你好世界"));
    }

    @Test
    void approxTokensMixed() {
        // "hi 你好" = 2 ascii + 2 cjk = 2/4 + 2 = 0 + 2 = 2 (ceil)
        int t = ContextBuilder.approxTokens("hi 你好");
        assertTrue(t >= 2 && t <= 5, "mixed approx should be reasonable, got " + t);
    }

    @Test
    void approxTokensNullEmpty() {
        assertEquals(0, ContextBuilder.approxTokens(null));
        assertEquals(0, ContextBuilder.approxTokens(""));
    }
}
