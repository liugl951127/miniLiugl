package com.minimax.ai.marketplace.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * License 模板 (V3.5.2) 单元测试
 */
class LicenseTemplateTest {

    private LicenseTemplateMapper mapper;
    private LicenseTemplateService service;

    @BeforeEach
    void setUp() {
        mapper = Mockito.mock(LicenseTemplateMapper.class);
        service = new LicenseTemplateService(mapper);
    }

    /**
     * 测试 1: 创建模板
     */
    @Test
    @DisplayName("1. 创建模板 (含 features/limits JSON)")
    void testCreate() {
        when(mapper.findByKey("custom-1")).thenReturn(null);
        when(mapper.insert(any())).thenAnswer(inv -> {
            LicenseTemplate t = inv.getArgument(0);
            t.setId(1L);
            t.setVersion(1);
            return 1;
        });
        LicenseTemplate t = service.create(LicenseTemplateService.LicenseTemplateDraft.builder()
                .templateKey("custom-1")
                .name("自定义")
                .licenseType("PERSONAL")
                .quotaCalls(5000L)
                .quotaDays(30)
                .priceCents(4900L)
                .features(List.of("inference", "rag"))
                .limits(Map.of("qps", 5))
                .isPublic(1)
                .isActive(1)
                .createdBy(100L)
                .build());
        assertEquals(1L, t.getId());
        assertEquals("custom-1", t.getTemplateKey());
        assertEquals(1, t.getVersion());
        assertTrue(t.getFeatures().contains("inference"));
    }

    /**
     * 测试 2: 重复 key 抛错
     */
    @Test
    @DisplayName("2. 重复 templateKey 抛 IllegalArgumentException")
    void testDuplicateKey() {
        LicenseTemplate existing = new LicenseTemplate();
        existing.setId(99L);
        when(mapper.findByKey("dup")).thenReturn(existing);
        assertThrows(IllegalArgumentException.class, () -> service.create(
                LicenseTemplateService.LicenseTemplateDraft.builder()
                        .templateKey("dup").name("x").licenseType("TRIAL").build()));
    }

    /**
     * 测试 3: 更新模板 (version +1)
     */
    @Test
    @DisplayName("3. 更新模板 (version 递增)")
    void testUpdate() {
        LicenseTemplate existing = new LicenseTemplate();
        existing.setId(1L);
        existing.setVersion(1);
        existing.setName("旧名");
        when(mapper.findById(1L)).thenReturn(existing);
        LicenseTemplate updated = service.update(1L, LicenseTemplateService.LicenseTemplateDraft.builder()
                .name("新名").priceCents(9900L).build());
        assertEquals(2, updated.getVersion());
        assertEquals("新名", updated.getName());
    }

    /**
     * 测试 4: 克隆模板
     */
    @Test
    @DisplayName("4. 克隆模板 (新 key + 特性复制)")
    void testClone() {
        LicenseTemplate src = new LicenseTemplate();
        src.setId(1L);
        src.setTemplateKey("trial-7d");
        src.setName("试用版");
        src.setLicenseType("TRIAL");
        src.setQuotaCalls(1000L);
        src.setQuotaDays(7);
        src.setPriceCents(0L);
        src.setFeatures("[\"inference\",\"rag\"]");
        src.setLimits("{\"qps\":2}");
        src.setIsPublic(1);
        src.setIsActive(1);
        when(mapper.findById(1L)).thenReturn(src);
        when(mapper.findByKey("trial-30d")).thenReturn(null);
        when(mapper.insert(any())).thenAnswer(inv -> {
            LicenseTemplate t = inv.getArgument(0);
            t.setId(2L);
            return 1;
        });
        LicenseTemplate clone = service.clone(1L, "trial-30d");
        assertEquals(2L, clone.getId());
        assertEquals("trial-30d", clone.getTemplateKey());
        assertEquals("TRIAL", clone.getLicenseType());
        assertTrue(clone.getFeatures().contains("inference"));
    }

    /**
     * 测试 5: 下架 (isActive=0)
     */
    @Test
    @DisplayName("5. 下架模板 (isActive=0)")
    void testDeactivate() {
        LicenseTemplate t = new LicenseTemplate();
        t.setId(1L);
        t.setIsActive(1);
        t.setVersion(1);
        when(mapper.findById(1L)).thenReturn(t);
        boolean ok = service.deactivate(1L);
        assertTrue(ok);
        assertEquals(0, t.getIsActive());
        assertEquals(2, t.getVersion());
    }

