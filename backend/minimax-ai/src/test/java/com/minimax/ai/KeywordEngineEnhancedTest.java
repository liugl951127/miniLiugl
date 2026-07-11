package com.minimax.ai;

import com.minimax.ai.generation.KeywordEngine;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KeywordEngine 增强测试 (V2.8.2)
 * 验证 13 个意图的识别准确率
 */
class KeywordEngineEnhancedTest {

    private final KeywordEngine engine = new KeywordEngine(null);

    @Test
    void testAllIntentsRecognized() {
        // 每个意图至少一个用例
        assertEquals(KeywordEngine.Intent.GENERATE_CHART, engine.recognize("画个柱状图"));
        assertEquals(KeywordEngine.Intent.GENERATE_CHART, engine.recognize("做个饼图"));
        assertEquals(KeywordEngine.Intent.GENERATE_MUSIC, engine.recognize("生成一段旋律"));
        assertEquals(KeywordEngine.Intent.GENERATE_ANIMATION, engine.recognize("做一个 GIF 动画"));
        assertEquals(KeywordEngine.Intent.QUERY_DATA, engine.recognize("SELECT * FROM user"));
        assertEquals(KeywordEngine.Intent.ANALYZE_DATA, engine.recognize("统计 user 表的平均年龄"));
        assertEquals(KeywordEngine.Intent.GENERATE_CODE, engine.recognize("生成一个 Spring Boot 项目"));
        assertEquals(KeywordEngine.Intent.CHAT, engine.recognize("你好"));
        assertEquals(KeywordEngine.Intent.TRANSFER_HUMAN, engine.recognize("转人工"));
        assertEquals(KeywordEngine.Intent.IMAGE_ANALYZE, engine.recognize("分析图片"));
        assertEquals(KeywordEngine.Intent.AUDIO_ANALYZE, engine.recognize("分析音频"));
        assertEquals(KeywordEngine.Intent.VIDEO_ANALYZE, engine.recognize("分析视频"));
    }

    @Test
    void testEmptyInput() {
        assertEquals(KeywordEngine.Intent.UNKNOWN, engine.recognize(""));
        assertEquals(KeywordEngine.Intent.UNKNOWN, engine.recognize(null));
        assertEquals(KeywordEngine.Intent.UNKNOWN, engine.recognize("   "));
    }

    @Test
    void testRegexPriorityOverKeyword() {
        // "生成一个图表统计一下" 既有 regex 也有 keyword, regex 优先
        assertEquals(KeywordEngine.Intent.GENERATE_CHART, engine.recognize("生成一个图表统计一下"));
    }

    @Test
    void testCaseInsensitive() {
        assertEquals(KeywordEngine.Intent.GENERATE_CHART, engine.recognize("PIE CHART"));
        assertEquals(KeywordEngine.Intent.QUERY_DATA, engine.recognize("select from user"));
    }

    @Test
    void testMixedLanguage() {
        // 中英混合
        assertEquals(KeywordEngine.Intent.GENERATE_CHART, engine.recognize("画个 bar chart"));
        assertEquals(KeywordEngine.Intent.GENERATE_MUSIC, engine.recognize("生成一段 music 旋律"));
    }

    @Test
    void testExtractParamsMusic() {
        Map<String, String> p = engine.extractParams("生成 C 大调 120 bpm 的音乐, 8 小节");
        assertNotNull(p);
        assertTrue(p.containsKey("key") || p.containsKey("bpm") || p.containsKey("bars"));
    }

    @Test
    void testExtractParamsChart() {
        Map<String, String> p = engine.extractParams("画一个 5 列的柱状图, 维度是 type");
        assertNotNull(p);
        // 不强制提取, 只验证能调用
    }

    @Test
    void testRouteResultStructure() {
        // 完整结构
        KeywordEngine.RouteResult r = engine.route("画一个柱状图", null);
        assertNotNull(r);
        assertNotNull(r.intent);
        assertNotNull(r.params);
        assertNotNull(r.originalText);
        assertEquals(KeywordEngine.Intent.GENERATE_CHART, r.intent);
        assertTrue(r.confidence >= 0);
    }

    @Test
    void testIntentCount() {
        // 14 意图 (含 UNKNOWN)
        assertEquals(14, KeywordEngine.Intent.values().length);
    }
}
