package com.bank.dualrecord.contract;

import com.bank.dualrecord.model.Contract;
import com.bank.dualrecord.util.JsonUtil;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 电子合同存证合约(带事件 nonce)
 */
@Contract(
    name = "ContractContract",
    info = @Info(
        title = "电子合同存证合约",
        description = "合同 PDF + 签名 + 上链 + 事件 nonce 防重放",
        version = "1.1.0",
        license = @License(name = "Apache-2.0")
    )
)
public class ContractContract implements ContractInterface {

    private static final Logger log = LoggerFactory.getLogger(ContractContract.class);
    private static final String IDX_PUBKEY = "PUBKEY~";
    private static final String IDX_ORDER = "CONTRACT_ORDER~";

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String generateContract(@Default() Context ctx, String contractJson) {
        Contract contract = JsonUtil.fromJson(contractJson, Contract.class);
        if (!com.bank.dualrecord.util.HashUtil.isValidHash(contract.getFileHash())) {
            throw new IllegalArgumentException("无效的合同哈希");
        }

        byte[] existing = ctx.getStub().getState(contract.getContractId());
        if (existing != null && existing.length > 0) {
            throw new IllegalStateException("合同已存在: " + contract.getContractId());
        }

        contract.setStatus(0);
        contract.setCreatedAt(Instant.now());
        contract.setSm3Hash(com.bank.dualrecord.crypto.SM3Util.hashHex(contract.getFileHash()));

        ctx.getStub().putState(contract.getContractId(),
            JsonUtil.toJson(contract).getBytes(StandardCharsets.UTF_8));
        addToOrderIndex(ctx, contract.getOrderId(), contract.getContractId());

        emitEvent(ctx, "ContractGenerated", contract);
        return contract.getContractId();
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String signContract(@Default() Context ctx, String contractId, int signMethod, String signCert, String sm2Sig) {
        Contract contract = queryContract(ctx, contractId);
        if (contract == null) {
            throw new IllegalArgumentException("合同不存在: " + contractId);
        }
        if (contract.getStatus() != 0) {
            throw new IllegalStateException("合同状态非待签: " + contract.getStatus());
        }

        byte[] pubKeyBytes = ctx.getStub().getState(IDX_PUBKEY + "CUSTOMER~" + contract.getOrderId());
        if (pubKeyBytes != null) {
            String plaintext = contract.getFileHash() + contract.getOrderId();
            boolean valid = com.bank.dualrecord.crypto.SM2Util.verify(
                new String(pubKeyBytes, StandardCharsets.UTF_8),
                plaintext.getBytes(StandardCharsets.UTF_8), sm2Sig);
            if (!valid) {
                throw new IllegalStateException("SM2 签名验证失败");
            }
        }

        contract.setStatus(1);
        contract.setSignMethod(signMethod);
        contract.setSignCert(signCert);
        contract.setSm2Signature(sm2Sig);
        contract.setSignedAt(Instant.now());
        contract.setTrustTime(Instant.now());

        ctx.getStub().putState(contractId, JsonUtil.toJson(contract).getBytes(StandardCharsets.UTF_8));
        emitEvent(ctx, "ContractSigned", contract);
        return "OK";
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String voidContract(@Default() Context ctx, String contractId, String reason) {
        Contract contract = queryContract(ctx, contractId);
        if (contract == null) {
            throw new IllegalArgumentException("合同不存在");
        }
        if (contract.getStatus() == 2) {
            throw new IllegalStateException("合同已作废");
        }
        contract.setStatus(2);
        ctx.getStub().putState(contractId, JsonUtil.toJson(contract).getBytes(StandardCharsets.UTF_8));
        emitEvent(ctx, "ContractVoided", contract);
        return "OK";
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Contract queryContract(@Default() Context ctx, String contractId) {
        byte[] bytes = ctx.getStub().getState(contractId);
        if (bytes == null || bytes.length == 0) return null;
        return JsonUtil.fromJson(new String(bytes, StandardCharsets.UTF_8), Contract.class);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryByOrder(@Default() Context ctx, String orderId) {
        byte[] idx = ctx.getStub().getState(IDX_ORDER + orderId);
        if (idx == null) return "[]";
        String[] ids = new String(idx, StandardCharsets.UTF_8).split(",");
        List<Contract> list = new ArrayList<>();
        for (String id : ids) {
            Contract c = queryContract(ctx, id);
            if (c != null) list.add(c);
        }
        return JsonUtil.toJson(list);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String verifyContract(@Default() Context ctx, String contractId, String fileHash) {
        Contract c = queryContract(ctx, contractId);
        if (c == null) throw new IllegalArgumentException("合同不存在");

        Map<String, Object> r = new HashMap<>();
        r.put("contractId", contractId);
        r.put("fileValid", c.getFileHash().equalsIgnoreCase(fileHash));
        r.put("sm3Valid", c.getSm3Hash() != null && c.getSm3Hash().equalsIgnoreCase(
            com.bank.dualrecord.crypto.SM3Util.hashHex(fileHash)));
        r.put("signed", c.getStatus() == 1);
        r.put("signMethod", c.getSignMethod());
        r.put("sm2Signature", c.getSm2Signature());
        r.put("trustTime", c.getTrustTime());
        return JsonUtil.toJson(r);
    }

    private void addToOrderIndex(Context ctx, String orderId, String contractId) {
        String key = IDX_ORDER + orderId;
        byte[] idx = ctx.getStub().getState(key);
        List<String> ids = new ArrayList<>();
        if (idx != null && idx.length > 0) {
            for (String s : new String(idx, StandardCharsets.UTF_8).split(",")) {
                if (!s.isEmpty()) ids.add(s);
            }
        }
        ids.add(contractId);
        ctx.getStub().putState(key, String.join(",", ids).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 发送事件 - 带 nonce
     */
    private void emitEvent(Context ctx, String eventName, Object payload) {
        String nonce = UUID.randomUUID().toString();
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("nonce", nonce);
        envelope.put("eventName", eventName);
        envelope.put("txId", ctx.getStub().getTxId());
        envelope.put("timestamp", Instant.now().toString());
        envelope.put("data", payload);
        ctx.getStub().setEvent(eventName, JsonUtil.toJson(envelope).getBytes(StandardCharsets.UTF_8));
    }
}
