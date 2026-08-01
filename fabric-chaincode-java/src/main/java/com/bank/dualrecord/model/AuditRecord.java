package com.bank.dualrecord.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * 审计记录 - 只增不改
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditRecord {

    @JsonProperty("id")
    private String id;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("action")
    private String action;

    @JsonProperty("operator")
    private String operator;

    @JsonProperty("operatorOrg")
    private String operatorOrg;

    @JsonProperty("oldState")
    private OrderState oldState;

    @JsonProperty("newState")
    private OrderState newState;

    @JsonProperty("oldValue")
    private String oldValue;

    @JsonProperty("newValue")
    private String newValue;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("evidenceHash")
    private String evidenceHash;

    @JsonProperty("ipAddress")
    private String ipAddress;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("txId")
    private String txId;

    @JsonProperty("blockNum")
    private long blockNum;

    public AuditRecord() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getOperatorOrg() { return operatorOrg; }
    public void setOperatorOrg(String operatorOrg) { this.operatorOrg = operatorOrg; }

    public OrderState getOldState() { return oldState; }
    public void setOldState(OrderState oldState) { this.oldState = oldState; }

    public OrderState getNewState() { return newState; }
    public void setNewState(OrderState newState) { this.newState = newState; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getEvidenceHash() { return evidenceHash; }
    public void setEvidenceHash(String evidenceHash) { this.evidenceHash = evidenceHash; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getTxId() { return txId; }
    public void setTxId(String txId) { this.txId = txId; }

    public long getBlockNum() { return blockNum; }
    public void setBlockNum(long blockNum) { this.blockNum = blockNum; }
}
