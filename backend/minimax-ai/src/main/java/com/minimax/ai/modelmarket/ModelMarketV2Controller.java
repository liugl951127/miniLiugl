package com.minimax.ai.modelmarket;

import com.minimax.ai.entity.*;
import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 模型市场 v2 REST API (V3.3.2)
 *
 * <p>API 列表 (统一 /api/v1/ai/market-v2 前缀):
 * <ul>
 *   <li>POST   /version/publish          发布新版本 (multipart)</li>
 *   <li>GET    /version/list/{entryId}   列版本</li>
 *   <li>GET    /version/latest/{entryId} 最新版本</li>
 *   <li>POST   /version/{vid}/deprecate  弃用版本</li>
 *   <li>POST   /license/purchase         购买 license</li>
 *   <li>POST   /license/renew            续费</li>
 *   <li>POST   /license/refund           退款</li>
 *   <li>GET    /license/list             用户的 license</li>
 *   <li>GET    /license/{key}            查 license</li>
 *   <li>POST   /license/check            鉴权 + 扣次</li>
 *   <li>GET    /billing/list             计费历史</li>
 *   <li>GET    /billing/summary          计费汇总</li>
 * </ul>
 */
@Tag(name = "模型市场 v2")
@RestController
@RequestMapping("/api/v1/ai/market-v2")
@RequiredArgsConstructor
public class ModelMarketV2Controller {

    private final ModelMarketV2Service service;

    /**
     * 发布新版本
     */
    @Operation(summary = "发布新版本")
    @PostMapping("/version/publish")
    public Result<ModelVersion> publishVersion(@RequestParam Long modelEntryId,
                                                 @RequestParam String version,
                                                 @RequestParam(required = false, defaultValue = "") String changelog,
                                                 @RequestParam("file") MultipartFile file,
                                                 @RequestParam(required = false) String uploaderId) {
        try {
            ModelVersion v = service.publishVersion(modelEntryId, version, changelog, file.getBytes(), uploaderId);
            return Result.ok(v);
        } catch (Exception e) {
            return Result.fail(500, "发布失败: " + e.getMessage());
        }
    }

    /**
     * 列版本
     */
    @Operation(summary = "列模型版本")
    @GetMapping("/version/list/{entryId}")
    public Result<List<ModelVersion>> listVersions(@PathVariable Long entryId) {
        return Result.ok(service.listVersions(entryId));
    }

    /**
     * 最新版本
     */
    @Operation(summary = "最新版本")
    @GetMapping("/version/latest/{entryId}")
    public Result<ModelVersion> latestVersion(@PathVariable Long entryId) {
        ModelVersion v = service.latestVersion(entryId);
        if (v == null) return Result.fail(404, "无版本");
        return Result.ok(v);
    }

    /**
     * 弃用
     */
    @Operation(summary = "弃用版本")
    @PostMapping("/version/{versionId}/deprecate")
    public Result<Void> deprecate(@PathVariable String versionId) {
        service.deprecate(versionId);
        return Result.ok();
    }

    /**
     * 购买 license
     */
    @Operation(summary = "购买 license")
    @PostMapping("/license/purchase")
    public Result<ModelLicense> purchase(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        Long modelEntryId = ((Number) body.get("modelEntryId")).longValue();
        Long modelVersionId = body.get("modelVersionId") == null ? null : ((Number) body.get("modelVersionId")).longValue();
        String licenseType = (String) body.getOrDefault("licenseType", "PERSONAL");
        long quotaCalls = ((Number) body.getOrDefault("quotaCalls", 1000)).longValue();
        long priceCents = ((Number) body.getOrDefault("priceCents", 0)).longValue();
        int days = ((Number) body.getOrDefault("days", 30)).intValue();
        return Result.ok(service.purchaseLicense(userId, modelEntryId, modelVersionId, licenseType, quotaCalls, priceCents, days));
    }

    /**
     * 续费
     */
    @Operation(summary = "续费")
    @PostMapping("/license/renew")
    public Result<ModelLicense> renew(@RequestBody Map<String, Object> body) {
        String licenseKey = (String) body.get("licenseKey");
        int days = ((Number) body.getOrDefault("days", 30)).intValue();
        long priceCents = ((Number) body.getOrDefault("priceCents", 0)).longValue();
        ModelLicense lic = service.renewLicense(licenseKey, days, priceCents);
        if (lic == null) return Result.fail(404, "license 不存在");
        return Result.ok(lic);
    }

    /**
     * 退款
     */
    @Operation(summary = "退款")
    @PostMapping("/license/refund")
    public Result<Void> refund(@RequestBody Map<String, String> body) {
        String licenseKey = body.get("licenseKey");
        String reason = body.getOrDefault("reason", "用户申请");
        boolean ok = service.refundLicense(licenseKey, reason);
        return ok ? Result.ok() : Result.fail(404, "license 不存在或非 ACTIVE");
    }

    /**
     * 用户的 license
     */
    @Operation(summary = "用户的 license")
    @GetMapping("/license/list")
    public Result<List<ModelLicense>> userLicenses(@RequestParam Long userId) {
        return Result.ok(service.userLicenses(userId));
    }

    /**
     * 查 license
     */
    @Operation(summary = "查 license")
    @GetMapping("/license/{key}")
    public Result<ModelLicense> findLicense(@PathVariable String key) {
        ModelLicense lic = service.findLicense(key);
        if (lic == null) return Result.fail(404, "不存在");
        return Result.ok(lic);
    }

    /**
     * 鉴权 + 扣次
     */
    @Operation(summary = "鉴权 + 扣次")
    @PostMapping("/license/check")
    public Result<Map<String, Object>> checkAndUse(@RequestBody Map<String, String> body) {
        String licenseKey = body.get("licenseKey");
        boolean ok = service.checkAndUse(licenseKey);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("allowed", ok);
        return Result.ok(out);
    }

    /**
     * 计费历史
     */
    @Operation(summary = "计费历史")
    @GetMapping("/billing/list")
    public Result<List<BillingRecord>> billingList(@RequestParam Long userId,
                                                    @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(service.userBilling(userId, limit));
    }

    /**
     * 计费汇总
     */
    @Operation(summary = "计费汇总")
    @GetMapping("/billing/summary")
    public Result<Map<String, Object>> billingSummary(@RequestParam Long userId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalSpend", service.userTotalSpend(userId));
        out.put("usageCost", service.userUsageCost(userId));
        out.put("userId", userId);
        return Result.ok(out);
    }
}
