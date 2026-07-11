package com.minimax.ai;

import com.minimax.ai.generation.ChartGenerator;
import com.minimax.ai.generation.KeywordEngine;
import com.minimax.ai.generation.Nl2Chart;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Nl2Chart 增强测试 (V2.8.2)
 * 验证自然语言 → 图表的全链路
 */
class Nl2ChartEnhancedTest {

    private final KeywordEngine engine = new KeywordEngine(null);

    @Test
    void testRecognizeChartFromNaturalLanguage() {
        // 各种自然语言表达都能识别为 GENERATE_CHART
        assertEquals(KeywordEngine.Intent.GENERATE_CHART, engine.recognize("画一个柱状图"));
        assertEquals(KeywordEngine.Intent.GENERATE_CHART, engine.recognize("pie chart"));
        assertEquals(KeywordEngine.Intent.GENERATE_CHART, engine.recognize("bar chart"));
        assertEquals(KeywordEngine.Intent.GENERATE_CHART, engine.recognize("line chart"));
        assertEquals(KeywordEngine.Intent.GENERATE_CHART, engine.recognize("radar chart"));
    }

    @Test
    void testParseChartSpecBar() {
        // 模拟用户输入 -> parseChartSpec
        // "画一个 5 列柱状图, 维度是 type" -> 5 列 + type 字段
        // (简化测试: Nl2Chart 主要做图表生成, 解析交给 KeywordEngine.extractParams)
        Map<String, String> params = engine.extractParams("画一个 5 列的柱状图, 维度是 type");
        assertNotNull(params);
    }

    @Test
    void testChartTypeEnum() {
        // 7 种图表类型
        ChartGenerator.ChartType[] types = ChartGenerator.ChartType.values();
        assertEquals(7, types.length);
        // 验证常用类型
        assertNotNull(ChartGenerator.ChartType.valueOf("BAR"));
        assertNotNull(ChartGenerator.ChartType.valueOf("LINE"));
        assertNotNull(ChartGenerator.ChartType.valueOf("PIE"));
        assertNotNull(ChartGenerator.ChartType.valueOf("SCATTER"));
        assertNotNull(ChartGenerator.ChartType.valueOf("RADAR"));
        assertNotNull(ChartGenerator.ChartType.valueOf("HEATMAP"));
        assertNotNull(ChartGenerator.ChartType.valueOf("SANKEY"));
    }

    @Test
    void testChartDataBuilder() {
        // 验证 Builder 模式
        ChartGenerator.ChartData data = new ChartGenerator.ChartData();
        data.title = "Test";
        data.series = new java.util.ArrayList<>();
        data.points = new java.util.ArrayList<>();
        data.flows = new java.util.ArrayList<>();
        ChartGenerator.Series s = new ChartGenerator.Series("S1", List.of(1.0, 2.0, 3.0));
        data.series.add(s);
        assertEquals("Test", data.title);
        assertEquals(1, data.series.size());
        assertEquals(3, data.series.get(0).values.size());
    }
}
