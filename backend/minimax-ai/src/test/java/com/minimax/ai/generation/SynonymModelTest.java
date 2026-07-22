package com.minimax.ai.generation;

import com.minimax.ai.generation.model.SynonymModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SynonymModel 同义词扩展测试
 */
@DisplayName("SynonymModel 同义词测试")
class SynonymModelTest {

    private SynonymModel model;

    @BeforeEach
    void setUp() {
        model = new SynonymModel();
    }

    @Test
    @DisplayName("扩展: '搞个' → 含 '生成'/'做个'/'画一个'")
    void testExpandVerb() {
        Set<String> expansions = model.expand("搞个统计图");
        assertTrue(expansions.contains("搞个统计图"));  // 原 query
        // 至少应该包含 "生成统计图" "做个统计图" 等
        boolean hasSynonym = expansions.stream().anyMatch(s -> s.contains("生成") || s.contains("做个"));
        assertTrue(hasSynonym, "Should contain synonym expansion");
    }

    @Test
    @DisplayName("扩展: '图表' 同义有 'chart'/'plot'")
    void testExpandChart() {
        Set<String> expansions = model.expand("生成图表");
        assertTrue(expansions.stream().anyMatch(s -> s.contains("chart") || s.contains("plot")));
    }

    @Test
    @DisplayName("扩展: 已知词的同义")
    void testGetSynonyms() {
        Set<String> syns = model.getSynonyms("生成");
        assertTrue(syns.contains("生成"));
        assertTrue(syns.contains("搞个"));
        assertTrue(syns.contains("create"));
    }

    @Test
    @DisplayName("扩展: 未知词不报错")
    void testUnknownWord() {
        Set<String> syns = model.getSynonyms("xyz_unknown");
        assertEquals(1, syns.size());  // 自身
    }

    @Test
    @DisplayName("性能: 1000 次扩展 < 100ms")
    void testPerformance() {
        // 预热
        for (int i = 0; i < 10; i++) model.expand("搞个统计图");

        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            model.expand("搞个统计图");
        }
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertTrue(ms < 100, "Should be fast: " + ms + "ms");
        System.out.printf("[synonym-perf] 1000 expand: %dms%n", ms);
    }
}
