package com.minimax.analytics;

import com.minimax.analytics.service.report.TrendAnalyzer;
import com.minimax.analytics.service.report.TrendAnalyzer.TrendPoint;
import com.minimax.analytics.vo.QueryResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 趋势分析测试 (V5.31)
 */
class TrendAnalyzerTest {

    private final TrendAnalyzer analyzer = new TrendAnalyzer();

    @Test void movingAverage() {
        QueryResult qr = QueryResult.builder()
                .columns(List.of("day", "v"))
                .rows(List.of(
                        Map.of("day", "1", "v", 10),
                        Map.of("day", "2", "v", 20),
                        Map.of("day", "3", "v", 30),
                        Map.of("day", "4", "v", 40)
                ))
                .rowCount(4L)
                .build();
        List<TrendPoint> pts = analyzer.analyze(qr, "day", "v");
        assertEquals(4, pts.size());
        assertEquals(10.0, pts.get(0).ma3, 0.01);
        assertEquals(20.0, pts.get(1).ma3, 0.01);
        assertEquals(20.0, pts.get(2).ma3, 0.01);  // (10+20+30)/3
        assertEquals(30.0, pts.get(3).ma3, 0.01);  // (20+30+40)/3
    }

    @Test void qoqCalculation() {
        QueryResult qr = QueryResult.builder()
                .columns(List.of("d", "v"))
                .rows(List.of(
                        Map.of("d", "1", "v", 100),
                        Map.of("d", "2", "v", 200)
                ))
                .rowCount(2L)
                .build();
        List<TrendPoint> pts = analyzer.analyze(qr, "d", "v");
        // pts[1].qoq = (200-100)/100 * 100 = 100%
        assertNotNull(pts.get(1).qoq);
        assertEquals(100.0, pts.get(1).qoq, 0.01);
    }

    @Test void handleEmpty() {
        QueryResult qr = QueryResult.builder().columns(List.of()).rows(List.of()).build();
        List<TrendPoint> pts = analyzer.analyze(qr, "x", "y");
        assertTrue(pts.isEmpty());
    }

    @Test void handleNullValue() {
        QueryResult qr = QueryResult.builder()
                .columns(List.of("d", "v"))
                .rows(List.of(
                        Map.of("d", "1", "v", 10),
                        hashWith("d", "2", "v", null)
                ))
                .rowCount(2L)
                .build();
        List<TrendPoint> pts = analyzer.analyze(qr, "d", "v");
        assertEquals(2, pts.size());
        // pts[1].raw = 0.0 (null 解析为 0)
        assertEquals(0.0, pts.get(1).raw, 0.01);
    }

    /** 允许 null value 的 HashMap (Map.of 不支持 null) */
    private static Map<String, Object> hashWith(String k1, String v1, String k2, Object v2) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    @Test void yoyAtLeast12Points() {
        QueryResult qr = QueryResult.builder()
                .columns(List.of("d", "v"))
                .rows(new ArrayList<>())
                .build();
        for (int i = 1; i <= 13; i++) {
            ((List<Map<String, Object>>) qr.getRows()).add(Map.of("d", String.valueOf(i), "v", 100 + i));
        }
        qr.setRowCount(13L);
        List<TrendPoint> pts = analyzer.analyze(qr, "d", "v");
        assertNotNull(pts.get(12).yoy, "第 13 个点应该有 yoy");
    }

    @Test void yoyMissingIfNotEnoughPoints() {
        QueryResult qr = QueryResult.builder()
                .columns(List.of("d", "v"))
                .rows(List.of(
                        Map.of("d", "1", "v", 100),
                        Map.of("d", "2", "v", 200)
                ))
                .rowCount(2L)
                .build();
        List<TrendPoint> pts = analyzer.analyze(qr, "d", "v");
        assertNull(pts.get(1).yoy);
    }
}
