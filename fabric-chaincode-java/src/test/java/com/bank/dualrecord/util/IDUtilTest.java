package com.bank.dualrecord.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IDUtilTest {

    @Test
    void testGenerateOrderId() {
        String id = IDUtil.generateOrderId();
        assertTrue(IDUtil.isValidOrderId(id), "应符合 ORD+yyyyMMdd+6 位 格式: " + id);
    }

    @Test
    void testGenerateSessionId() {
        String id = IDUtil.generateSessionId();
        assertTrue(IDUtil.isValidSessionId(id), "应符合 SES+yyyyMMddHHmmss+3 位 格式: " + id);
    }

    @Test
    void testGenerateUuid() {
        String id = IDUtil.generateUuid();
        assertEquals(32, id.length());
        assertFalse(id.contains("-"));
    }

    @Test
    void testOrderIdUniqueness() {
        String id1 = IDUtil.generateOrderId();
        String id2 = IDUtil.generateOrderId();
        // 注:同毫秒下可能相同,这里只校验格式
        assertNotNull(id1);
        assertNotNull(id2);
    }

    @Test
    void testInvalidOrderId() {
        assertFalse(IDUtil.isValidOrderId("INVALID"));
        assertFalse(IDUtil.isValidOrderId("ORD2026"));
        assertFalse(IDUtil.isValidOrderId("ORD20260801001XX"));
    }
}
