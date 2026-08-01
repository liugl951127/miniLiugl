package com.bank.dualrecord.contract;

import com.bank.dualrecord.model.Evidence;
import com.bank.dualrecord.model.OrderState;
import com.bank.dualrecord.model.ProductType;
import com.bank.dualrecord.util.JsonUtil;
import com.bank.dualrecord.util.SM3Util;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 链码事件 nonce 防重放测试
 *
 * <p>DRL-2026-005 回归测试
 */
class EvidenceContractNonceTest {

    private EvidenceContract contract;
    private Context ctx;
    private ChaincodeStub stub;

    @BeforeEach
    void setup() {
        contract = new EvidenceContract();
        ctx = mock(Context.class);
        stub = mock(ChaincodeStub.class);
        when(ctx.getStub()).thenReturn(stub);
        when(stub.getTxId()).thenReturn("test-tx-" + System.currentTimeMillis());
    }

    @Test
    void testSubmitEvidence_emitsEventWithNonce() {
        // 准备一个完整的 evidence JSON
        String sampleHash = SM3Util.hashHex("test-data");
        String evidenceJson = String.format("""
            {
              "orderId": "TEST2026080112000001",
              "customerId": "C001",
              "customerName": "张三",
              "customerIdCard": "110101199001011234",
              "productType": "INSURANCE",
              "productCode": "PRD001",
              "channel": "PAD",
              "salesUserId": "S001",
              "branchId": "B001",
              "videoHash": "%s",
              "audioHash": "%s",
              "contractHash": "%s",
              "videoSize": 104857600,
              "audioSize": 5242880,
              "contractSize": 1048576,
              "duration": 300,
              "nodeCode": "PAD-001",
              "nodeIp": "192.168.1.100",
              "tsaCertSerial": "TSA-SERIAL-001"
            }
            """, sampleHash, sampleHash, sampleHash);

        when(stub.getState(anyString())).thenReturn(new byte[0]);
        when(stub.getCreator()).thenReturn("CN=BANK-CA, OU=client");

        ArgumentCaptor<String> eventNameCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> eventPayloadCap = ArgumentCaptor.forClass(byte[].class);

        contract.submitEvidence(ctx, evidenceJson);

        // 验证调用了 setEvent 且 eventName 是 "EvidenceSubmitted"
        verify(stub, atLeastOnce()).setEvent(eventNameCap.capture(), eventPayloadCap.capture());

        // 至少有一个事件
        boolean hasSubmittedEvent = eventNameCap.getAllValues().contains("EvidenceSubmitted");
        assertTrue(hasSubmittedEvent, "应发送 EvidenceSubmitted 事件");

        // 验证事件载荷包含 nonce 字段
        byte[] payload = eventPayloadCap.getAllValues().stream()
            .filter(p -> new String(p, StandardCharsets.UTF_8).contains("EvidenceSubmitted"))
            .findFirst().orElseThrow();
        String payloadStr = new String(payload, StandardCharsets.UTF_8);
        assertTrue(payloadStr.contains("\"nonce\""), "事件载荷必须包含 nonce 字段");
        assertTrue(payloadStr.contains("\"eventName\":\"EvidenceSubmitted\""), "事件载荷必须包含 eventName");
        assertTrue(payloadStr.contains("\"txId\""), "事件载荷必须包含 txId");
        assertTrue(payloadStr.contains("\"timestamp\""), "事件载荷必须包含 timestamp");
    }

    @Test
    void testUpdateState_emitsEventWithNonce() {
        // 1. 先模拟已存在的 evidence
        Evidence existing = createSampleEvidence();
        existing.setState(OrderState.VERIFIED);
        when(stub.getState("TEST2026080112000002"))
            .thenReturn(JsonUtil.toJson(existing).getBytes(StandardCharsets.UTF_8));

        contract.updateState(ctx, "TEST2026080112000002", "QA_PASSED", "质检通过");

        // 验证 StateChanged 事件被发送,且 payload 含 nonce
        ArgumentCaptor<byte[]> payloadCap = ArgumentCaptor.forClass(byte[].class);
        verify(stub, atLeastOnce()).setEvent(eq("StateChanged"), payloadCap.capture());
        String payload = new String(payloadCap.getValue(), StandardCharsets.UTF_8);
        assertTrue(payload.contains("\"nonce\""));
        assertTrue(payload.contains("\"newState\":\"QA_PASSED\""));
    }

    @Test
    void testAppendNodeResult_emitsEventWithNonce() {
        String nodeResultJson = """
            {
              "nodeCode": "PAD-001",
              "result": "OK",
              "hash": "a1b2c3d4e5f67890abcdef1234567890abcdef1234567890abcdef1234567890",
              "timestamp": "2026-08-01T10:00:00Z"
            }
            """;

        contract.appendNodeResult(ctx, "TEST2026080112000003", nodeResultJson);

        ArgumentCaptor<byte[]> payloadCap = ArgumentCaptor.forClass(byte[].class);
        verify(stub).setEvent(eq("NodeResultAppended"), payloadCap.capture());
        String payload = new String(payloadCap.getValue(), StandardCharsets.UTF_8);
        assertTrue(payload.contains("\"nonce\""));
    }

    @Test
    void testEventNonce_isUniquePerCall() {
        String evidenceJson = makeEvidenceJson("TEST2026080112000099");
        when(stub.getState(anyString())).thenReturn(new byte[0]);
        when(stub.getCreator()).thenReturn("CN=BANK-CA");

        contract.submitEvidence(ctx, evidenceJson);

        // 提取 nonce,验证是 UUID 格式
        ArgumentCaptor<byte[]> payloadCap = ArgumentCaptor.forClass(byte[].class);
        verify(stub, atLeastOnce()).setEvent(anyString(), payloadCap.capture());
        String payload = new String(payloadCap.getValue(), StandardCharsets.UTF_8);
        assertTrue(payload.matches(".*\"nonce\":\"[a-f0-9-]{36}\".*"),
            "nonce 应为 UUID 格式,实际:" + payload);
    }

    private Evidence createSampleEvidence() {
        Evidence e = new Evidence();
        e.setOrderId("TEST2026080112000002");
        e.setCustomerId("C001");
        e.setProductType(ProductType.INSURANCE);
        e.setState(OrderState.VERIFIED);
        String h = "a1b2c3d4e5f67890abcdef1234567890abcdef1234567890abcdef1234567890";
        e.setVideoHash(h);
        e.setAudioHash(h);
        e.setContractHash(h);
        return e;
    }

    private String makeEvidenceJson(String orderId) {
        String h = "a1b2c3d4e5f67890abcdef1234567890abcdef1234567890abcdef1234567890";
        return String.format("""
            {
              "orderId": "%s",
              "customerId": "C001",
              "customerName": "张三",
              "customerIdCard": "110101199001011234",
              "productType": "INSURANCE",
              "productCode": "PRD001",
              "channel": "PAD",
              "salesUserId": "S001",
              "branchId": "B001",
              "videoHash": "%s",
              "audioHash": "%s",
              "contractHash": "%s",
              "videoSize": 104857600,
              "audioSize": 5242880,
              "contractSize": 1048576,
              "duration": 300,
              "nodeCode": "PAD-001",
              "nodeIp": "192.168.1.100",
              "tsaCertSerial": "TSA-SERIAL-001"
            }
            """, orderId, h, h, h);
    }
}
