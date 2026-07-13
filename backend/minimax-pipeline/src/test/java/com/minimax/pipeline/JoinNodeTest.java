package com.minimax.pipeline;

import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.executor.ExecutionContext;
import com.minimax.pipeline.executor.NodeExecutor;
import com.minimax.pipeline.executor.transform.JoinNode;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JOIN 节点测试 (V5.32)
 */
class JoinNodeTest {

    private final NodeExecutor join = new JoinNode();

    @Test void innerJoin_MergesRows() {
        List<Map<String, Object>> left = List.of(
                Map.of("uid", 1, "name", "Alice"),
                Map.of("uid", 2, "name", "Bob"),
                Map.of("uid", 3, "name", "Charlie")
        );
        List<Map<String, Object>> right = List.of(
                Map.of("uid", 1, "order", "A1"),
                Map.of("uid", 2, "order", "B1"),
                Map.of("uid", 4, "order", "D1")  // 不匹配
        );
        Map<String, Object> cfg = Map.of("type", "INNER", "leftKey", "uid", "rightKey", "uid");
        ExecutionContext ctx = new ExecutionContext(1L, 1L, 1L, 1000, 100);
        List<Map<String, Object>> out = join.execute("n1", cfg, new java.util.LinkedHashMap<>(java.util.Map.of("left", left, "right", right)), ctx);
        assertEquals(2, out.size());
    }

    @Test void leftJoin_PreservesUnmatched() {
        List<Map<String, Object>> left = List.of(
                Map.of("uid", 1, "name", "Alice"),
                Map.of("uid", 2, "name", "Bob")
        );
        List<Map<String, Object>> right = List.of(
                Map.of("uid", 1, "order", "A1")
        );
        Map<String, Object> cfg = Map.of("type", "LEFT", "leftKey", "uid", "rightKey", "uid");
        ExecutionContext ctx = new ExecutionContext(1L, 1L, 1L, 1000, 100);
        List<Map<String, Object>> out = join.execute("n1", cfg, new java.util.LinkedHashMap<>(java.util.Map.of("left", left, "right", right)), ctx);
        assertEquals(2, out.size());  // Bob 也有, 右侧为 null
    }

    @Test void joinRequiresTwoInputs() {
        Map<String, Object> cfg = Map.of("leftKey", "uid", "rightKey", "uid");
        ExecutionContext ctx = new ExecutionContext(1L, 1L, 1L, 1000, 100);
        assertThrows(Exception.class, () ->
                join.execute("n1", cfg, new java.util.LinkedHashMap<>(java.util.Map.of("left", List.of())), ctx));
    }

    @Test void joinRequiresKeys() {
        List<Map<String, Object>> left = List.of(Map.of("a", 1));
        List<Map<String, Object>> right = List.of(Map.of("a", 1));
        ExecutionContext ctx = new ExecutionContext(1L, 1L, 1L, 1000, 100);
        assertThrows(Exception.class, () ->
                join.execute("n1", Map.of(), Map.of("left", left, "right", right), ctx));
    }

    @Test void joinSupportedType() {
        assertEquals(NodeType.JOIN, join.supportedType());
    }
}
