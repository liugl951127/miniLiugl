package com.minimax.ai;

import com.minimax.ai.codegen.SchemaCodeGenTool;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SchemaCodeGenTool 测试 (V2.7)
 */
class SchemaCodeGenToolTest {

    @Test
    void testGetCode() {
        SchemaCodeGenTool tool = new SchemaCodeGenTool(null, null);
        assertEquals("code.gen.from-schema", tool.getCode());
    }

    @Test
    void testCamelCase() throws Exception {
        SchemaCodeGenTool tool = new SchemaCodeGenTool(null, null);
        Method m = SchemaCodeGenTool.class.getDeclaredMethod("camelCase", String.class);
        m.setAccessible(true);
        assertEquals("UserOrder", m.invoke(tool, "user_order"));
        assertEquals("Order", m.invoke(tool, "order"));
        assertEquals("UserOrderItem", m.invoke(tool, "user_order_item"));
    }

    @Test
    void testCamelCaseLower() throws Exception {
        SchemaCodeGenTool tool = new SchemaCodeGenTool(null, null);
        Method m = SchemaCodeGenTool.class.getDeclaredMethod("camelCaseLower", String.class);
        m.setAccessible(true);
        assertEquals("userOrder", m.invoke(tool, "user_order"));
        assertEquals("order", m.invoke(tool, "order"));
    }

    @Test
    void testUpperFirst() throws Exception {
        SchemaCodeGenTool tool = new SchemaCodeGenTool(null, null);
        Method m = SchemaCodeGenTool.class.getDeclaredMethod("upperFirst", String.class);
        m.setAccessible(true);
        assertEquals("User", m.invoke(tool, "user"));
        assertEquals("U", m.invoke(tool, "u"));
    }

    @Test
    void testSqlToJava() throws Exception {
        SchemaCodeGenTool tool = new SchemaCodeGenTool(null, null);
        Method m = SchemaCodeGenTool.class.getDeclaredMethod("sqlToJava", String.class, int.class);
        m.setAccessible(true);
        assertEquals("String", m.invoke(tool, "VARCHAR", 12));
        assertEquals("Long", m.invoke(tool, "BIGINT", -5));
        assertEquals("Integer", m.invoke(tool, "INT", 4));
        assertEquals("java.time.LocalDateTime", m.invoke(tool, "TIMESTAMP", 93));
        assertEquals("Integer", m.invoke(tool, "TINYINT", -6));  // TINYINT 映射 Integer
        assertEquals("Boolean", m.invoke(tool, "BIT", -7));  // BIT 映射 Boolean
    }
}
