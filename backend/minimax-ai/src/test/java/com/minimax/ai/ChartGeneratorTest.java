package com.minimax.ai;

import com.minimax.ai.generation.ChartGenerator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChartGenerator 测试 (V2.7)
 */
class ChartGeneratorTest {

    private final ChartGenerator gen = new ChartGenerator();

    @Test
    void testBarChart() {
        ChartGenerator.ChartData data = ChartGenerator.ChartData.builder()
                .type(ChartGenerator.ChartType.BAR)
                .title("销量")
                .categories(Arrays.asList("Q1", "Q2", "Q3", "Q4"))
                .addSeries("2024", Arrays.asList(100.0, 200.0, 150.0, 300.0))
                .build();
        byte[] png = gen.render(data);
        assertNotNull(png);
        assertTrue(png.length > 100);
        // PNG magic
        assertEquals((byte) 0x89, png[0]);
        assertEquals((byte) 'P', png[1]);
    }

    @Test
    void testLineChart() {
        ChartGenerator.ChartData data = ChartGenerator.ChartData.builder()
                .type(ChartGenerator.ChartType.LINE)
                .categories(Arrays.asList("1月", "2月", "3月"))
                .addSeries("A", Arrays.asList(10.0, 20.0, 15.0))
                .addSeries("B", Arrays.asList(5.0, 25.0, 30.0))
                .build();
        byte[] png = gen.render(data);
        assertNotNull(png);
        assertTrue(png.length > 100);
    }

    @Test
    void testPieChart() {
        ChartGenerator.ChartData data = ChartGenerator.ChartData.builder()
                .type(ChartGenerator.ChartType.PIE)
                .categories(Arrays.asList("A", "B", "C"))
                .addSeries("占比", Arrays.asList(40.0, 30.0, 30.0))
                .build();
        byte[] png = gen.render(data);
        assertNotNull(png);
        assertTrue(png.length > 100);
    }

    @Test
    void testRadarChart() {
        ChartGenerator.ChartData data = ChartGenerator.ChartData.builder()
                .type(ChartGenerator.ChartType.RADAR)
                .categories(Arrays.asList("性能", "易用", "稳定", "安全", "扩展"))
                .addSeries("V1", Arrays.asList(8.0, 9.0, 7.0, 8.0, 6.0))
                .build();
        byte[] png = gen.render(data);
        assertNotNull(png);
    }

    @Test
    void testHeatmap() {
        double[][] matrix = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        ChartGenerator.ChartData data = ChartGenerator.ChartData.builder()
                .type(ChartGenerator.ChartType.HEATMAP)
                .heatmap(matrix, new String[]{"行1", "行2", "行3"}, new String[]{"列1", "列2", "列3"})
                .build();
        byte[] png = gen.render(data);
        assertNotNull(png);
    }

    @Test
    void testSankey() {
        ChartGenerator.ChartData data = ChartGenerator.ChartData.builder()
                .type(ChartGenerator.ChartType.SANKEY)
                .flows(Arrays.asList(
                        new ChartGenerator.SankeyFlow("源A", "目标1", 10.0),
                        new ChartGenerator.SankeyFlow("源A", "目标2", 20.0),
                        new ChartGenerator.SankeyFlow("源B", "目标1", 15.0)))
                .build();
        byte[] png = gen.render(data);
        assertNotNull(png);
    }

    @Test
    void testScatter() {
        List<ChartGenerator.Point> points = Arrays.asList(
                new ChartGenerator.Point(1, 2), new ChartGenerator.Point(2, 4), new ChartGenerator.Point(3, 6));
        ChartGenerator.ChartData data = ChartGenerator.ChartData.builder()
                .type(ChartGenerator.ChartType.SCATTER)
                .points(points)
                .build();
        byte[] png = gen.render(data);
        assertNotNull(png);
    }

    @Test
    void testCustomSize() {
        ChartGenerator.ChartData data = ChartGenerator.ChartData.builder()
                .type(ChartGenerator.ChartType.BAR)
                .size(400, 300)
                .categories(Arrays.asList("A", "B"))
                .addSeries("V", Arrays.asList(1.0, 2.0))
                .build();
        byte[] png = gen.render(data);
        assertNotNull(png);
    }
}
