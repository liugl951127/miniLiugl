package com.bank.dualrecord.contract;

import com.bank.dualrecord.crypto.SM2Util;
import com.bank.dualrecord.model.Evidence;
import com.bank.dualrecord.model.OrderState;
import com.bank.dualrecord.model.ProductType;
import com.bank.dualrecord.util.JsonUtil;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 证据合约单元测试
 *
 * <p>使用内存账本 + Mockito 模拟 ChaincodeStub
 */
class EvidenceContractTest {

    private EvidenceContract contract;
    private Context ctx;
    private ChaincodeStub stub;
    private Map<String, byte[]> ledger;

    @BeforeEach
    void setUp() {
        contract = new EvidenceContract();
        ctx = mock(Context.class);
        stub = mock(ChaincodeStub.class);
        ledger = new ConcurrentHashMap<>();

        when(ctx.getStub()).thenReturn(stub);
        when(stub.getTxId()).thenReturn("test-tx-001");
        when(stub.getChannelId()).thenReturn("testchannel");
        when(stub.getTxTimestamp()).thenReturn(Instant.now());

        // 内存账本
        when(stub.getState(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            return ledger.get(key);
        });
        doAnswer(inv -> {
            String key = inv.getArgument(0);
            byte[] value = inv.getArgument(1);
            ledger.put(key, value);
            return null;
        }).when(stub).putState(anyString(), any(byte[].class));

        // 事件
        doNothing().when(stub).setEvent(anyString(), any(byte[].class));

        // 客户端身份
        org.hyperledger.fabric.contract.ClientIdentity cid = mock(org.hyperledger.fabric.contract.ClientIdentity.class);
        when(cid.getMspId()).thenReturn("TestOrgMSP");
        when(cid.getId()).thenReturn("CN=test-user");
        when(ctx.getClientIdentity()).thenReturn(cid);
    }

    /**
     * 构造测试证据
     */
    private Evidence makeEvidence() {
        Evidence e = new Evidence();
        e.setOrderId("ORD20260801000001");
        e.setCustomerId("C001");
        e.setProductType(ProductType.INSURANCE);
        e.setProductName("XX 寿险");
        e.setAmount(5000000);
        e.setVideoHash("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
        e.setAudioHash("3e23e8160039594a33894f6564e1b1348bbd7a0088d42c4acb73eeaed59c009d");
        e.setContractHash("b3a8e0e1f9c18a6d54c9b65a0c5e0a3b2c1d4e5f6789abcdef0123456789abcd");
        e.setChannel("H5");
        e.setSalesUserId("M001");
        e.setBranchId("2001");
        e.setScriptVersion("V3.2");
        return e;
    }

    @Test
    void testSubmitEvidence_Success() {
        Evidence e = makeEvidence();
        String json = JsonUtil.toJson(e);

        String result = contract.submitEvidence(ctx, json);
        assertEquals("ORD20260801000001", result);

        // 验证已写入
        byte[] stored = ledger.get("ORD20260801000001");
        assertNotNull(stored, "证据未写入账本");
        Evidence parsed = JsonUtil.fromJson(new String(stored, StandardCharsets.UTF_8), Evidence.class);
        assertEquals(OrderState.VERIFIED, parsed.getState());
        assertEquals("test-tx-001", parsed.getTxId());
    }

    @Test
    void testSubmitEvidence_Duplicate() {
        Evidence e = makeEvidence();
        String json = JsonUtil.toJson(e);
        contract.submitEvidence(ctx, json);

        assertThrows(IllegalStateException.class, () -> contract.submitEvidence(ctx, json));
    }

    @Test
    void testSubmitEvidence_InvalidOrderId() {
        Evidence e = makeEvidence();
        e.setOrderId("BAD-ID");
        assertThrows(IllegalArgumentException.class,
            () -> contract.submitEvidence(ctx, JsonUtil.toJson(e)));
    }

    @Test
    void testSubmitEvidence_InvalidHash() {
        Evidence e = makeEvidence();
        e.setVideoHash("invalid");
        assertThrows(IllegalArgumentException.class,
            () -> contract.submitEvidence(ctx, JsonUtil.toJson(e)));
    }

    @Test
    void testUpdateState_Valid() {
        Evidence e = makeEvidence();
        contract.submitEvidence(ctx, JsonUtil.toJson(e));

        String result = contract.updateState(ctx, e.getOrderId(), "SCRIPTING", "开始话术");
        assertEquals("SCRIPTING", result);
    }

    @Test
    void testUpdateState_InvalidTransition() {
        Evidence e = makeEvidence();
        contract.submitEvidence(ctx, JsonUtil.toJson(e));

        // VERIFIED -> COMPLETED(跳跃,不允许)
        assertThrows(IllegalStateException.class,
            () -> contract.updateState(ctx, e.getOrderId(), "COMPLETED", "测试"));
    }

    @Test
    void testUpdateState_NotFound() {
        assertThrows(IllegalArgumentException.class,
            () -> contract.updateState(ctx, "ORD-UNKNOWN", "SCRIPTING", "test"));
    }

    @Test
    void testQueryEvidence_Found() {
        Evidence e = makeEvidence();
        contract.submitEvidence(ctx, JsonUtil.toJson(e));
        Evidence queried = contract.queryEvidence(ctx, e.getOrderId());
        assertNotNull(queried);
        assertEquals(e.getOrderId(), queried.getOrderId());
    }

    @Test
    void testQueryEvidence_NotFound() {
        Evidence queried = contract.queryEvidence(ctx, "ORD-UNKNOWN");
        assertNull(queried);
    }

    @Test
    void testVerifyEvidence_Valid() {
        Evidence e = makeEvidence();
        contract.submitEvidence(ctx, JsonUtil.toJson(e));
        String result = contract.verifyEvidence(ctx, e.getOrderId(),
            e.getVideoHash(), e.getAudioHash(), e.getContractHash());
        assertTrue(result.contains("\"verified\":true"));
    }

    @Test
    void testVerifyEvidence_Tampered() {
        Evidence e = makeEvidence();
        contract.submitEvidence(ctx, JsonUtil.toJson(e));
        String tampered = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
        String result = contract.verifyEvidence(ctx, e.getOrderId(),
            tampered, e.getAudioHash(), e.getContractHash());
        assertTrue(result.contains("\"verified\":false"));
        assertTrue(result.contains("\"videoValid\":false"));
    }

    @Test
    void testRegisterPublicKey() {
        SM2Util.SM2KeyPair kp = SM2Util.generateKeyPairHex();
        String result = contract.registerPublicKey(ctx, "CUSTOMER", "C001", kp.getPublicKeyHex());
        assertEquals("OK", result);
    }
}
