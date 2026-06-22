package com.minimax.pipeline;

import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.executor.ExecutionContext;
import com.minimax.pipeline.executor.NodeExecutor;
import com.minimax.pipeline.executor.transform.SelectNode;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SELECT 节点测试 (V5.32) - 列投影 + 重命名 + 计算列
 */
class SelectNodeTest {

    private final NodeExecutor select = new SelectNode();

    @Test void renameAndCompute() {
        List<Map<String, Object>> input = List.of(
                Map.of("first_name", "Alice", "last_name", "Wong", "price", 100, "qty", 3),
                Map.of("first_name", "Bob", "last_name", "Lee", "price", 50, "qty", 7)
        );
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("columns", List.of(
                Map.of("alias", "name", "expr", "first_name + ' ' + last_name"),
                Map.of("alias", "total", "expr", "price * qty")
        ));
        ExecutionContext ctx = new ExecutionContext(1L, 1L, 1L, 1000, 100);
        List<Map<String, Object>> out = select.execute("n1", cfg, Map.of("up", input), ctx);
        assertEquals(2, out.size());
        assertEquals("Alice Wong", out.get(0).get("name"));
        assertEquals(300L, out.get(0).get("total"));
        assertEquals("Bob Lee", out.get(1).get("name"));
        assertEquals(350L, out.get(1).get("total"));
    }

    @Test void requiresColumns() {
        ExecutionContext ctx = new ExecutionContext(1L, 1L, 1L, 1000, 100);
        assertThrows(Exception.class, () ->
                select.execute("n1", Map.of(), Map.of("up", List.of()), ctx));
    }

    @Test void emptyInput() {
        Map<String, Object> cfg = Map.of("columns", List.of(Map.of("alias", "x", "expr", "a + 1")));
        ExecutionContext ctx = new ExecutionContext(1L, 1L, 1L, 1000, 100);
        List<Map<String, Object>> out = select.execute("n1", cfg, Map.of("up", List.of()), ctx);
        assertTrue(out.isEmpty());
    }

    @Test void supportedType() {
        assertEquals(NodeType.SELECT, select.supportedType());
    }
}
