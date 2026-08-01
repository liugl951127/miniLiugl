package com.bank.dualrecord.contract;

import com.bank.dualrecord.crypto.MerkleUtil;
import com.bank.dualrecord.crypto.SM2Util;
import com.bank.dualrecord.model.Evidence;
import com.bank.dualrecord.model.NodeResult;
import com.bank.dualrecord.model.OrderState;
import com.bank.dualrecord.util.ContextUtil;
import com.bank.dualrecord.util.HashUtil;
import com.bank.dualrecord.util.JsonUtil;
import com.bank.dualrecord.util.StateMachine;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 证据上链合约
 *
 * <p>负责双录业务证据包(视频/音频/合同指纹)上链存证
 * <p>支持:提交、状态流转、节点结果追加、终结归档、多维查询
 *
 * @author Mavis
 */
@Contract(
    name = "EvidenceContract",
    info = @Info(
        title = "双录证据上链合约",
        description = "证据指纹 + 多方 SM2 签名 + 国家授时上链存证",
        version = "1.0.0",
        license = @License(name = "Apache-2.0"),
        contact = @Contact(email = "blockchain@bank.com", name = "区块链团队")
    )
)
public class EvidenceContract implements ContractInterface {

    private static final Logger log = LoggerFactory.getLogger(EvidenceContract.class);

    // 索引前缀
    private static final String IDX_CUSTOMER = "CUSTOMER~";
    private static final String IDX_PRODUCT = "PRODUCT~";
    private static final String IDX_CHANNEL = "CHANNEL~";
    private static final String IDX_TIME = "TIME~";
    private static final String IDX_NODE = "NODE~";
    private static final String IDX_PUBKEY = "PUBKEY~";

    // ============================================================
    // 提交证据
    // ============================================================

