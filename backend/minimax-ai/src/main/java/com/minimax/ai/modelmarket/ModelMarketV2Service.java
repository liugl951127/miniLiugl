package com.minimax.ai.modelmarket;

import com.minimax.ai.entity.*;
import com.minimax.ai.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 模型市场 v2 服务 (V3.3.2)
 *
 * <p>升级: 模型版本 + License + 计费
 *
 * <h3>核心流程</h3>
 * <pre>
 *   1. 发布模型 → createVersion() → ModelVersion (status=PUBLISHED)
 *   2. 用户购买 → purchaseLicense() → ModelLicense (status=ACTIVE) + BillingRecord (USAGE)
 *   3. 调用模型 → checkLicense() → 扣 usedCalls
 *   4. 续费/退款 → renewLicense() / refundLicense()
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelMarketV2Service {

    private final ModelVersionMapper versionMapper;
    private final ModelLicenseMapper licenseMapper;
    private final BillingRecordMapper billingMapper;

    // ============= 版本管理 =============

    /**
     * 发布新版本
     */
    public ModelVersion publishVersion(Long modelEntryId, String version, String changelog,
                                        byte[] fileBytes, String uploaderId) {
        // 1. 上传文件 (实际生产写到文件系统)
        String filePath = "/var/minimax/models/" + modelEntryId + "/" + version + ".bin";
        String sha256 = sha256(fileBytes);
        // 2. 创建版本
        ModelVersion v = new ModelVersion();
        v.setVersionId("v-" + UUID.randomUUID().toString().substring(0, 8));
        v.setModelEntryId(modelEntryId);
        v.setVersion(version);
        v.setChangelog(changelog);
        v.setFilePath(filePath);
        v.setSizeBytes((long) fileBytes.length);
        v.setSha256(sha256);
        v.setStatus("PUBLISHED");
        v.setIsLatest(true);
        v.setUploaderId(uploaderId == null ? null : Long.valueOf(uploaderId));
        versionMapper.insert(v);
        // 3. 取消旧版本 latest
        versionMapper.setLatest(v.getId(), modelEntryId);
        log.info("[market-v2] 发布版本: entry={}, version={}, size={} bytes", modelEntryId, version, fileBytes.length);
        return v;
    }

    /**
     * 弃用版本
     */
    public void deprecate(String versionId) {
        versionMapper.updateStatus(versionId, "DEPRECATED");
    }

    public List<ModelVersion> listVersions(Long modelEntryId) { return versionMapper.findByEntry(modelEntryId); }
    public ModelVersion latestVersion(Long modelEntryId) { return versionMapper.findLatest(modelEntryId); }

    // ============= License 管理 =============

    /**
     * 购买 license
     */
    public ModelLicense purchaseLicense(Long userId, Long modelEntryId, Long modelVersionId,
                                          String licenseType, long quotaCalls, long priceCents, int days) {
        // 1. 创建 license
        ModelLicense lic = new ModelLicense();
        lic.setLicenseKey("lic-" + UUID.randomUUID().toString().substring(0, 8));
        lic.setModelEntryId(modelEntryId);
        lic.setModelVersionId(modelVersionId);
        lic.setUserId(userId);
        lic.setLicenseType(licenseType);
        lic.setStatus("ACTIVE");
        lic.setQuotaCalls(quotaCalls);
        lic.setUsedCalls(0L);
        lic.setStartAt(LocalDateTime.now());
        lic.setExpireAt(days > 0 ? LocalDateTime.now().plusDays(days) : null);
        lic.setPriceCents(priceCents);
        licenseMapper.insert(lic);
        // 2. 创建计费记录
        BillingRecord bill = new BillingRecord();
        bill.setRecordId("br-" + UUID.randomUUID().toString().substring(0, 8));
        bill.setUserId(userId);
        bill.setLicenseId(lic.getId());
        bill.setModelEntryId(modelEntryId);
        bill.setRecordType("PURCHASE");
        bill.setAmountCents(priceCents);
        bill.setCurrency("CNY");
        bill.setStatus("SUCCESS");
        bill.setPaymentMethod("internal");
        bill.setDescription("购买 " + licenseType + " license, " + quotaCalls + " 调用配额");
        billingMapper.insert(bill);
        log.info("[market-v2] 购买 license: user={}, entry={}, type={}, price={}分",
                userId, modelEntryId, licenseType, priceCents);
        return lic;
    }

    /**
     * 续费
     */
    public ModelLicense renewLicense(String licenseKey, int days, long priceCents) {
        ModelLicense lic = licenseMapper.findByKey(licenseKey);
        if (lic == null) return null;
        // 续期: 在现有过期时间上 + days
        LocalDateTime base = lic.getExpireAt() == null ? LocalDateTime.now() : lic.getExpireAt();
        lic.setExpireAt(base.plusDays(days));
        lic.setStatus("ACTIVE");
        licenseMapper.updateById(lic);
        // 计费
        BillingRecord bill = new BillingRecord();
        bill.setRecordId("br-" + UUID.randomUUID().toString().substring(0, 8));
        bill.setUserId(lic.getUserId());
        bill.setLicenseId(lic.getId());
        bill.setRecordType("RENEW");
        bill.setAmountCents(priceCents);
        bill.setCurrency("CNY");
        bill.setStatus("SUCCESS");
        bill.setPaymentMethod("internal");
        bill.setDescription("续费 " + days + " 天");
        billingMapper.insert(bill);
        return lic;
    }

    /**
     * 退款
     */
    public boolean refundLicense(String licenseKey, String reason) {
        ModelLicense lic = licenseMapper.findByKey(licenseKey);
        if (lic == null || !"ACTIVE".equals(lic.getStatus())) return false;
        // 1. 标 REVOKED
        lic.setStatus("REVOKED");
        licenseMapper.updateById(lic);
        // 2. 退款记录
        BillingRecord bill = new BillingRecord();
        bill.setRecordId("br-" + UUID.randomUUID().toString().substring(0, 8));
        bill.setUserId(lic.getUserId());
        bill.setLicenseId(lic.getId());
        bill.setRecordType("REFUND");
        bill.setAmountCents(-lic.getPriceCents());
        bill.setCurrency("CNY");
        bill.setStatus("SUCCESS");
        bill.setPaymentMethod("internal");
        bill.setDescription("退款: " + reason);
        billingMapper.insert(bill);
        return true;
    }

    public List<ModelLicense> userLicenses(Long userId) { return licenseMapper.findActiveByUser(userId); }
    public ModelLicense findLicense(String licenseKey) { return licenseMapper.findByKey(licenseKey); }

    /**
     * 鉴权 + 扣次
     */
    public boolean checkAndUse(String licenseKey) {
        ModelLicense lic = licenseMapper.findByKey(licenseKey);
        if (lic == null) return false;
        if (!"ACTIVE".equals(lic.getStatus())) return false;
        if (lic.getExpireAt() != null && lic.getExpireAt().isBefore(LocalDateTime.now())) {
            lic.setStatus("EXPIRED");
            licenseMapper.updateById(lic);
            return false;
        }
        // 配额检查
        if (lic.getQuotaCalls() > 0 && lic.getUsedCalls() >= lic.getQuotaCalls()) {
            return false;
        }
        // 扣次
        return licenseMapper.incrementUsage(lic.getId()) > 0;
    }

    // ============= 计费查询 =============

    public List<BillingRecord> userBilling(Long userId, int limit) { return billingMapper.findByUser(userId, limit); }
    public long userTotalSpend(Long userId) { Long n = billingMapper.sumByUser(userId); return n == null ? 0 : n; }
    public long userUsageCost(Long userId) { Long n = billingMapper.sumUsageByUser(userId); return n == null ? 0 : n; }

    // ============= 内部工具 =============

    private String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "(error)";
        }
    }
}
