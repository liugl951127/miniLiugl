package com.minimax.common.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V3.7.37+ Result<T> 包装单元测试
 * 
 * 验证 4 场景:
 * 1. Result.ok() 默认结构
 * 2. Result.fail() 错误结构
 * 3. JSON 序列化字段顺序
 * 4. timestamp 自动生成
 */
class ResultTest {

    @Test
    void test1_okDefault() {
        Result<String> r = Result.ok("hello");
        assertEquals(0, r.getCode());
        assertEquals("success", r.getMessage());
        assertEquals("hello", r.getData());
        assertNotNull(r.getTimestamp());
    }

    @Test
    void test2_okDataOnly() {
        // Result.ok(T data) - 简化
        Result<Integer> r = Result.ok(42);
        assertEquals(42, r.getData());
        assertEquals(0, r.getCode());  // 默认成功码
    }

    @Test
    void test3_fail() {
        Result<String> r = Result.fail("用户不存在");
        assertEquals(500, r.getCode());  // 默认业务错码
        assertEquals("用户不存在", r.getMessage());
        assertNull(r.getData());
    }

    @Test
    void test4_failHasCode() {
        // Result.fail 必定有错码
        Result<String> r = Result.fail("权限不足");
        assertNotEquals(0, r.getCode());
        assertEquals("权限不足", r.getMessage());
    }

    @Test
    void test5_jsonSerialization() throws Exception {
        Result<String> r = Result.ok("data-value");
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(r);
        
        assertTrue(json.contains("\"code\":0"));
        assertTrue(json.contains("\"message\":\"success\""));
        assertTrue(json.contains("\"data\":\"data-value\""));
        assertTrue(json.contains("\"timestamp\""));
    }

    @Test
    void test6_timestampIsMillis() {
        long before = System.currentTimeMillis();
        Result<String> r = Result.ok("x");
        long after = System.currentTimeMillis();
        assertTrue(r.getTimestamp() >= before);
        assertTrue(r.getTimestamp() <= after);
    }

    @Test
    void test7_jsonDeserialization() throws Exception {
        String json = "{\"code\":0,\"message\":\"success\",\"data\":{\"id\":1,\"name\":\"admin\"},\"timestamp\":1722566400000}";
        ObjectMapper mapper = new ObjectMapper();
        Result<?> r = mapper.readValue(json, Result.class);
        
        assertEquals(0, r.getCode());
        assertEquals("success", r.getMessage());
        assertNotNull(r.getData());
        assertEquals(1722566400000L, r.getTimestamp());
    }
}
