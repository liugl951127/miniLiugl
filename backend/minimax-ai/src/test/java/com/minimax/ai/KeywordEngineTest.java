package com.minimax.ai;

import com.minimax.ai.generation.KeywordEngine;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KeywordEngine 测试 (V2.7)
 */
class KeywordEngineTest {

    private final KeywordEngine engine = new KeywordEngine(null);

    @Test
    void testRecognizeChart() {
        assertEquals(KeywordEngine.Intent.GENERATE_CHART, engine.recognize("给我画一个柱状图"));
        assertEquals(KeywordEngine.Intent.GENERATE_CHART, engine.recognize("show me a bar chart"));
    }

    @Test
    void testRecognizeMusic() {
        assertEquals(KeywordEngine.Intent.GENERATE_MUSIC, engine.recognize("生成一段旋律"));
        assertEquals(KeywordEngine.Intent.GENERATE_MUSIC, engine.recognize("compose a melody"));
    }

    @Test
    void testRecognizeCode() {
        assertEquals(KeywordEngine.Intent.GENERATE_CODE, engine.recognize("生成一个 Spring Boot 项目"));
    }

    @Test
    void testRecognizeQuery() {
        assertEquals(KeywordEngine.Intent.QUERY_DATA, engine.recognize("查询 user 表前 10 条"));
    }

    @Test
    void testRecognizeTransfer() {
        assertEquals(KeywordEngine.Intent.TRANSFER_HUMAN, engine.recognize("转人工"));
        assertEquals(KeywordEngine.Intent.TRANSFER_HUMAN, engine.recognize("我要找真人"));
    }

    @Test
    void testDefaultChat() {
        assertEquals(KeywordEngine.Intent.CHAT, engine.recognize("今天天气怎么样"));
    }

    @Test
    void testUnknown() {
        // null / empty 文本返回 UNKNOWN (路由层降级为 chat)
        assertEquals(KeywordEngine.Intent.UNKNOWN, engine.recognize(""));
        assertEquals(KeywordEngine.Intent.UNKNOWN, engine.recognize(null));
    }

    @Test
    void testExtractKey() {
        Map<String, String> params = engine.extractParams("C 大调 120bpm 8 小节");
        assertEquals("C 大调", params.get("key"));
        assertEquals("120", params.get("bpm"));
        assertEquals("8", params.get("bars"));
    }

    @Test
    void testExtractChartType() {
        Map<String, String> params = engine.extractParams("画一个饼图");
        assertEquals("饼图", params.get("chartType"));
    }

    @Test
    void testExtractTable() {
        Map<String, String> params = engine.extractParams("查询 user 表");
        assertEquals("user", params.get("table"));
    }

    @Test
    void testRoute() {
        KeywordEngine.RouteResult r = engine.route("生成柱状图 user 表的销量", null);
        assertNotNull(r);
        assertEquals(KeywordEngine.Intent.GENERATE_CHART, r.intent);
    }
}
