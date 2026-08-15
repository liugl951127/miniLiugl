package com.minimax.analytics;

import com.minimax.analytics.service.chart.ChartTypeDecider;
import com.minimax.analytics.service.chart.ChartTypeDecider.Decision;
import com.minimax.analytics.vo.QueryResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 图表决策器测试 (V5.31)
 */
class ChartTypeDeciderTest {

    private final ChartTypeDecider decider = new ChartTypeDecider();

    @Test void decideBarForCategoryAndNumber() {
        QueryResult qr = QueryResult.builder()
                .columns(List.of("role", "cnt"))
                .rows(List.of(
                        Map.of("role", "admin", "cnt", 10),
                        Map.of("role", "user", "cnt", 100),
                        Map.of("role", "guest", "cnt", 50)
                ))
                .rowCount(3L)
                .build();
        Decision d = decider.decide(qr);
        assertEquals(ChartTypeDecider.ChartType.BAR, d.type);
        assertEquals("role", d.xField);
        assertEquals("cnt", d.yField);
    }

    @Test void decideLineForTimeSeries() {
        QueryResult qr = QueryResult.builder()
                .columns(List.of("date", "count"))
                .rows(List.of(
                        Map.of("date", "2024-01-01", "count", 10),
                        Map.of("date", "2024-01-02", "count", 20)
                ))
                .rowCount(2L)
                .build();
        Decision d = decider.decide(qr);
        assertEquals(ChartTypeDecider.ChartType.LINE, d.type);
    }

    @Test void decideLineForLargeCategory() {
        QueryResult qr = QueryResult.builder()
                .columns(List.of("k", "v"))
                .rows(new ArrayList<>())
                .build();
        for (int i = 0; i < 20; i++) {
            ((List<Map<String, Object>>) qr.getRows()).add(Map.of("k", "k" + i, "v", i * 10));
        }
        qr.setRowCount(20L);
        Decision d = decider.decide(qr);
        // > 10 行 → LINE
        assertEquals(ChartTypeDecider.ChartType.LINE, d.type);
    }

    @Test void decideMultiNumeric() {
        QueryResult qr = QueryResult.builder()
                .columns(List.of("month", "a", "b"))
                .rows(List.of(
                        Map.of("month", "2024-01", "a", 1, "b", 2),
                        Map.of("month", "2024-02", "a", 3, "b", 4)
                ))
                .rowCount(2L)
                .build();
        Decision d = decider.decide(qr);
        assertEquals(ChartTypeDecider.ChartType.LINE, d.type);
        assertTrue(d.yFields.contains("a"));
        assertTrue(d.yFields.contains("b"));
    }

    @Test void decideTableForEmpty() {
        QueryResult qr = QueryResult.builder().columns(List.of()).rows(List.of()).build();
        Decision d = decider.decide(qr);
        assertEquals(ChartTypeDecider.ChartType.TABLE, d.type);
    }

    @Test void decideTableForNonNumeric() {
        QueryResult qr = QueryResult.builder()
                .columns(List.of("name", "desc"))
                .rows(List.of(
                        Map.of("name", "a", "desc", "x"),
                        Map.of("name", "b", "desc", "y")
                ))
                .rowCount(2L)
                .build();
        Decision d = decider.decide(qr);
        assertEquals(ChartTypeDecider.ChartType.TABLE, d.type);
    }
}
