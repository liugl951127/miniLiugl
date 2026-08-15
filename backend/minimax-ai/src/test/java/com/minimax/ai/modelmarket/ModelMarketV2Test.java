package com.minimax.ai.modelmarket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模型市场 v2 (V3.3.2) 单元测试
 */
class ModelMarketV2Test {

    /**
     * 测试 1: SHA256 工具
     */
    @Test
    @DisplayName("1. SHA256 计算")
    void testSha256() {
        ModelMarketV2Service svc = new ModelMarketV2Service(null, null, null);
        try {
            java.lang.reflect.Method m = ModelMarketV2Service.class.getDeclaredMethod("sha256", byte[].class);
            m.setAccessible(true);
            String h1 = (String) m.invoke(svc, "hello".getBytes());
            String h2 = (String) m.invoke(svc, "hello".getBytes());
            String h3 = (String) m.invoke(svc, "world".getBytes());
            // 同输入同输出
            assertEquals(h1, h2);
            // 不同输入不同输出
            assertNotEquals(h1, h3);
            // SHA256 = 64 字符
            assertEquals(64, h1.length());
        } catch (Exception e) { fail(e.getMessage()); }
    }

    /**
     * 测试 2: ModelVersion 实体
     */
    @Test
    @DisplayName("2. ModelVersion 实体字段")
    void testModelVersionEntity() {
        com.minimax.ai.entity.ModelVersion v = new com.minimax.ai.entity.ModelVersion();
        v.setVersion("1.0.0");
        v.setStatus("PUBLISHED");
        v.setIsLatest(true);
        v.setSha256("abc123");
        v.setSizeBytes(1024L);
        assertEquals("1.0.0", v.getVersion());
        assertEquals("PUBLISHED", v.getStatus());
        assertTrue(v.getIsLatest());
        assertEquals(1024L, v.getSizeBytes());
    }

    /**
     * 测试 3: ModelLicense 实体
     */
    @Test
    @DisplayName("3. ModelLicense 4 类型 + 3 状态")
    void testLicenseEntity() {
        com.minimax.ai.entity.ModelLicense l = new com.minimax.ai.entity.ModelLicense();
        for (String t : new String[]{"TRIAL", "PERSONAL", "COMMERCIAL", "ENTERPRISE"}) {
            l.setLicenseType(t);
            assertEquals(t, l.getLicenseType());
        }
        for (String s : new String[]{"ACTIVE", "EXPIRED", "REVOKED"}) {
            l.setStatus(s);
            assertEquals(s, l.getStatus());
        }
    }

    /**
     * 测试 4: BillingRecord 5 类型
     */
    @Test
    @DisplayName("4. BillingRecord 5 类型 + 4 状态")
    void testBillingEntity() {
        com.minimax.ai.entity.BillingRecord b = new com.minimax.ai.entity.BillingRecord();
        for (String t : new String[]{"PURCHASE", "RENEW", "REFUND", "TOPUP", "USAGE"}) {
            b.setRecordType(t);
            assertEquals(t, b.getRecordType());
        }
        for (String s : new String[]{"PENDING", "SUCCESS", "FAILED", "REFUNDED"}) {
            b.setStatus(s);
            assertEquals(s, b.getStatus());
        }
    }

    /**
     * 测试 5: price 字段 (分)
     */
    @Test
    @DisplayName("5. 价格字段 (分单位, 0=免费)")
    void testPriceField() {
        com.minimax.ai.entity.ModelLicense l = new com.minimax.ai.entity.ModelLicense();
        l.setPriceCents(9900L);  // 99.00 元
        l.setQuotaCalls(0L);     // 无限
        assertEquals(9900L, l.getPriceCents());
        assertEquals(0L, l.getQuotaCalls());
    }

    /**
     * 测试 6: 退款字段 (负数)
     */
    @Test
    @DisplayName("6. 退款记录 amountCents 负数")
    void testRefundAmount() {
        com.minimax.ai.entity.BillingRecord b = new com.minimax.ai.entity.BillingRecord();
        b.setRecordType("REFUND");
        b.setAmountCents(-9900L);  // 退款 99.00
        assertEquals(-9900L, b.getAmountCents());
    }
}
