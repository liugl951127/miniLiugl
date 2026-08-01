package com.bank.dualrecord.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * 证据包 - 一次双录会话的完整存证
 * <p>
 * 关键设计:
 * - 链上只存指纹,不存原数据
 * - 多方 SM2 签名 + 国家授时时间戳
 * - 节点结果用 Merkle 根聚合
 *
 * @author Mavis
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Evidence {

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("productType")
    private ProductType productType;

    @JsonProperty("productName")
    private String productName;

    @JsonProperty("amount")
    private long amount;

    @JsonProperty("videoHash")
    private String videoHash;

    @JsonProperty("audioHash")
    private String audioHash;

    @JsonProperty("videoSm3Hash")
    private String videoSm3Hash;

    @JsonProperty("contractHash")
    private String contractHash;

    @JsonProperty("scriptHash")
    private String scriptHash;

    @JsonProperty("scriptVersion")
    private String scriptVersion;

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("channel")
    private String channel;

    @JsonProperty("salesUserId")
    private String salesUserId;

    @JsonProperty("branchId")
    private String branchId;

    // 多方签名
    @JsonProperty("customerSm2Signature")
    private String customerSm2Signature;

    @JsonProperty("managerSm2Signature")
    private String managerSm2Signature;

    @JsonProperty("witnessSm2Signature")
    private String witnessSm2Signature;

    // 可信时间
    @JsonProperty("trustTimestamp")
    private Instant trustTimestamp;

    // Merkle 根(节点结果聚合)
    @JsonProperty("nodeResultsMerkle")
    private String nodeResultsMerkle;

    // 提交方信息
    @JsonProperty("submitterOrg")
    private String submitterOrg;

    @JsonProperty("submitterCert")
    private String submitterCert;

    // 区块信息(系统填充)
    @JsonProperty("txId")
    private String txId;

    @JsonProperty("blockNum")
    private long blockNum;

    @JsonProperty("blockHash")
    private String blockHash;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @JsonProperty("state")
    private OrderState state;

    @JsonProperty("isArchived")
    private boolean archived;

    // ============================================================
    // 构造方法
    // ============================================================
    public Evidence() {
    }

    // ============================================================
    // Getters and Setters
    // ============================================================
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public String getVideoHash() { return videoHash; }
    public void setVideoHash(String videoHash) { this.videoHash = videoHash; }

    public String getAudioHash() { return audioHash; }
    public void setAudioHash(String audioHash) { this.audioHash = audioHash; }

    public String getVideoSm3Hash() { return videoSm3Hash; }
    public void setVideoSm3Hash(String videoSm3Hash) { this.videoSm3Hash = videoSm3Hash; }

    public String getContractHash() { return contractHash; }
    public void setContractHash(String contractHash) { this.contractHash = contractHash; }

    public String getScriptHash() { return scriptHash; }
    public void setScriptHash(String scriptHash) { this.scriptHash = scriptHash; }

    public String getScriptVersion() { return scriptVersion; }
    public void setScriptVersion(String scriptVersion) { this.scriptVersion = scriptVersion; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getSalesUserId() { return salesUserId; }
    public void setSalesUserId(String salesUserId) { this.salesUserId = salesUserId; }

    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }

    public String getCustomerSm2Signature() { return customerSm2Signature; }
    public void setCustomerSm2Signature(String customerSm2Signature) { this.customerSm2Signature = customerSm2Signature; }

    public String getManagerSm2Signature() { return managerSm2Signature; }
    public void setManagerSm2Signature(String managerSm2Signature) { this.managerSm2Signature = managerSm2Signature; }

    public String getWitnessSm2Signature() { return witnessSm2Signature; }
    public void setWitnessSm2Signature(String witnessSm2Signature) { this.witnessSm2Signature = witnessSm2Signature; }

    public Instant getTrustTimestamp() { return trustTimestamp; }
    public void setTrustTimestamp(Instant trustTimestamp) { this.trustTimestamp = trustTimestamp; }

    public String getNodeResultsMerkle() { return nodeResultsMerkle; }
    public void setNodeResultsMerkle(String nodeResultsMerkle) { this.nodeResultsMerkle = nodeResultsMerkle; }

    public String getSubmitterOrg() { return submitterOrg; }
    public void setSubmitterOrg(String submitterOrg) { this.submitterOrg = submitterOrg; }

    public String getSubmitterCert() { return submitterCert; }
    public void setSubmitterCert(String submitterCert) { this.submitterCert = submitterCert; }

    public String getTxId() { return txId; }
    public void setTxId(String txId) { this.txId = txId; }

    public long getBlockNum() { return blockNum; }
    public void setBlockNum(long blockNum) { this.blockNum = blockNum; }

    public String getBlockHash() { return blockHash; }
    public void setBlockHash(String blockHash) { this.blockHash = blockHash; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public OrderState getState() { return state; }
    public void setState(OrderState state) { this.state = state; }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    @Override
    public String toString() {
        return "Evidence{orderId=" + orderId + ", state=" + state + ", txId=" + txId + "}";
    }
}
