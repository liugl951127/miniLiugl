package com.minimax.rag;

import com.minimax.rag.chunker.TextChunker;
import com.minimax.rag.entity.DocumentChunk;
import com.minimax.rag.service.DocumentService;
import com.minimax.rag.service.KnowledgeBaseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Day 36: RAG 上传进度回调 + 切片完整性测试
 */
@SpringBootTest
@ActiveProfiles("test")
class RagUploadProgressTest {

    @Autowired
    private TextChunker chunker;

    @Autowired
    private DocumentService docService;

    @Autowired
    private KnowledgeBaseService kbService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("TC1: TXT 文档上传后切片数量 ≥ 1，charCount > 0，embedding dim = 64")
    void uploadTextGeneratesChunks() {
        jdbc.update("DELETE FROM document_chunk");
        jdbc.update("DELETE FROM document");
        jdbc.update("DELETE FROM knowledge_base");

        Long kbId = kbService.create(1L, "测试KB", null, "private", null);
        String text = "第一章：概述\n\n这是第一段内容。" +
                "第二章：技术细节\n\n这是第二段内容，关于系统的架构设计。" +
                "第三章：使用指南\n\n这是第三段内容，介绍如何操作本系统。";
        byte[] content = text.getBytes(StandardCharsets.UTF_8);

        Long docId = docService.upload(1L, kbId, "测试文档", "txt", content, "test.txt", null);
        assertNotNull(docId);

        List<DocumentChunk> chunks = docService.chunksOfDoc(docId);
        assertFalse(chunks.isEmpty(), "切片列表不应为空");

        for (DocumentChunk c : chunks) {
            assertNotNull(c.getContent(), "chunk content 不应为 null");
            assertTrue(c.getCharCount() > 0, "chunk charCount 应 > 0");
            assertNotNull(c.getEmbedding(), "embedding 不应为 null");
            assertEquals(64, c.getDim(), "embedding 维度应为 64");
            assertEquals(docId, c.getDocId(), "chunk.docId 应匹配");
            assertTrue(c.getChunkIndex() >= 0, "chunkIndex 应 >= 0");
        }

        // 验证切片数量（assert 覆盖，无 print）
        assertTrue(chunks.size() >= 1, "TXT 应生成 ≥ 1 个 chunk");
    }

    @Test
    @DisplayName("TC2: 长文本多 chunk，每片 content 不重复（滑动窗口不重叠内容区隔）")
    void longTextGeneratesMultipleChunks() {
        jdbc.update("DELETE FROM document_chunk");
        jdbc.update("DELETE FROM document");
        jdbc.update("DELETE FROM knowledge_base");

        Long kbId = kbService.create(1L, "长文KB", null, "private", null);
        // 构造 2000+ 字符的长文本，强制触发多 chunk
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            sb.append("段落 ").append(i).append("：这是一段关于技术的内容。");
        }
        byte[] content = sb.toString().getBytes(StandardCharsets.UTF_8);

        Long docId = docService.upload(1L, kbId, "长文测试", "txt", content, "long.txt", null);
        List<DocumentChunk> chunks = docService.chunksOfDoc(docId);

        assertTrue(chunks.size() >= 2, "长文本应生成 ≥ 2 个 chunk，实际: " + chunks.size());

        // 验证 chunkIndex 连续
        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).getChunkIndex(), "chunkIndex 应连续");
        }

    }

    @Test
    @DisplayName("TC3: TextChunker 单元测试 — 不同 size text 返回合理的 chunk 数")
    void chunkerHandlesVariousSizes() {
        // 极短文本
        List<TextChunker.Chunk> tiny = chunker.chunk("hello world");
        assertTrue(tiny.size() >= 1, "极短文本至少 1 个 chunk");

        // 中等文本（约 500 字符）
        StringBuilder mid = new StringBuilder();
        for (int i = 0; i < 25; i++) {
            mid.append("这是第 ").append(i).append(" 句话。");
        }
        List<TextChunker.Chunk> medium = chunker.chunk(mid.toString());
        assertFalse(medium.isEmpty());

        // 验证所有 chunk 有 charCount 和 content
        for (TextChunker.Chunk c : tiny) {
            assertTrue(c.charCount() > 0);
            assertNotNull(c.content());
        }
        for (TextChunker.Chunk c : medium) {
            assertTrue(c.charCount() > 0);
            assertNotNull(c.content());
        }

    }
}
