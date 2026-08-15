package com.minimax.pipeline;

import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.executor.ExecutionContext;
import com.minimax.pipeline.executor.NodeExecutor;
import com.minimax.pipeline.executor.transform.LimitNode;
import com.minimax.pipeline.executor.transform.SortNode;
import com.minimax.pipeline.executor.transform.DistinctNode;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SORT/LIMIT/DISTINCT 节点测试 (V5.32)
 */
class SortLimitNodeTest {

    @Test void sortAscDesc() {
        NodeExecutor sort = new SortNode();
        List<Map<String, Object>> rows = new ArrayList<>(List.of(
                Map.of("name", "Charlie", "score", 80),
                Map.of("name", "Alice", "score", 95),
                Map.of("name", "Bob", "score", 70)
        ));
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("orders", List.of(
                Map.of("col", "score", "dir", "DESC")
        ));
        ExecutionContext ctx = new ExecutionContext(1L, 1L, 1L, 1000, 100);
        List<Map<String, Object>> out = sort.execute("n1", cfg, Map.of("up", rows), ctx);
        assertEquals("Alice", out.get(0).get("name"));
        assertEquals("Bob", out.get(2).get("name"));
    }

    @Test void limitTruncates() {
        NodeExecutor limit = new LimitNode();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 1000; i++) rows.add(Map.of("i", i));
        Map<String, Object> cfg = Map.of("limit", 50, "offset", 100);
        ExecutionContext ctx = new ExecutionContext(1L, 1L, 1L, 1000, 100);
        List<Map<String, Object>> out = limit.execute("n1", cfg, Map.of("up", rows), ctx);
        assertEquals(50, out.size());
        assertEquals(100, out.get(0).get("i"));
        assertEquals(149, out.get(49).get("i"));
    }

    @Test void limitRequiresLimitConfig() {
        NodeExecutor limit = new LimitNode();
        ExecutionContext ctx = new ExecutionContext(1L, 1L, 1L, 1000, 100);
        assertThrows(Exception.class, () ->
                limit.execute("n1", Map.of(), Map.of("up", List.of()), ctx));
    }

    @Test void distinctByColumns() {
        NodeExecutor distinct = new DistinctNode();
        List<Map<String, Object>> rows = List.of(
                Map.of("user", 1, "action", "login"),
                Map.of("user", 1, "action", "login"),  // dup
                Map.of("user", 1, "action", "logout"),
                Map.of("user", 2, "action", "login")
        );
        Map<String, Object> cfg = Map.of("columns", List.of("user", "action"));
        ExecutionContext ctx = new ExecutionContext(1L, 1L, 1L, 1000, 100);
        List<Map<String, Object>> out = distinct.execute("n1", cfg, Map.of("up", rows), ctx);
        assertEquals(3, out.size());
    }

    @Test void distinctAllRows() {
        NodeExecutor distinct = new DistinctNode();
        List<Map<String, Object>> rows = List.of(
                Map.of("a", 1), Map.of("a", 1), Map.of("a", 2)
        );
        Map<String, Object> cfg = Map.of();  // 无 columns
        ExecutionContext ctx = new ExecutionContext(1L, 1L, 1L, 1000, 100);
        List<Map<String, Object>> out = distinct.execute("n1", cfg, Map.of("up", rows), ctx);
        assertEquals(2, out.size());
    }

    @Test void sortSupportedType() { assertEquals(NodeType.SORT, new SortNode().supportedType()); }
    @Test void limitSupportedType() { assertEquals(NodeType.LIMIT, new LimitNode().supportedType()); }
    @Test void distinctSupportedType() { assertEquals(NodeType.DISTINCT, new DistinctNode().supportedType()); }
}
