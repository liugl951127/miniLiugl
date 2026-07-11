package com.minimax.ai;

import com.minimax.ai.tool.builtin.DeduplicateTool;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DeduplicateTool 测试 (V2.7)
 */
class DeduplicateToolTest {

    @Test
    void testGetCode() {
        DeduplicateTool tool = new DeduplicateTool(null, null);
        assertEquals("data.clean.deduplicate", tool.getCode());
    }

    @Test
    void testBuildKey() throws Exception {
        DeduplicateTool tool = new DeduplicateTool(null, null);
        Method m = DeduplicateTool.class.getDeclaredMethod("buildKey", Map.class, List.class);
        m.setAccessible(true);
        Map<String, Object> row1 = new HashMap<>();
        row1.put("name", "Alice");
        row1.put("age", 30);
        String key1 = (String) m.invoke(tool, row1, Arrays.asList("name", "age"));
        assertEquals("Alice\u000130", key1);
    }

    @Test
    void testBuildKeyWithNull() throws Exception {
        DeduplicateTool tool = new DeduplicateTool(null, null);
        Method m = DeduplicateTool.class.getDeclaredMethod("buildKey", Map.class, List.class);
        m.setAccessible(true);
        Map<String, Object> row = new HashMap<>();
        row.put("a", null);
        row.put("b", "x");
        String key = (String) m.invoke(tool, row, Arrays.asList("a", "b"));
        assertEquals("NULL\u0001x", key);
    }
}
