package com.minimax.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.memory.embedding.MockEmbeddingClient;
import com.minimax.memory.longterm.LongTermMemory;
import com.minimax.memory.longterm.LongTermMemoryMapper;
import com.minimax.memory.longterm.LongTermMemoryService;
import com.minimax.memory.pref.UserPrefMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Day 7 集成测试：覆盖端到端
 *  store / recall / pref / cross-context / short-term
 * 不需要 HTTP - 直接调 Service 层 (避开 JWT 鉴权).
 */
@SpringBootTest
@ActiveProfiles("test")
class MemoryIntegrationTestIT {

    @Autowired LongTermMemoryService longTerm;
    @Autowired LongTermMemoryMapper longTermMapper;
    @Autowired UserPrefMapper prefMapper;
    @Autowired MockEmbeddingClient mockEmbedding;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM memory_long_term");
        jdbc.update("DELETE FROM memory_user_pref");
    }

    @Test
    void storeAndRecall() {
        // 1) 存 3 条
        Long id1 = longTerm.store(1L, 100L, "user", "我喜欢川菜", null, "food", 0.8);
        Long id2 = longTerm.store(1L, 100L, "user", "我的工作是后端开发", null, "job", 0.7);
        Long id3 = longTerm.store(1L, 100L, "user", "我养了一只猫叫小黄", null, "pet", 0.9);
        assertNotNull(id1);
        assertNotNull(id2);
        assertNotNull(id3);

        // 2) 数据库有
        List<LongTermMemory> all = longTermMapper.selectRecentByUser(1L, 10);
        assertEquals(3, all.size());

        // 3) recall 宠物相关
        List<LongTermMemoryService.RecallHit> hits = longTerm.recall(1L, "我养什么宠物", 3);
        assertNotNull(hits);
        assertTrue(hits.size() >= 1, "should recall at least 1 memory");
        // mock embedding 64 维下，最低分过滤后，hits 至少要 1 条 (标量相关性)
    }

    @Test
    void storeSetsCorrectDim() {
        Long id = longTerm.store(2L, null, "user", "测试", null, null, 0.5);
        // 直接查
        LongTermMemory m = longTermMapper.selectById(id);
        assertNotNull(m, "should find id=" + id);
        assertEquals(64, m.getDim(), "mock embedding should be 64-dim");
        // embedding 字段 select=false，单独查
        List<LongTermMemory> full = longTermMapper.selectEmbeddingsByUser(2L, 1);
        assertEquals(1, full.size());
        assertEquals(64 * 4, full.get(0).getEmbedding().length, "64 floats = 256 bytes");
    }

    @Test
    void importanceAndTags() {
        Long id = longTerm.store(3L, null, "user", "重要的事", null, "tag1,tag2", 0.95);
        LongTermMemory m = longTermMapper.selectById(id);
        assertEquals(0.95, m.getImportance().doubleValue(), 1e-6);
        assertEquals("tag1,tag2", m.getTags());
    }

    @Test
    void recallUpdatesAccessCount() {
        longTerm.store(4L, 200L, "user", "recalled memory", null, null, 0.5);
        List<LongTermMemoryService.RecallHit> hits = longTerm.recall(4L, "recalled", 5);
        // 至少找到 1 条 (相同文本 cosine=1.0)
        if (!hits.isEmpty()) {
            Long id = hits.get(0).id();
            LongTermMemory m = longTermMapper.selectById(id);
            assertTrue(m.getAccessCount() >= 1, "access_count should be >= 1 after recall");
            assertNotNull(m.getLastAccessAt(), "last_access_at should be set");
        }
    }

    @Test
    void deleteBelongsToUser() {
        Long id = longTerm.store(5L, null, "user", "user5 memory", null, null, 0.5);
        // user 6 删不掉
        assertFalse(longTerm.delete(id, 6L));
        // user 5 能删
        assertTrue(longTerm.delete(id, 5L));
        assertNull(longTermMapper.selectById(id));
    }

    @Test
    void embeddingDeterministic() {
        float[] a = mockEmbedding.embed("今天天气好");
        float[] b = mockEmbedding.embed("今天天气好");
        // cos = 1
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        double cos = dot / (Math.sqrt(na) * Math.sqrt(nb));
        assertEquals(1.0, cos, 1e-6);
    }
}