    /**
     * 提交证据上链
     * 背书策略:2 节点背书
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String submitEvidence(@Default() Context ctx, String evidenceJson) {
        log.info("submitEvidence invoked, len={}", evidenceJson == null ? 0 : evidenceJson.length());

        // 1. 解析
        Evidence evidence = JsonUtil.fromJson(evidenceJson, Evidence.class);
        validate(evidence);

        // 2. 幂等性
        ChaincodeStub stub = ctx.getStub();
        byte[] existing = stub.getState(evidence.getOrderId());
        if (existing != null && existing.length > 0) {
            throw new IllegalStateException("订单 " + evidence.getOrderId() + " 证据已存在");
        }

        // 3. 验签(任一签名无效则拒绝)
        if (evidence.getCustomerSm2Signature() != null && !evidence.getCustomerSm2Signature().isEmpty()) {
            verifySignature(ctx, "CUSTOMER", evidence.getCustomerId(),
                evidence.getOrderId() + evidence.getVideoHash(),
                evidence.getCustomerSm2Signature());
        }
        if (evidence.getManagerSm2Signature() != null && !evidence.getManagerSm2Signature().isEmpty()) {
            verifySignature(ctx, "MANAGER", evidence.getSalesUserId(),
                evidence.getOrderId() + evidence.getVideoHash(),
                evidence.getManagerSm2Signature());
        }

        // 4. 补充系统字段
        evidence.setTxId(ContextUtil.getTxId(ctx));
        evidence.setState(OrderState.VERIFIED);
        evidence.setArchived(false);
        Instant now = ContextUtil.getCurrentTime(ctx);
        evidence.setCreatedAt(now);
        evidence.setUpdatedAt(now);
        evidence.setSubmitterOrg(ContextUtil.getSubmitterMspId(ctx));
        if (evidence.getSubmitterCert() == null) {
            evidence.setSubmitterCert(ContextUtil.getSubmitterCN(ctx));
        }

        // 5. 写入
        stub.putState(evidence.getOrderId(), JsonUtil.toJson(evidence).getBytes(StandardCharsets.UTF_8));

        // 6. 索引
        addToIndex(stub, IDX_CUSTOMER + evidence.getCustomerId(), evidence.getOrderId());
        addToIndex(stub, IDX_PRODUCT + evidence.getProductType().getCode(), evidence.getOrderId());
        addToIndex(stub, IDX_CHANNEL + evidence.getChannel(), evidence.getOrderId());
        addToIndex(stub, IDX_TIME + now.toString().substring(0, 10).replace("-", ""), evidence.getOrderId());

        // 7. 事件
        stub.setEvent("EvidenceSubmitted", JsonUtil.toJson(evidence).getBytes(StandardCharsets.UTF_8));

        log.info("证据 {} 已上链,TxID={}", evidence.getOrderId(), evidence.getTxId());
        return evidence.getOrderId();
    }

    /**
     * 状态机流转
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String updateState(@Default() Context ctx, String orderId, String newStateName, String reason) {
        log.info("updateState: {} -> {}, reason={}", orderId, newStateName, reason);

        Evidence evidence = queryEvidence(ctx, orderId);
        if (evidence == null) {
            throw new IllegalArgumentException("订单不存在: " + orderId);
        }

        OrderState newState = OrderState.fromName(newStateName);
        if (!StateMachine.isValidTransition(evidence.getState(), newState)) {
            throw new IllegalStateException("非法状态流转: " + evidence.getState() + " -> " + newState);
        }

        OrderState oldState = evidence.getState();
        evidence.setState(newState);
        evidence.setUpdatedAt(ContextUtil.getCurrentTime(ctx));

        ctx.getStub().putState(orderId, JsonUtil.toJson(evidence).getBytes(StandardCharsets.UTF_8));

        // 审计
        recordAuditInternal(ctx, orderId, "UPDATE_STATE", oldState, newState, reason);

        // 事件
        Map<String, String> evt = new HashMap<>();
        evt.put("orderId", orderId);
        evt.put("oldState", oldState.name());
        evt.put("newState", newState.name());
        evt.put("reason", reason == null ? "" : reason);
        ctx.getStub().setEvent("StateChanged", JsonUtil.toJson(evt).getBytes(StandardCharsets.UTF_8));

        return newState.name();
    }

    /**
     * 追加话术节点结果(支持断点续传)
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String appendNodeResult(@Default() Context ctx, String orderId, String nodeResultJson) {
        NodeResult result = JsonUtil.fromJson(nodeResultJson, NodeResult.class);
        if (!HashUtil.isValidHash(result.getHash())) {
            throw new IllegalArgumentException("无效的节点哈希");
        }

        String key = IDX_NODE + orderId + "~" + result.getNodeCode();
        ctx.getStub().putState(key, nodeResultJson.getBytes(StandardCharsets.UTF_8));

        // 事件
        Map<String, String> evt = new HashMap<>();
        evt.put("orderId", orderId);
        evt.put("nodeCode", result.getNodeCode());
        evt.put("result", result.getResult());
        ctx.getStub().setEvent("NodeResultAppended", JsonUtil.toJson(evt).getBytes(StandardCharsets.UTF_8));

        return "OK";
    }

    /**
     * 终结证据(Merkle 根聚合)
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String finalizeEvidence(@Default() Context ctx, String orderId, String merkleRoot) {
        log.info("finalizeEvidence: {}, root={}", orderId, merkleRoot);

        Evidence evidence = queryEvidence(ctx, orderId);
        if (evidence == null) {
            throw new IllegalArgumentException("订单不存在: " + orderId);
        }
        if (evidence.getState() != OrderState.QA_PASSED) {
            throw new IllegalStateException("订单状态非 QA_PASSED,无法终结: " + evidence.getState());
        }
        if (!HashUtil.isValidHash(merkleRoot)) {
            throw new IllegalArgumentException("无效的 Merkle 根");
        }

        evidence.setNodeResultsMerkle(merkleRoot);
        evidence.setState(OrderState.COMPLETED);
        evidence.setArchived(true);
        evidence.setUpdatedAt(ContextUtil.getCurrentTime(ctx));

        ctx.getStub().putState(orderId, JsonUtil.toJson(evidence).getBytes(StandardCharsets.UTF_8));

        // 审计
        recordAuditInternal(ctx, orderId, "FINALIZE", OrderState.QA_PASSED, OrderState.COMPLETED, "归档锁定");

        ctx.getStub().setEvent("EvidenceFinalized", JsonUtil.toJson(evidence).getBytes(StandardCharsets.UTF_8));
        return "OK";
    }

    // ============================================================
    // 查询(免费)
    // ============================================================

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Evidence queryEvidence(@Default() Context ctx, String orderId) {
        byte[] bytes = ctx.getStub().getState(orderId);
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return JsonUtil.fromJson(new String(bytes, StandardCharsets.UTF_8), Evidence.class);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryEvidenceJson(@Default() Context ctx, String orderId) {
        Evidence e = queryEvidence(ctx, orderId);
        return e == null ? null : JsonUtil.toJson(e);
    }

    /**
     * 验证证据(链下数据 vs 链上指纹)
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String verifyEvidence(@Default() Context ctx, String orderId, String videoHash, String audioHash, String contractHash) {
        Evidence evidence = queryEvidence(ctx, orderId);
        if (evidence == null) {
            throw new IllegalArgumentException("订单不存在: " + orderId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("videoValid", evidence.getVideoHash().equalsIgnoreCase(videoHash));
        result.put("audioValid", evidence.getAudioHash().equalsIgnoreCase(audioHash));
        result.put("contractValid", evidence.getContractHash().equalsIgnoreCase(contractHash));
        result.put("verified",
            evidence.getVideoHash().equalsIgnoreCase(videoHash) &&
            evidence.getAudioHash().equalsIgnoreCase(audioHash) &&
            evidence.getContractHash().equalsIgnoreCase(contractHash));
        result.put("txId", evidence.getTxId());
        result.put("createdAt", evidence.getCreatedAt());
        result.put("trustTimestamp", evidence.getTrustTimestamp());
        result.put("state", evidence.getState());
        Map<String, String> sigs = new HashMap<>();
        sigs.put("customer", evidence.getCustomerSm2Signature());
        sigs.put("manager", evidence.getManagerSm2Signature());
        sigs.put("witness", evidence.getWitnessSm2Signature());
        result.put("signatures", sigs);
        return JsonUtil.toJson(result);
    }

    /**
     * 查询订单的所有历史
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getEvidenceHistory(@Default() Context ctx, String orderId) {
        List<Map<String, Object>> history = new ArrayList<>();
        ctx.getStub().getHistoryForKey(orderId).forEach(mod -> {
            if (!mod.isDeleted()) {
                Map<String, Object> item = new HashMap<>();
                item.put("txId", mod.getTxId());
                item.put("timestamp", mod.getTimestamp());
                item.put("value", new String(mod.getValue(), StandardCharsets.UTF_8));
                history.add(item);
            }
        });
        return JsonUtil.toJson(history);
    }

    /**
     * 按客户查询(返回 JSON 数组)
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryByCustomer(@Default() Context ctx, String customerId, int pageSize, String bookmark) {
        return queryByIndex(ctx, IDX_CUSTOMER + customerId, pageSize, bookmark);
    }

    /**
     * 按产品查询
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryByProduct(@Default() Context ctx, int productType, int pageSize, String bookmark) {
        return queryByIndex(ctx, IDX_PRODUCT + productType, pageSize, bookmark);
    }

    // ============================================================
    // 公钥管理
    // ============================================================

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String registerPublicKey(@Default() Context ctx, String partyType, String partyId, String publicKeyHex) {
        if (publicKeyHex == null || publicKeyHex.length() < 64) {
            throw new IllegalArgumentException("无效的公钥");
        }
        String key = IDX_PUBKEY + partyType + "~" + partyId;
        ctx.getStub().putState(key, publicKeyHex.getBytes(StandardCharsets.UTF_8));
        return "OK";
    }

    // ============================================================
    // 内部方法
    // ============================================================

    private void validate(Evidence evidence) {
        if (evidence == null) {
            throw new IllegalArgumentException("证据为空");
        }
        if (evidence.getOrderId() == null || !com.bank.dualrecord.util.IDUtil.isValidOrderId(evidence.getOrderId())) {
            throw new IllegalArgumentException("无效的订单号: " + evidence.getOrderId());
        }
        if (!HashUtil.isValidHash(evidence.getVideoHash())) {
            throw new IllegalArgumentException("无效的视频哈希");
        }
        if (!HashUtil.isValidHash(evidence.getAudioHash())) {
            throw new IllegalArgumentException("无效的音频哈希");
        }
        if (!HashUtil.isValidHash(evidence.getContractHash())) {
            throw new IllegalArgumentException("无效的合同哈希");
        }
        if (evidence.getProductType() == null) {
            throw new IllegalArgumentException("产品类型不能为空");
        }
    }

    private void verifySignature(Context ctx, String partyType, String partyId, String plaintext, String signatureHex) {
        String key = IDX_PUBKEY + partyType + "~" + partyId;
        byte[] pubKeyBytes = ctx.getStub().getState(key);
        if (pubKeyBytes == null) {
            throw new IllegalStateException("公钥未注册: " + partyType + "/" + partyId);
        }
        boolean valid = SM2Util.verify(new String(pubKeyBytes, StandardCharsets.UTF_8),
            plaintext.getBytes(StandardCharsets.UTF_8), signatureHex);
        if (!valid) {
            throw new IllegalStateException(partyType + " SM2 签名验证失败: " + partyId);
        }
    }

    private void addToIndex(ChaincodeStub stub, String indexKey, String orderId) {
        byte[] idxBytes = stub.getState(indexKey);
        List<String> orderIds = new ArrayList<>();
        if (idxBytes != null && idxBytes.length > 0) {
            orderIds = new ArrayList<>(Arrays.asList(
                new String(idxBytes, StandardCharsets.UTF_8).split(",")));
        }
        if (!orderIds.contains(orderId)) {
            orderIds.add(orderId);
            stub.putState(indexKey, String.join(",", orderIds).getBytes(StandardCharsets.UTF_8));
        }
    }

    private String queryByIndex(Context ctx, String indexKey, int pageSize, String bookmark) {
        byte[] idxBytes = ctx.getStub().getState(indexKey);
        if (idxBytes == null || idxBytes.length == 0) {
            return "{\"items\":[],\"bookmark\":\"\"}";
        }
        String[] orderIds = new String(idxBytes, StandardCharsets.UTF_8).split(",");
        int start = 0;
        if (bookmark != null && !bookmark.isEmpty()) {
            try { start = Integer.parseInt(bookmark); } catch (Exception ignored) {}
        }
        int end = Math.min(start + pageSize, orderIds.length);

        List<Evidence> results = new ArrayList<>();
        for (int i = start; i < end; i++) {
            Evidence e = queryEvidence(ctx, orderIds[i]);
            if (e != null) results.add(e);
        }
        String newBookmark = end >= orderIds.length ? "" : String.valueOf(end);
        Map<String, Object> resp = new HashMap<>();
        resp.put("items", results);
        resp.put("bookmark", newBookmark);
        return JsonUtil.toJson(resp);
    }

    private void recordAuditInternal(Context ctx, String orderId, String action, OrderState oldState, OrderState newState, String reason) {
        Map<String, Object> audit = new HashMap<>();
        audit.put("id", java.util.UUID.randomUUID().toString().replace("-", ""));
        audit.put("orderId", orderId);
        audit.put("action", action);
        audit.put("operator", ContextUtil.getSubmitterCN(ctx));
        audit.put("operatorOrg", ContextUtil.getSubmitterMspId(ctx));
        audit.put("oldState", oldState);
        audit.put("newState", newState);
        audit.put("reason", reason);
        audit.put("timestamp", ContextUtil.getCurrentTime(ctx));
        audit.put("txId", ContextUtil.getTxId(ctx));

        String key = "AUDIT~" + orderId + "~" + audit.get("id");
        ctx.getStub().putState(key, JsonUtil.toJson(audit).getBytes(StandardCharsets.UTF_8));
    }
}
