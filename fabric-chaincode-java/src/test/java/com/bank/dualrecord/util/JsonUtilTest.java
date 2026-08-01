package com.bank.dualrecord.util;

import com.bank.dualrecord.model.OrderState;
import com.bank.dualrecord.model.ProductType;
import com.bank.dualrecord.model.Evidence;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonUtilTest {

    @Test
    void testEvidenceSerialization() {
        Evidence e = new Evidence();
        e.setOrderId("ORD20260801000001");
        e.setCustomerId("C001");
        e.setProductType(ProductType.INSURANCE);
        e.setProductName("XX 寿险");
        e.setAmount(5000000);
        e.setVideoHash("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
        e.setState(OrderState.VERIFIED);

        String json = JsonUtil.toJson(e);
        assertNotNull(json);
        assertTrue(json.contains("ORD20260801000001"));
        assertTrue(json.contains("\"state\":\"VERIFIED\""), "枚举应序列化为字符串");
        assertTrue(json.contains("\"productType\":\"INSURANCE\""));
    }

    @Test
    void testEvidenceDeserialization() {
        String json = "{\"orderId\":\"ORD20260801000002\",\"customerId\":\"C002\",\"productType\":\"WEALTH\",\"state\":\"SCRIPTING\"}";
        Evidence e = JsonUtil.fromJson(json, Evidence.class);
        assertEquals("ORD20260801000002", e.getOrderId());
        assertEquals(ProductType.WEALTH, e.getProductType());
        assertEquals(OrderState.SCRIPTING, e.getState());
    }

    @Test
    void testIgnoreNullFields() {
        Evidence e = new Evidence();
        e.setOrderId("ORD20260801000003");
        String json = JsonUtil.toJson(e);
        assertFalse(json.contains("customerId"), "null 字段应被忽略");
    }
}
