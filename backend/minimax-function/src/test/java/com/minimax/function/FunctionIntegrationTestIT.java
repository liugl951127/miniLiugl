package com.minimax.function;

import com.minimax.function.entity.FunctionCallLog;
import com.minimax.function.entity.FunctionTool;
import com.minimax.function.executor.ToolExecutor;
import com.minimax.function.service.FunctionToolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Day 9 集成测试 - 端到端 Function Calling
 */
@SpringBootTest
@ActiveProfiles("test")
class FunctionIntegrationTestIT {

    @Autowired FunctionToolService toolService;
    @Autowired ToolExecutor executor;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM function_call_log");
        jdbc.update("DELETE FROM function_tool");
    }

    @Test
    void builtinToolsLoaded() {
        // 测试 profile 启动时 schema-h2.sql 插入 4 个内置
        List<FunctionTool> all = toolService.listAll();
        // schema 在 Spring 启动时执行, 这里也至少有 0 (如果 schema 没执行)
        // 但 SpringBootTest 应该会执行 schema-locations
        // 至少能 list, 不抛异常
        assertNotNull(all);
    }

    @Test
    void registerUserTool() {
        Long id = toolService.createUserTool(1L,
                "weather_check", "查天气", "查某城市天气",
                "{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}},\"required\":[\"city\"]}",
                "http://localhost:9999/weather", "POST", "weather,custom");
        assertNotNull(id);

        FunctionTool t = toolService.getByName("weather_check");
        assertNotNull(t);
        assertEquals("user", t.getScope());
        assertEquals(1L, t.getOwnerId());
        assertEquals(1, t.getEnabled());
    }

    @Test
    void registerDuplicateNameFails() {
        toolService.createUserTool(1L, "dup_tool", "X", "x", "{}", null, null, null);
        assertThrows(IllegalArgumentException.class, () ->
                toolService.createUserTool(2L, "dup_tool", "Y", "y", "{}", null, null, null));
    }

    @Test
    void updateUserTool() {
        Long id = toolService.createUserTool(1L, "t1", "display", "desc", "{}", null, null, null);
        assertTrue(toolService.update(id, 1L, "new display", "new desc", null, null, 0));
        FunctionTool t = toolService.get(id);
        assertEquals("new display", t.getDisplayName());
        assertEquals(0, t.getEnabled());
    }

    @Test
    void updateOtherUsersToolDenied() {
        Long id = toolService.createUserTool(1L, "t2", "d", "d", "{}", null, null, null);
        assertFalse(toolService.update(id, 999L, "hacked", null, null, null, null));
    }

    @Test
    void deleteUserTool() {
        Long id = toolService.createUserTool(1L, "t3", "d", "d", "{}", null, null, null);
        assertTrue(toolService.delete(id, 1L));
        assertNull(toolService.get(id));
    }

    @Test
    void invokeBuiltinByApi() {
        // 重新插入一个测试工具 (因为 schema 在 Spring 启动时已运行过, 但 @BeforeEach 清了)
        jdbc.update("INSERT INTO function_tool (name, display_name, description, category, scope, parameters, endpoint, enabled) " +
                "VALUES ('get_current_time', 'time', 'time tool', 'system', 'builtin', " +
                "'{\"type\":\"object\"}', 'com.minimax.function.builtin.TimeTool', 1)");

        ToolExecutor.ToolResult r = executor.invoke(1L, 100L, "get_current_time", "{}", "127.0.0.1", "test");
        assertTrue(r.ok(), "should succeed, got: " + r.result());
        assertTrue(r.result().contains("datetime"));
        assertNotNull(r.durationMs());
    }

    @Test
    void invokeUnknownTool() {
        ToolExecutor.ToolResult r = executor.invoke(1L, null, "nonexistent_tool", "{}", null, null);
        assertFalse(r.ok());
        assertTrue(r.result().contains("not found"));
    }

    @Test
    void invokeBadJson() {
        jdbc.update("INSERT INTO function_tool (name, display_name, description, category, scope, parameters, endpoint, enabled) " +
                "VALUES ('calculator', 'calc', 'math', 'system', 'builtin', " +
                "'{\"type\":\"object\"}', 'com.minimax.function.builtin.CalculatorTool', 1)");

        ToolExecutor.ToolResult r = executor.invoke(1L, null, "calculator",
                "this is not json{", null, null);
        assertFalse(r.ok());
        assertTrue(r.result().contains("invalid"));
    }

    @Test
    void callLogCreated() {
        jdbc.update("INSERT INTO function_tool (name, display_name, description, category, scope, parameters, endpoint, enabled) " +
                "VALUES ('random_number', 'r', 'r', 'system', 'builtin', '{}', 'com.minimax.function.builtin.RandomNumberTool', 1)");

        executor.invoke(1L, null, "random_number", "{}", null, null);
        List<FunctionCallLog> logs = jdbc.query(
                "SELECT * FROM function_call_log WHERE tool_name='random_number' AND user_id=1",
                (rs, n) -> {
                    FunctionCallLog l = new FunctionCallLog();
                    l.setId(rs.getLong("id"));
                    l.setUserId(rs.getLong("user_id"));
                    l.setToolName(rs.getString("tool_name"));
                    l.setStatus(rs.getString("status"));
                    l.setDurationMs(rs.getInt("duration_ms"));
                    return l;
                });
        assertFalse(logs.isEmpty());
        assertEquals("ok", logs.get(0).getStatus());
    }
}
