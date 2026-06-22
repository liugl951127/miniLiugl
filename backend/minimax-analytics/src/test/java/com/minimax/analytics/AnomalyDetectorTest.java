package com.minimax.analytics;

import com.minimax.analytics.service.report.AnomalyDetector;
import com.minimax.analytics.service.report.AnomalyDetector.Anomaly;
import com.minimax.analytics.vo.QueryResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 异常检测测试 (V5.31)
 */
class AnomalyDetectorTest {

    private final AnomalyDetector detector = new AnomalyDetector();

    @Test void detectOutlier() {
        QueryResult qr = QueryResult.builder()
                .columns(List.of("value"))
                .rows(Arrays.asList(
                        Map.of("value", 1.0),
                        Map.of("value", 2.0),
                        Map.of("value", 3.0),
                        Map.of("value", 4.0),
                        Map.of("value", 5.0),
                        Map.of("value", 100.0)  // 明显离群
                ))
                .rowCount(6L)
                .build();
        List<Anomaly> anomalies = detector.detect(qr);
        assertFalse(anomalies.isEmpty(), "应该检测到离群点");
        boolean found = anomalies.stream().anyMatch(a -> Objects.equals(a.value, 100.0));
        assertTrue(found);
    }

    @Test void noOutlierInUniform() {
        QueryResult qr = QueryResult.builder()
                .columns(List.of("v"))
                .rows(Arrays.asList(
                        Map.of("v", 10), Map.of("v", 11), Map.of("v", 12),
                        Map.of("v", 13), Map.of("v", 14)
                ))
                .rowCount(5L)
                .build();
        List<Anomaly> anomalies = detector.detect(qr);
        assertTrue(anomalies.isEmpty());
    }

    @Test void skipNonNumericColumn() {
        QueryResult qr = QueryResult.builder()
                .columns(List.of("name"))
                .rows(Arrays.asList(
                        Map.of("name", "alice"),
                        Map.of("name", "bob"),
                        Map.of("name", "charlie"),
                        Map.of("name", "diana"),
                        Map.of("name", "eve")
                ))
                .build();
        List<Anomaly> anomalies = detector.detect(qr);
        assertTrue(anomalies.isEmpty(), "字符串列不应该检测异常");
    }

    @Test void handleNulls() {
        QueryResult qr = QueryResult.builder()
                .columns(List.of("v"))
                .rows(Arrays.asList(
                        Map.of("v", 1.0),
                        hash("v", null),
                        Map.of("v", 2.0),
                        hash("v", null),
                        Map.of("v", 3.0)
                ))
                .build();
        List<Anomaly> anomalies = detector.detect(qr);
        // 5 行 1 null, numCount=3 < 5, 不检测
        assertTrue(anomalies.isEmpty());
    }

    /** 允许 null value 的 HashMap (Map.of 不支持 null) */
    private static Map<String, Object> hash(String k, Object v) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put(k, v);
        return m;
    }

    @Test void handleEmptyResult() {
        QueryResult qr = QueryResult.builder().columns(List.of("v")).rows(List.of()).build();
        List<Anomaly> anomalies = detector.detect(qr);
        assertTrue(anomalies.isEmpty());
    }

    @Test void handleNullColumns() {
        QueryResult qr = QueryResult.builder().build();
        List<Anomaly> anomalies = detector.detect(qr);
        assertTrue(anomalies.isEmpty());
    }
}
