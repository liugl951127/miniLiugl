package com.bank.dualrecord.fabric;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Contract;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 审计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FabricAuditService {

    private final FabricGatewayManager gateway;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String recordAudit(Map<String, Object> audit) throws Exception {
        Contract c = gateway.getAuditContract();
        String json = objectMapper.writeValueAsString(audit);
        byte[] result = c.submitTransaction("recordAudit", json);
        return new String(result, StandardCharsets.UTF_8);
    }

    public String queryOrderAudits(String orderId) throws Exception {
        Contract c = gateway.getAuditContract();
        byte[] result = c.evaluateTransaction("queryOrderAudits", orderId);
        return new String(result, StandardCharsets.UTF_8);
    }

    public String queryByAction(String action, int pageSize) throws Exception {
        Contract c = gateway.getAuditContract();
        byte[] result = c.evaluateTransaction("queryByAction", action, String.valueOf(pageSize));
        return new String(result, StandardCharsets.UTF_8);
    }
}
