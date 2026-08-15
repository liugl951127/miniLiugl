package com.minimax.ai.marketplace.template;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * License 模板服务 (V3.5.2 自研)
 *
 * <h3>核心能力</h3>
 * <ol>
 *   <li>模板 CRUD: 创建/查询/更新/删除/克隆</li>
 *   <li>模板签发: 从模板生成 ModelLicense 字段</li>
 *   <li>模板对比: 多个模板特性 / 限制差异</li>
 *   <li>模板预置: 启动时自动 seed 4 套默认模板</li>
 * </ol>
 *
 * <h3>复杂度</h3>
 * 模板创建/查询 O(1), 克隆 O(F) F=特性数, 签发 O(1)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseTemplateService {

    private final LicenseTemplateMapper mapper;
    private final ObjectMapper json = new ObjectMapper();

    /** 4 种预置模板 key */
    public static final String KEY_TRIAL = "trial-7d";
    public static final String KEY_PERSONAL = "personal-monthly";
    public static final String KEY_COMMERCIAL = "commercial-quarterly";
    public static final String KEY_ENTERPRISE = "enterprise-yearly";

    /**
     * 启动时初始化预置模板 (幂等)
     */
    public void seedDefaults() {
        if (mapper.findByKey(KEY_TRIAL) == null) {
            create(LicenseTemplateDraft.builder()
                    .templateKey(KEY_TRIAL)
                    .name("试用版 (7天)")
                    .licenseType("TRIAL")
                    .description("新用户 7 天免费试用, 1000 次调用")
                    .quotaCalls(1000L)
                    .quotaDays(7)
                    .priceCents(0L)
                    .features(List.of("inference", "rag"))
                    .limits(Map.of("qps", 2, "rpm", 60))
                    .isPublic(1)
                    .isActive(1)
                    .createdBy(0L)
                    .build());
        }
        if (mapper.findByKey(KEY_PERSONAL) == null) {
            create(LicenseTemplateDraft.builder()
                    .templateKey(KEY_PERSONAL)
                    .name("个人版 (月付)")
                    .licenseType("PERSONAL")
                    .description("个人用户包月, 10万次调用, 含 RAG/Agent")
                    .quotaCalls(100_000L)
                    .quotaDays(30)
                    .priceCents(9900L)  // ¥99
                    .features(List.of("inference", "rag", "agent", "knowledge-base"))
                    .limits(Map.of("qps", 10, "rpm", 600))
                    .isPublic(1)
                    .isActive(1)
                    .createdBy(0L)
                    .build());
        }
        if (mapper.findByKey(KEY_COMMERCIAL) == null) {
            create(LicenseTemplateDraft.builder()
                    .templateKey(KEY_COMMERCIAL)
                    .name("商业版 (季付)")
                    .licenseType("COMMERCIAL")
                    .description("中小企业季付, 100万次调用, 全部特性")
                    .quotaCalls(1_000_000L)
                    .quotaDays(90)
                    .priceCents(89900L)  // ¥899
                    .features(List.of("inference", "rag", "agent", "knowledge-base", "training", "marketplace"))
                    .limits(Map.of("qps", 50, "rpm", 3000, "concurrent_users", 100))
                    .isPublic(1)
                    .isActive(1)
                    .createdBy(0L)
                    .build());
        }
        if (mapper.findByKey(KEY_ENTERPRISE) == null) {
            create(LicenseTemplateDraft.builder()
                    .templateKey(KEY_ENTERPRISE)
                    .name("企业版 (年付)")
                    .licenseType("ENTERPRISE")
                    .description("大型企业年付, 无限调用, 含 SLA + 私有部署")
                    .quotaCalls(0L)  // 0=无限
                    .quotaDays(365)
                    .priceCents(99_9900L)  // ¥9999
                    .features(List.of("inference", "rag", "agent", "knowledge-base", "training",
                            "marketplace", "sla", "private-deploy", "sso"))
                    .limits(Map.of("qps", 500, "rpm", 30000, "concurrent_users", -1))
                    .isPublic(1)
                    .isActive(1)
                    .createdBy(0L)
                    .build());
        }
        log.info("[LicenseTemplate] 预置模板初始化完成");
    }

    /**
     * 创建模板
     */
    public LicenseTemplate create(LicenseTemplateDraft draft) {
        if (mapper.findByKey(draft.getTemplateKey()) != null) {
            throw new IllegalArgumentException("templateKey 已存在: " + draft.getTemplateKey());
        }
        LicenseTemplate t = new LicenseTemplate();
        t.setTemplateKey(draft.getTemplateKey());
        t.setName(draft.getName());
        t.setLicenseType(draft.getLicenseType());
        t.setDescription(draft.getDescription());
        t.setQuotaCalls(draft.getQuotaCalls() != null ? draft.getQuotaCalls() : 0L);
        t.setQuotaDays(draft.getQuotaDays() != null ? draft.getQuotaDays() : 0);
        t.setPriceCents(draft.getPriceCents() != null ? draft.getPriceCents() : 0L);
        t.setFeatures(toJson(draft.getFeatures()));
        t.setLimits(toJson(draft.getLimits()));
        t.setIsPublic(draft.getIsPublic() != null ? draft.getIsPublic() : 0);
        t.setIsActive(draft.getIsActive() != null ? draft.getIsActive() : 1);
        t.setVersion(1);
        t.setCreatedBy(draft.getCreatedBy() != null ? draft.getCreatedBy() : 0L);
        mapper.insert(t);
        log.info("[LicenseTemplate] 创建 templateKey={}, id={}", t.getTemplateKey(), t.getId());
        return t;
    }

    /**
     * 更新模板 (version +1)
     */
    public LicenseTemplate update(Long id, LicenseTemplateDraft draft) {
        LicenseTemplate existing = mapper.findById(id);
        if (existing == null) throw new IllegalArgumentException("模板不存在: " + id);
        if (draft.getName() != null) existing.setName(draft.getName());
        if (draft.getDescription() != null) existing.setDescription(draft.getDescription());
        if (draft.getQuotaCalls() != null) existing.setQuotaCalls(draft.getQuotaCalls());
        if (draft.getQuotaDays() != null) existing.setQuotaDays(draft.getQuotaDays());
        if (draft.getPriceCents() != null) existing.setPriceCents(draft.getPriceCents());
        if (draft.getFeatures() != null) existing.setFeatures(toJson(draft.getFeatures()));
        if (draft.getLimits() != null) existing.setLimits(toJson(draft.getLimits()));
        if (draft.getIsPublic() != null) existing.setIsPublic(draft.getIsPublic());
        if (draft.getIsActive() != null) existing.setIsActive(draft.getIsActive());
        existing.setVersion(existing.getVersion() + 1);
        mapper.updateById(existing);
        log.info("[LicenseTemplate] 更新 id={}, version={}", id, existing.getVersion());
        return existing;
    }

    /**
     * 克隆模板
     */
    public LicenseTemplate clone(Long id, String newKey) {
        LicenseTemplate src = mapper.findById(id);
        if (src == null) throw new IllegalArgumentException("源模板不存在: " + id);
        LicenseTemplateDraft d = LicenseTemplateDraft.builder()
                .templateKey(newKey)
                .name(src.getName() + " (副本)")
                .licenseType(src.getLicenseType())
                .description(src.getDescription())
                .quotaCalls(src.getQuotaCalls())
                .quotaDays(src.getQuotaDays())
                .priceCents(src.getPriceCents())
                .features(fromJsonList(src.getFeatures()))
                .limits(fromJsonMap(src.getLimits()))
                .isPublic(src.getIsPublic())
                .isActive(src.getIsActive())
                .createdBy(src.getCreatedBy())
                .build();
        return create(d);
    }

    /**
     * 软删除 (isActive=0)
     */
    public boolean deactivate(Long id) {
        LicenseTemplate t = mapper.findById(id);
        if (t == null) return false;
        t.setIsActive(0);
        t.setVersion(t.getVersion() + 1);
        mapper.updateById(t);
        log.info("[LicenseTemplate] 下架 id={}", id);
        return true;
    }

    /**
     * 查
     */
    public LicenseTemplate get(Long id) {
        return mapper.findById(id);
    }

    public LicenseTemplate getByKey(String key) {
        return mapper.findByKey(key);
    }

    public List<LicenseTemplate> listByType(String type) {
        return mapper.listByType(type);
    }

    public List<LicenseTemplate> listPublic() {
        return mapper.listPublic();
    }

    /**
     * 签发 license (从模板生成 ModelLicense 字段)
     *
     * @return IssuedLicense (含 quotaCalls / expiresAt / features / limits)
     */
    public IssuedLicense issue(String templateKey, Long userId) {
        LicenseTemplate t = mapper.findByKey(templateKey);
        if (t == null) throw new IllegalArgumentException("模板不存在: " + templateKey);
        if (t.getIsActive() == 0) throw new IllegalStateException("模板已下架: " + templateKey);
        IssuedLicense lic = new IssuedLicense();
        lic.licenseType = t.getLicenseType();
        lic.quotaCalls = t.getQuotaCalls();
        lic.quotaDays = t.getQuotaDays();
        lic.expiresAt = t.getQuotaDays() > 0
                ? System.currentTimeMillis() + t.getQuotaDays() * 86_400_000L
                : 0L;  // 0 = 永久
        lic.features = fromJsonList(t.getFeatures());
        lic.limits = fromJsonMap(t.getLimits());
        lic.priceCents = t.getPriceCents();
        lic.templateKey = t.getTemplateKey();
        lic.templateVersion = t.getVersion();
        lic.userId = userId;
        log.info("[LicenseTemplate] 签发 userId={}, templateKey={}, quota={}, days={}",
                userId, templateKey, lic.quotaCalls, lic.quotaDays);
        return lic;
    }

    /**
     * 模板对比 (差异分析)
     */
    public Map<String, Object> compare(List<String> templateKeys) {
        List<LicenseTemplate> list = new ArrayList<>();
        for (String k : templateKeys) {
            LicenseTemplate t = mapper.findByKey(k);
            if (t != null) list.add(t);
        }
        // 收集所有特性
        Set<String> allFeatures = new LinkedHashSet<>();
        for (LicenseTemplate t : list) allFeatures.addAll(fromJsonList(t.getFeatures()));
        // 输出对比
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String f : allFeatures) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("feature", f);
            for (LicenseTemplate t : list) {
                row.put(t.getTemplateKey(), fromJsonList(t.getFeatures()).contains(f));
            }
            rows.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templates", templateKeys);
        result.put("featureMatrix", rows);
        return result;
    }

    /**
     * 模板统计
     */
    public Map<String, Object> stats() {
        List<LicenseTemplate> all = mapper.listPublic();
        Map<String, Long> byType = new HashMap<>();
        long totalQuota = 0;
        long totalPrice = 0;
        int activeCount = 0;
        for (LicenseTemplate t : all) {
            byType.merge(t.getLicenseType(), 1L, Long::sum);
            totalQuota += t.getQuotaCalls();
            totalPrice += t.getPriceCents();
            if (t.getIsActive() == 1) activeCount++;
        }
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("total", all.size());
        s.put("active", activeCount);
        s.put("byType", byType);
        s.put("totalQuotaCalls", totalQuota);
        s.put("totalPriceCents", totalPrice);
        return s;
    }

    // ----- JSON helper -----

    private String toJson(Object o) {
        if (o == null) return null;
        try { return json.writeValueAsString(o); }
        catch (Exception e) { throw new RuntimeException("JSON 序列化失败", e); }
    }

    private List<String> fromJsonList(String s) {
        if (s == null || s.isEmpty()) return new ArrayList<>();
        try { return json.readValue(s, new TypeReference<List<String>>() {}); }
        catch (Exception e) { return new ArrayList<>(); }
    }

    private Map<String, Object> fromJsonMap(String s) {
        if (s == null || s.isEmpty()) return new HashMap<>();
        try { return json.readValue(s, new TypeReference<Map<String, Object>>() {}); }
        catch (Exception e) { return new HashMap<>(); }
    }

    /**
     * 创建草稿
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LicenseTemplateDraft {
        private String templateKey;
        private String name;
        private String licenseType;
        private String description;
        private Long quotaCalls;
        private Integer quotaDays;
        private Long priceCents;
        private List<String> features;
        private Map<String, Object> limits;
        private Integer isPublic;
        private Integer isActive;
        private Long createdBy;
    }

    /**
     * 签发的 license
     */
    @lombok.Data
    public static class IssuedLicense {
        public String templateKey;
        public Integer templateVersion;
        public String licenseType;
        public Long quotaCalls;
        public Integer quotaDays;
        public Long expiresAt;       // ms, 0=永久
        public List<String> features;
        public Map<String, Object> limits;
        public Long priceCents;
        public Long userId;
    }
}
