package com.minimax.pipeline;

import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.executor.ExecutionContext;
import com.minimax.pipeline.executor.NodeExecutor;
import com.minimax.pipeline.executor.transform.AggregateNode;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AGGREGATE 节点测试 (V5.32)
 */
class AggregateNodeTest {

    private final NodeExecutor agg = new AggregateNode();

    @Test void groupByAndSum() {
        List<Map<String, Object>> rows = List.of(
                Map.of("category", "A", "amount", 100),
                Map.of("category", "B", "amount", 200),
                Map.of("category", "A", "amount", 50),
                Map.of("category", "B", "amount", 300)
        );
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("groupBy", List.of("category"));
        cfg.put("aggregations", List.of(
                Map.of("col", "amount", "op", "SUM", "alias", "total"),
                Map.of("col", "amount", "op", "COUNT", "alias", "cnt")
        ));
        ExecutionContext ctx = new ExecutionContext(1L, 1L, 1L, 1000, 100);
        List<Map<String, Object>> out = agg.execute("n1", cfg, Map.of("up", rows), ctx);
        assertEquals(2, out.size());
        Map<String, Object> a = out.stream().filter(r -> r.get("category").equals("A")).findFirst().orElseThrow();
        assertEquals(150.0, a.get("total"));
        assertEquals(2L, a.get("cnt"));
    }

    @Test void noGroupBy_AggregatesAll() {
        List<Map<String, Object>> rows = List.of(
                Map.of("v", 10), Map.of("v", 20), Map.of("v", 30)
        );
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("aggregations", List.of(
                Map.of("col", "v", "op", "SUM", "alias", "s"),
                Map.of("col", "v", "op", "AVG", "alias", "avg"),
                Map.of("col", "v", "op", "MIN", "alias", "min"),
                Map.of("col", "v", "op", "MAX", "alias", "max")
        ));
        ExecutionContext ctx = new ExecutionContext(1L, 1L, 1L, 1000, 100);
        List<Map<String, Object>> out = agg.execute("n1", cfg, Map.of("up", rows), ctx);
        assertEquals(1, out.size());
        Map<String, Object> r = out.get(0);
        assertEquals(60.0, r.get("s"));
        assertEquals(20.0, r.get("avg"));
        assertEquals(10.0, r.get("min"));
        assertEquals(30.0, r.get("max"));
    }

    @Test void countStar() {
        List<Map<String, Object>> rows = List.of(
                Map.of("a", 1), Map.of("a", 2), Map.of("a", 3), Map.of("a", 4)
        );
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("aggregations", List.of(Map.of("col", "*", "op", "COUNT", "alias", "total")));
        ExecutionContext ctx = new ExecutionContext(1L, 1L, 1L, 1000, 100);
        List<Map<String, Object>> out = agg.execute("n1", cfg, Map.of("up", rows), ctx);
        assertEquals(4, out.get(0).get("total"));
    }

    @Test void emptyInput() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("aggregations", List.of(Map.of("col", "x", "op", "COUNT", "alias", "c")));
        ExecutionContext ctx = new ExecutionContext(1L, 1L, 1L, 1000, 100);
        List<Map<String, Object>> out = agg.execute("n1", cfg, Map.of("up", List.of()), ctx);
        assertTrue(out.isEmpty());
    }

    @Test void aggregateSupportedType() {
        assertEquals(NodeType.AGGREGATE, agg.supportedType());
    }
}
