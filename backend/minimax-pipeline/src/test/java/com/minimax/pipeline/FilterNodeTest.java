package com.minimax.pipeline;

import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.executor.ExecutionContext;
import com.minimax.pipeline.executor.NodeExecutor;
import com.minimax.pipeline.executor.transform.FilterNode;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FILTER 节点测试 (V5.32)
 */
class FilterNodeTest {

    private final NodeExecutor filter = new FilterNode();

    @Test void filterByCondition() {
        Map<String, Object> cfg = Map.of("condition", "age >= 18 && status == \"ACTIVE\"");
        List<Map<String, Object>> input = List.of(
                Map.of("age", 25, "status", "ACTIVE", "name", "Alice"),
                Map.of("age", 16, "status", "ACTIVE", "name", "Bob"),
                Map.of("age", 30, "status", "INACTIVE", "name", "Charlie"),
                Map.of("age", 22, "status", "ACTIVE", "name", "Diana")
        );
        ExecutionContext ctx = new ExecutionContext(1L, 1L, 1L, 1000, 100);
        List<Map<String, Object>> out = filter.execute("n1", cfg, Map.of("upstream", input), ctx);
        assertEquals(2, out.size());
        assertTrue(out.stream().anyMatch(r -> r.get("name").equals("Alice")));
        assertTrue(out.stream().anyMatch(r -> r.get("name").equals("Diana")));
    }

    @Test void filterEmptyInput() {
        Map<String, Object> cfg = Map.of("condition", "x > 0");
        ExecutionContext ctx = new ExecutionContext(1L, 1L, 1L, 1000, 100);
        List<Map<String, Object>> out = filter.execute("n1", cfg, Map.of("upstream", List.of()), ctx);
        assertTrue(out.isEmpty());
    }

    @Test void filterRequiresCondition() {
        ExecutionContext ctx = new ExecutionContext(1L, 1L, 1L, 1000, 100);
        assertThrows(Exception.class, () ->
                filter.execute("n1", Map.of(), Map.of("upstream", List.of()), ctx));
    }

    @Test void filterMultipleUpstream_Errors() {
        Map<String, Object> cfg = Map.of("condition", "x > 0");
        ExecutionContext ctx = new ExecutionContext(1L, 1L, 1L, 1000, 100);
        assertThrows(Exception.class, () ->
                filter.execute("n1", cfg, Map.of("a", List.of(), "b", List.of()), ctx));
    }

    @Test void filterSupportedType() {
        assertEquals(NodeType.FILTER, filter.supportedType());
    }
}