    /**
     * 测试 6: 签发 license
     */
    @Test
    @DisplayName("6. 签发 license (从模板)")
    void testIssue() {
        LicenseTemplate t = new LicenseTemplate();
        t.setId(1L);
        t.setTemplateKey("personal-monthly");
        t.setLicenseType("PERSONAL");
        t.setQuotaCalls(100000L);
        t.setQuotaDays(30);
        t.setPriceCents(9900L);
        t.setFeatures("[\"inference\",\"rag\"]");
        t.setLimits("{\"qps\":10}");
        t.setIsActive(1);
        t.setVersion(2);
        when(mapper.findByKey("personal-monthly")).thenReturn(t);
        LicenseTemplateService.IssuedLicense lic = service.issue("personal-monthly", 12345L);
        assertEquals(100000L, lic.quotaCalls);
        assertEquals(30, lic.quotaDays);
        assertEquals("PERSONAL", lic.licenseType);
        assertEquals(2, lic.templateVersion);
        assertEquals(12345L, lic.userId);
        assertTrue(lic.expiresAt > System.currentTimeMillis());
        assertEquals(2, lic.features.size());
    }

    /**
     * 测试 7: 签发下架模板抛错
     */
    @Test
    @DisplayName("7. 签发下架模板抛 IllegalStateException")
    void testIssueDeactivated() {
        LicenseTemplate t = new LicenseTemplate();
        t.setIsActive(0);
        when(mapper.findByKey("x")).thenReturn(t);
        assertThrows(IllegalStateException.class, () -> service.issue("x", 1L));
    }

    /**
     * 测试 8: 永久 license (quotaDays=0)
     */
    @Test
    @DisplayName("8. 永久 license (quotaDays=0 → expiresAt=0)")
    void testIssuePermanent() {
        LicenseTemplate t = new LicenseTemplate();
        t.setLicenseType("ENTERPRISE");
        t.setQuotaCalls(0L);
        t.setQuotaDays(0);
        t.setPriceCents(999900L);
        t.setFeatures("[]");
        t.setLimits("{}");
        t.setIsActive(1);
        t.setVersion(1);
        t.setTemplateKey("ent-perm");
        when(mapper.findByKey("ent-perm")).thenReturn(t);
        LicenseTemplateService.IssuedLicense lic = service.issue("ent-perm", 1L);
        assertEquals(0L, lic.expiresAt, "永久 license expiresAt 应为 0");
    }

    /**
     * 测试 9: 模板对比
     */
    @Test
    @DisplayName("9. 模板对比 (特性矩阵)")
    void testCompare() {
        LicenseTemplate t1 = new LicenseTemplate();
        t1.setTemplateKey("trial");
        t1.setFeatures("[\"inference\"]");
        LicenseTemplate t2 = new LicenseTemplate();
        t2.setTemplateKey("personal");
        t2.setFeatures("[\"inference\",\"rag\"]");
        when(mapper.findByKey("trial")).thenReturn(t1);
        when(mapper.findByKey("personal")).thenReturn(t2);
        Map<String, Object> cmp = service.compare(List.of("trial", "personal"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> matrix = (List<Map<String, Object>>) cmp.get("featureMatrix");
        assertTrue(matrix.size() >= 2, "应有 2 个特性行");
    }

    /**
     * 测试 10: 统计
     */
    @Test
    @DisplayName("10. 模板统计 (总数/类型/价格)")
    void testStats() {
        LicenseTemplate t1 = new LicenseTemplate();
        t1.setTemplateKey("a");
        t1.setLicenseType("TRIAL");
        t1.setQuotaCalls(1000L);
        t1.setPriceCents(0L);
        t1.setIsActive(1);
        LicenseTemplate t2 = new LicenseTemplate();
        t2.setTemplateKey("b");
        t2.setLicenseType("PERSONAL");
        t2.setQuotaCalls(100000L);
        t2.setPriceCents(9900L);
        t2.setIsActive(1);
        when(mapper.listPublic()).thenReturn(List.of(t1, t2));
        Map<String, Object> s = service.stats();
        assertEquals(2, s.get("total"));
        assertEquals(2, s.get("active"));
        assertNotNull(s.get("byType"));
    }

    /**
     * 测试 11: 预置模板初始化
     */
    @Test
    @DisplayName("11. 预置模板初始化 (seedDefaults)")
    void testSeedDefaults() {
        // 第一次查全部 null, 创建
        when(mapper.findByKey(Mockito.anyString())).thenReturn(null);
        when(mapper.insert(any())).thenAnswer(inv -> {
            LicenseTemplate t = inv.getArgument(0);
            t.setId(System.nanoTime());
            return 1;
        });
        service.seedDefaults();
        // 4 次 insert (TRIAL/PERSONAL/COMMERCIAL/ENTERPRISE)
        Mockito.verify(mapper, Mockito.times(4)).insert(any());
    }

    /**
     * 测试 12: 预置幂等 (重复 seed 不重复创建)
     */
    @Test
    @DisplayName("12. 预置幂等 (已存在则跳过)")
    void testSeedIdempotent() {
        LicenseTemplate existing = new LicenseTemplate();
        when(mapper.findByKey(Mockito.anyString())).thenReturn(existing);
        service.seedDefaults();
        Mockito.verify(mapper, Mockito.never()).insert(any());
    }
}
