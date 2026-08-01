package com.bank.dualrecord.controller;

import com.bank.dualrecord.dto.ApiResponse;
import com.bank.dualrecord.fabric.FabricAuditService;
import com.bank.dualrecord.fabric.FabricContractService;
import com.bank.dualrecord.fabric.FabricEvidenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 区块链存证 Controller
 *
 * <p>封装与 Java 链码(fabric-chaincode-java)的交互
 */
@Slf4j
@RestController
@RequestMapping("/api/chain")
@RequiredArgsConstructor
@Tag(name = "区块链存证", description = "Fabric 链码调用接口")
public class ChainController {

    private final FabricEvidenceService evidenceService;
    private final FabricContractService contractService;
    private final FabricAuditService auditService;

    // ============================================================
    // 证据
    // ============================================================

    @PostMapping("/evidence/submit")
    @Operation(summary = "提交证据上链")
    public ApiResponse<Map<String, String>> submitEvidence(@RequestBody Map<String, Object> evidence) {
        try {
            String orderId = evidenceService.submitEvidence(evidence);
            return ApiResponse.ok(Map.of("orderId", orderId, "status", "submitted"));
        } catch (Exception e) {
            log.error("提交证据失败", e);
            return ApiResponse.fail(500, "提交失败: " + e.getMessage());
        }
    }

    @GetMapping("/evidence/{orderId}")
    @Operation(summary = "查询链上证据")
    public ApiResponse<Map<String, Object>> queryEvidence(@PathVariable String orderId) {
        try {
            return ApiResponse.ok(evidenceService.queryEvidence(orderId));
        } catch (Exception e) {
            return ApiResponse.fail(404, "证据不存在: " + e.getMessage());
        }
    }

    @PostMapping("/evidence/verify")
    @Operation(summary = "验证链上证据")
    public ApiResponse<Map<String, Object>> verifyEvidence(@RequestBody Map<String, String> body) {
        try {
            return ApiResponse.ok(evidenceService.verifyEvidence(
                body.get("orderId"),
                body.get("videoHash"),
                body.get("audioHash"),
                body.get("contractHash")
            ));
        } catch (Exception e) {
            return ApiResponse.fail(500, "验证失败: " + e.getMessage());
        }
    }

    @GetMapping("/evidence/{orderId}/history")
    @Operation(summary = "查询证据历史")
    public ApiResponse<String> getHistory(@PathVariable String orderId) {
        try {
            return ApiResponse.ok(evidenceService.getEvidenceHistory(orderId));
        } catch (Exception e) {
            return ApiResponse.fail(500, e.getMessage());
        }
    }

    // ============================================================
    // 合同
    // ============================================================

    @PostMapping("/contract/submit")
    @Operation(summary = "提交合同存证")
    public ApiResponse<Map<String, String>> submitContract(@RequestBody Map<String, Object> contract) {
        try {
            String contractId = contractService.generateContract(contract);
            return ApiResponse.ok(Map.of("contractId", contractId, "status", "generated"));
        } catch (Exception e) {
            return ApiResponse.fail(500, e.getMessage());
        }
    }

    @PostMapping("/contract/sign")
    @Operation(summary = "签署合同")
    public ApiResponse<Void> signContract(@RequestBody Map<String, String> body) {
        try {
            contractService.signContract(
                body.get("contractId"),
                Integer.parseInt(body.getOrDefault("signMethod", "1")),
                body.getOrDefault("signCert", ""),
                body.get("sm2Sig")
            );
            return ApiResponse.ok();
        } catch (Exception e) {
            return ApiResponse.fail(500, e.getMessage());
        }
    }

    // ============================================================
    // 审计
    // ============================================================

    @GetMapping("/audit/{orderId}")
    @Operation(summary = "查询订单审计历史")
    public ApiResponse<String> getAuditHistory(@PathVariable String orderId) {
        try {
            return ApiResponse.ok(auditService.queryOrderAudits(orderId));
        } catch (Exception e) {
            return ApiResponse.fail(500, e.getMessage());
        }
    }

    // ============================================================
    // 公钥管理
    // ============================================================

    @PostMapping("/publickey/register")
    @Operation(summary = "注册公钥")
    public ApiResponse<Void> registerPublicKey(@RequestBody Map<String, String> body) {
        try {
            evidenceService.registerPublicKey(
                body.get("partyType"),
                body.get("partyId"),
                body.get("publicKeyHex")
            );
            return ApiResponse.ok();
        } catch (Exception e) {
            return ApiResponse.fail(500, e.getMessage());
        }
    }

    // ============================================================
    // 健康检查
    // ============================================================

    @GetMapping("/health")
    @Operation(summary = "链码健康检查")
    public ApiResponse<Map<String, Object>> health() {
        boolean ok = evidenceService.healthCheck();
        return ApiResponse.ok(Map.of(
            "status", ok ? "UP" : "DOWN",
            "channel", "dual-record-channel",
            "chaincode", "dual-record-chaincode"
        ));
    }
}
