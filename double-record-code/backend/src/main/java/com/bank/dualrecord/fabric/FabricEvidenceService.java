package com.bank.dualrecord.fabric;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Contract;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 证据上链服务
 *
 * <p>封装与 Fabric 链码 EvidenceContract 的交互
 * <p>对应 Java 链码方法:
 * <ul>
 *   <li>submitEvidence
 *   <li>queryEvidence
 *   <li>verifyEvidence
 *   <li>updateState
 *   <li>finalizeEvidence
 *   <li>getEvidenceHistory
 *   <li>queryByCustomer
 *   <li>queryByProduct
 *   <li>registerPublicKey
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FabricEvidenceService {

    private final FabricGatewayManager gateway;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 提交证据
     */
    public String submitEvidence(Map<String, Object> evidence) throws Exception {
        Contract contract = gateway.getEvidenceContract();
        String json = objectMapper.writeValueAsString(evidence);
        byte[] result = contract.submitTransaction("submitEvidence", json);
        String orderId = new String(result, StandardCharsets.UTF_8);
        log.info("证据已上链: orderId={}", orderId);
        return orderId;
    }

    /**
     * 查询证据
     */
    public Map<String, Object> queryEvidence(String orderId) throws Exception {
        Contract contract = gateway.getEvidenceContract();
        byte[] result = contract.evaluateTransaction("queryEvidence", orderId);
        return objectMapper.readValue(result, Map.class);
    }

    /**
     * 验证证据
     */
    public Map<String, Object> verifyEvidence(String orderId, String videoHash, String audioHash, String contractHash) throws Exception {
        Contract contract = gateway.getEvidenceContract();
        byte[] result = contract.evaluateTransaction("verifyEvidence", orderId, videoHash, audioHash, contractHash);
        return objectMapper.readValue(result, Map.class);
    }

    /**
     * 状态流转
     */
    public String updateState(String orderId, String newState, String reason) throws Exception {
        Contract contract = gateway.getEvidenceContract();
        byte[] result = contract.submitTransaction("updateState", orderId, newState, reason);
        return new String(result, StandardCharsets.UTF_8);
    }

    /**
     * 终结证据
     */
    public void finalizeEvidence(String orderId, String merkleRoot) throws Exception {
        Contract contract = gateway.getEvidenceContract();
        contract.submitTransaction("finalizeEvidence", orderId, merkleRoot);
        log.info("证据已终结: orderId={}, merkleRoot={}", orderId, merkleRoot);
    }

    /**
     * 追加节点结果
     */
    public void appendNodeResult(String orderId, Map<String, Object> nodeResult) throws Exception {
        Contract contract = gateway.getEvidenceContract();
        String json = objectMapper.writeValueAsString(nodeResult);
        contract.submitTransaction("appendNodeResult", orderId, json);
    }

    /**
     * 查询证据历史
     */
    public String getEvidenceHistory(String orderId) throws Exception {
        Contract contract = gateway.getEvidenceContract();
        byte[] result = contract.evaluateTransaction("getEvidenceHistory", orderId);
        return new String(result, StandardCharsets.UTF_8);
    }

    /**
     * 按客户查询
     */
    public String queryByCustomer(String customerId, int pageSize, String bookmark) throws Exception {
        Contract contract = gateway.getEvidenceContract();
        byte[] result = contract.evaluateTransaction("queryByCustomer", customerId, String.valueOf(pageSize), bookmark == null ? "" : bookmark);
        return new String(result, StandardCharsets.UTF_8);
    }

    /**
     * 按产品查询
     */
    public String queryByProduct(int productType, int pageSize, String bookmark) throws Exception {
        Contract contract = gateway.getEvidenceContract();
        byte[] result = contract.evaluateTransaction("queryByProduct", String.valueOf(productType), String.valueOf(pageSize), bookmark == null ? "" : bookmark);
        return new String(result, StandardCharsets.UTF_8);
    }

    /**
     * 注册公钥
     */
    public void registerPublicKey(String partyType, String partyId, String publicKeyHex) throws Exception {
        Contract contract = gateway.getEvidenceContract();
        contract.submitTransaction("registerPublicKey", partyType, partyId, publicKeyHex);
        log.info("公钥已注册: {}/{}", partyType, partyId);
    }

    /**
     * 健康检查
     */
    public boolean healthCheck() {
        try {
            Contract contract = gateway.getEvidenceContract();
            contract.evaluateTransaction("queryEvidence", "__HEALTH_CHECK__");
            return true;
        } catch (Exception e) {
            // 查不到不算错,说明链码响应了
            String msg = e.getMessage() == null ? "" : e.getMessage();
            return msg.contains("订单不存在") || msg.contains("not found");
        }
    }
}
