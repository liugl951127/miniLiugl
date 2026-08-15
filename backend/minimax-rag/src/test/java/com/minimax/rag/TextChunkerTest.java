package com.minimax.rag;

import com.minimax.rag.chunker.TextChunker;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkerTest {

    private TextChunker chunker(int size, int overlap) {
        TextChunker c = new TextChunker();
        ReflectionTestUtils.setField(c, "chunkSize", size);
        ReflectionTestUtils.setField(c, "overlap", overlap);
        return c;
    }

    @Test
    void emptyOrNull() {
        TextChunker c = chunker(500, 50);
        assertTrue(c.chunk(null).isEmpty());
        assertTrue(c.chunk("").isEmpty());
        assertTrue(c.chunk("   \n\n  ").isEmpty());
    }

    @Test
    void singleParagraph() {
        TextChunker c = chunker(500, 50);
        String text = "Hello world. This is a test.";
        List<TextChunker.Chunk> chunks = c.chunk(text);
        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0).content());
    }

    @Test
    void multipleParagraphs() {
        TextChunker c = chunker(500, 50);
        String text = "Paragraph 1.\n\nParagraph 2.\n\nParagraph 3.";
        List<TextChunker.Chunk> chunks = c.chunk(text);
        // 3 段都不超 500 → 合并成 1 块
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).content().contains("Paragraph 1"));
        assertTrue(chunks.get(0).content().contains("Paragraph 2"));
        assertTrue(chunks.get(0).content().contains("Paragraph 3"));
    }

    @Test
    void longParagraphForceSplit() {
        TextChunker c = chunker(20, 5);
        String text = "a".repeat(100); // 100 chars single paragraph
        List<TextChunker.Chunk> chunks = c.chunk(text);
        assertTrue(chunks.size() >= 4, "should split into multiple chunks, got " + chunks.size());
        // 每块都不超过 20 + 一点 overlap
        for (TextChunker.Chunk ch : chunks) {
            assertTrue(ch.charCount() <= 25, "chunk too big: " + ch.charCount());
        }
    }

    @Test
    void overlapKeepsContext() {
        TextChunker c = chunker(30, 10);
        String text = "a".repeat(100);
        List<TextChunker.Chunk> chunks = c.chunk(text);
        // 相邻 chunk 应有重叠
        if (chunks.size() >= 2) {
            String c1 = chunks.get(0).content();
            String c2 = chunks.get(1).content();
            // c2 的前 10 字符应来自 c1
            assertTrue(c2.startsWith(c1.substring(c1.length() - 10)),
                    "c2 should start with c1's last 10 chars");
        }
    }

    @Test
    void positionTracking() {
        TextChunker c = chunker(50, 10);
        String text = "Hello world\n\nSecond paragraph here\n\nThird";
        List<TextChunker.Chunk> chunks = c.chunk(text);
        for (TextChunker.Chunk ch : chunks) {
            assertTrue(ch.startPos() >= 0);
            assertTrue(ch.endPos() > ch.startPos());
        }
    }
}
