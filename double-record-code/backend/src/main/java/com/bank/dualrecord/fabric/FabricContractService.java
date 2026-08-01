package com.bank.dualrecord.fabric;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Contract;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 合同存证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FabricContractService {

    private final FabricGatewayManager gateway;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateContract(Map<String, Object> contract) throws Exception {
        Contract c = gateway.getContractContract();
        String json = objectMapper.writeValueAsString(contract);
        byte[] result = c.submitTransaction("generateContract", json);
        return new String(result, StandardCharsets.UTF_8);
    }

    public void signContract(String contractId, int signMethod, String signCert, String sm2Sig) throws Exception {
        Contract c = gateway.getContractContract();
        c.submitTransaction("signContract", contractId, String.valueOf(signMethod), signCert, sm2Sig);
    }

    public void voidContract(String contractId, String reason) throws Exception {
        Contract c = gateway.getContractContract();
        c.submitTransaction("voidContract", contractId, reason);
    }

    public Map<String, Object> queryContract(String contractId) throws Exception {
        Contract c = gateway.getContractContract();
        byte[] result = c.evaluateTransaction("queryContract", contractId);
        return objectMapper.readValue(result, Map.class);
    }

    public String queryByOrder(String orderId) throws Exception {
        Contract c = gateway.getContractContract();
        byte[] result = c.evaluateTransaction("queryByOrder", orderId);
        return new String(result, StandardCharsets.UTF_8);
    }

    public String verifyContract(String contractId, String fileHash) throws Exception {
        Contract c = gateway.getContractContract();
        byte[] result = c.evaluateTransaction("verifyContract", contractId, fileHash);
        return new String(result, StandardCharsets.UTF_8);
    }
}
