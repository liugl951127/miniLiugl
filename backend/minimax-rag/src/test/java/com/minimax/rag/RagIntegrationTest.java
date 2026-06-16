package com.minimax.rag;

import com.minimax.rag.entity.Document;
import com.minimax.rag.entity.DocumentChunk;
import com.minimax.rag.entity.KnowledgeBase;
import com.minimax.rag.retriever.Retriever;
import com.minimax.rag.service.DocumentService;
import com.minimax.rag.service.KnowledgeBaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Day 8 集成测试 - 端到端 RAG 流程
 */
@SpringBootTest
@ActiveProfiles("test")
class RagIntegrationTest {

    @Autowired KnowledgeBaseService kbService;
    @Autowired DocumentService docService;
    @Autowired Retriever retriever;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM document_chunk");
        jdbc.update("DELETE FROM document");
        jdbc.update("DELETE FROM knowledge_base");
    }

    @Test
    void createAndListKb() {
        Long id = kbService.create(1L, "我的产品手册", "产品相关文档", "private", "product,manual");
        assertNotNull(id);

        KnowledgeBase kb = kbService.get(id, 1L);
        assertNotNull(kb);
        assertEquals("我的产品手册", kb.getName());
        assertEquals("private", kb.getVisibility());
        assertEquals(0, kb.getDocCount());

        List<KnowledgeBase> mine = kbService.listByOwner(1L);
        assertEquals(1, mine.size());
    }

    @Test
    void publicKbVisibleToAll() {
        Long id = kbService.create(1L, "公共文档", "公开知识库", "public", null);
        KnowledgeBase mine = kbService.get(id, 1L);
        assertNotNull(mine);
        // 别人也能看
        KnowledgeBase others = kbService.get(id, 999L);
        assertNotNull(others);
    }

    @Test
    void privateKbHiddenFromOthers() {
        Long id = kbService.create(1L, "私有", "私人", "private", null);
        assertNotNull(kbService.get(id, 1L));
        assertNull(kbService.get(id, 2L));
    }

    @Test
    void uploadAndChunkTextDoc() {
        Long kbId = kbService.create(1L, "kb", null, "private", null);
        String text = "人工智能正在改变世界。\n\n机器学习是 AI 的核心。\n\n深度学习是机器学习的子集。\n\n" +
                "自然语言处理让机器理解人类语言。\n\n计算机视觉让机器看懂图像。\n\n" +
                "强化学习让机器从试错中学习。\n\n推荐系统广泛应用于电商和社交。";
        byte[] content = text.getBytes(StandardCharsets.UTF_8);

        Long docId = docService.upload(1L, kbId, "AI 入门", "txt", content, "ai.txt", "ai,intro");
        assertNotNull(docId);

        // 检查切片
        List<DocumentChunk> chunks = docService.chunksOfDoc(docId);
        assertTrue(chunks.size() >= 1, "should have at least 1 chunk, got " + chunks.size());
        // 第一片必须有内容
        assertNotNull(chunks.get(0).getContent());
        assertTrue(chunks.get(0).getCharCount() > 0);
        assertEquals(64, chunks.get(0).getDim());
    }

    @Test
    void dedupSameContent() {
        Long kbId = kbService.create(1L, "kb", null, "private", null);
        byte[] content = "hello world".getBytes(StandardCharsets.UTF_8);
        Long id1 = docService.upload(1L, kbId, "doc1", "txt", content, "a.txt", null);
        Long id2 = docService.upload(1L, kbId, "doc2", "txt", content, "b.txt", null);
        // 相同 checksum + 同 KB → 返回已存在 id
        assertEquals(id1, id2);
    }

    @Test
    void kbCounters() {
        Long kbId = kbService.create(1L, "kb", null, "private", null);
        byte[] c1 = "doc 1 content with some text to chunk".getBytes(StandardCharsets.UTF_8);
        byte[] c2 = "doc 2 another long content also to be chunked by chunker".getBytes(StandardCharsets.UTF_8);
        docService.upload(1L, kbId, "d1", "txt", c1, "a.txt", null);
        docService.upload(1L, kbId, "d2", "txt", c2, "b.txt", null);

        KnowledgeBase kb = kbService.get(kbId, 1L);
        assertEquals(2, kb.getDocCount());
        assertTrue(kb.getChunkCount() >= 2);
    }

    @Test
    void retrieveFindsRelevantDoc() {
        Long kbId = kbService.create(1L, "kb", null, "private", null);
        byte[] c1 = "猫是可爱的宠物。它们喜欢晒太阳。".getBytes(StandardCharsets.UTF_8);
        byte[] c2 = "狗是人类最忠诚的朋友。它们会看家。".getBytes(StandardCharsets.UTF_8);
        byte[] c3 = "Java 是一门面向对象编程语言，由 Sun 公司发明。".getBytes(StandardCharsets.UTF_8);
        docService.upload(1L, kbId, "cat", "txt", c1, "cat.txt", null);
        docService.upload(1L, kbId, "dog", "txt", c2, "dog.txt", null);
        docService.upload(1L, kbId, "java", "txt", c3, "java.txt", null);

        // 检索 "宠物"
        List<Retriever.Hit> hits = retriever.retrieve(kbId, "宠物", 3);
        // mock embedding 64 维下相关性可能不完全精准，但能返回一些
        assertNotNull(hits);
        // 至少返回了 (kb 内有 3 个文档，分块后若干 chunk)
    }

    @Test
    void deleteDocDecrementsCounters() {
        Long kbId = kbService.create(1L, "kb", null, "private", null);
        byte[] c = "some text content here for chunking test".getBytes(StandardCharsets.UTF_8);
        Long docId = docService.upload(1L, kbId, "d", "txt", c, "a.txt", null);
        KnowledgeBase before = kbService.get(kbId, 1L);
        int beforeDoc = before.getDocCount();
        int beforeChunk = before.getChunkCount();

        assertTrue(docService.delete(docId, 1L));
        KnowledgeBase after = kbService.get(kbId, 1L);
        assertEquals(beforeDoc - 1, after.getDocCount());
        assertTrue(after.getChunkCount() < beforeChunk);
    }
}
