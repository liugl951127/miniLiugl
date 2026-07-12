package com.minimax.ai;

import com.minimax.ai.modelmarket.ModelEntry;
import com.minimax.ai.modelmarket.ModelMarketMapper;
import com.minimax.ai.modelmarket.ModelMarketService;
import com.minimax.ai.modelmarket.ModelRating;
import com.minimax.ai.modelmarket.ModelRatingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * V2.9.1 AI 模型市场测试
 */
class V291ModelMarketTest {

    private ModelMarketService service;
    private ModelMarketMapper marketMapper;
    private ModelRatingMapper ratingMapper;

    @BeforeEach
    void setup() {
        marketMapper = mock(ModelMarketMapper.class);
        ratingMapper = mock(ModelRatingMapper.class);
        service = new ModelMarketService(marketMapper, ratingMapper);
    }

    @Test
    void testUploadFile_Basic() throws Exception {
        when(marketMapper.insert(any())).thenReturn(1);
        byte[] content = "fake model weights".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "file", "model.pt", "application/octet-stream", content);

        Map<String, Object> meta = new HashMap<>();
        meta.put("description", "test model");
        meta.put("modelType", "PYTORCH");
        meta.put("taskType", "TEXT_CLASSIFICATION");
        meta.put("baseModel", "bert-base");
        meta.put("license", "MIT");

        ModelEntry entry = service.upload(file, "test-model", meta, 1L, "alice");
        assertNotNull(entry);
        assertNotNull(entry.getModelKey());
        assertEquals("test-model", entry.getName());
        assertEquals("PYTORCH", entry.getModelType());
        assertEquals("DRAFT", entry.getStatus());
        assertEquals(1L, entry.getAuthorId());
    }

    @Test
    void testUploadFile_Empty() {
        MockMultipartFile empty = new MockMultipartFile("file", new byte[0]);
        assertThrows(Exception.class, () -> {
            service.upload(empty, "test", new HashMap<>(), 1L, "alice");
        });
    }

    @Test
    void testUploadFile_EmptyName() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "m.pt", "application/octet-stream", "x".getBytes());
        assertThrows(IllegalArgumentException.class, () -> {
            service.upload(file, "", new HashMap<>(), 1L, "alice");
        });
    }

    @Test
    void testUploadMetadata() {
        when(marketMapper.insert(any())).thenReturn(1);
        ModelEntry entry = service.uploadMetadata(
            "metadata model", "d", "GGUF", "TEXT_GENERATION",
            "minimax-7b", "1.0.0", "APACHE_2_0",
            1L, "alice", "LLM,7B");
        assertNotNull(entry);
        assertEquals("PUBLISHED", entry.getStatus());  // 元数据直接发布
        assertNotNull(entry.getPublishedAt());
        assertEquals("GGUF", entry.getModelType());
    }

    @Test
    void testBrowse() {
        List<ModelEntry> entries = Arrays.asList(
            mkEntry("m-1", "A", "PYTORCH", 4.5, 100, "PUBLISHED"),
            mkEntry("m-2", "B", "GGUF", 4.0, 50, "PUBLISHED")
        );
        when(marketMapper.selectList(any())).thenReturn(entries);

        List<ModelEntry> result = service.browse(null, null, null, "rating", 50);
        assertEquals(2, result.size());
    }

    @Test
    void testRate_New() {
        ModelEntry entry = mkEntry("m-1", "A", "PYTORCH", 0, 0, "PUBLISHED");
        when(marketMapper.selectOne(any())).thenReturn(entry);
        when(ratingMapper.findUserRating(any(), any())).thenReturn(null);
        when(ratingMapper.insert(any())).thenReturn(1);
        when(marketMapper.updateRatingStats(any())).thenReturn(1);

        service.rate("m-1", 1L, "alice", 5, "good");
        org.mockito.Mockito.verify(ratingMapper).insert(any());
    }

    @Test
    void testRate_Update() {
        ModelEntry entry = mkEntry("m-1", "A", "PYTORCH", 4.0, 5, "PUBLISHED");
        ModelRating existing = ModelRating.builder()
            .modelKey("m-1").userId(1L).rating(3).build();
        when(marketMapper.selectOne(any())).thenReturn(entry);
        when(ratingMapper.findUserRating(any(), any())).thenReturn(existing);
        when(ratingMapper.updateById(any())).thenReturn(1);
        when(marketMapper.updateRatingStats(any())).thenReturn(1);

        service.rate("m-1", 1L, "alice", 5, "better");
        org.mockito.Mockito.verify(ratingMapper).updateById(any());
    }

    @Test
    void testRate_InvalidScore() {
        ModelEntry entry = mkEntry("m-1", "A", "PYTORCH", 0, 0, "PUBLISHED");
        when(marketMapper.selectOne(any())).thenReturn(entry);
        assertThrows(IllegalArgumentException.class, () -> service.rate("m-1", 1L, "a", 6, ""));
        assertThrows(IllegalArgumentException.class, () -> service.rate("m-1", 1L, "a", 0, ""));
    }

    @Test
    void testRate_NotFound() {
        when(marketMapper.selectOne(any())).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () ->
            service.rate("nope", 1L, "a", 5, ""));
    }

    @Test
    void testRecordDownload() {
        when(marketMapper.incrementDownload(any())).thenReturn(1);
        service.recordDownload("m-1");
        org.mockito.Mockito.verify(marketMapper).incrementDownload("m-1");
    }

    @Test
    void testChangeStatus() {
        ModelEntry entry = mkEntry("m-1", "A", "PYTORCH", 0, 0, "DRAFT");
        when(marketMapper.selectOne(any())).thenReturn(entry);
        when(marketMapper.updateById(any())).thenReturn(1);

        boolean ok = service.changeStatus("m-1", "PUBLISHED");
        assertTrue(ok);
    }

    @Test
    void testChangeStatus_NotFound() {
        when(marketMapper.selectOne(any())).thenReturn(null);
        boolean ok = service.changeStatus("nope", "PUBLISHED");
        assertFalse(ok);
    }

    @Test
    void testStats() {
        when(marketMapper.selectCount(any())).thenReturn(10L, 8L, 2L);
        when(marketMapper.selectList(any())).thenReturn(Arrays.asList(
            mkEntry("m-1", "A", "PYTORCH", 4.0, 100, "PUBLISHED"),
            mkEntry("m-2", "B", "GGUF", 4.5, 50, "PUBLISHED")
        ));

        Map<String, Object> stats = service.stats();
        assertEquals(10L, stats.get("total"));
        assertEquals(8L, stats.get("published"));
        assertEquals(150L, stats.get("totalDownloads"));
        assertNotNull(stats.get("typeDistribution"));
    }

    private ModelEntry mkEntry(String key, String name, String type, double rating, long downloads, String status) {
        return ModelEntry.builder()
            .id(1L).modelKey(key).name(name).description("d").modelType(type)
            .taskType("OTHER").baseModel("").version("1.0.0")
            .filePath("").fileName("").fileSize(0L).sha256("")
            .license("MIT").authorId(1L).authorName("alice")
            .tags("").metricsJson("{}").status(status)
            .downloadCount(downloads).avgRating(rating).ratingCount(5L)
            .build();
    }
}
