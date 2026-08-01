package com.bank.dualrecord.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * 话术节点执行结果
 * 用于断点续传 + Merkle 根聚合
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeResult {

    @JsonProperty("nodeCode")
    private String nodeCode;

    @JsonProperty("nodeName")
    private String nodeName;

    @JsonProperty("result")
    private String result;  // PASS / FAIL / SKIP

    @JsonProperty("duration")
    private int duration;  // 毫秒

    @JsonProperty("customerSaid")
    private String customerSaid;

    @JsonProperty("keywordsHit")
    private java.util.List<String> keywordsHit;

    @JsonProperty("asrConfidence")
    private double asrConfidence;

    @JsonProperty("startedAt")
    private Instant startedAt;

    @JsonProperty("endedAt")
    private Instant endedAt;

    @JsonProperty("hash")
    private String hash;  // SM3 节点指纹

    public NodeResult() {}

    public String getNodeCode() { return nodeCode; }
    public void setNodeCode(String nodeCode) { this.nodeCode = nodeCode; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getCustomerSaid() { return customerSaid; }
    public void setCustomerSaid(String customerSaid) { this.customerSaid = customerSaid; }

    public java.util.List<String> getKeywordsHit() { return keywordsHit; }
    public void setKeywordsHit(java.util.List<String> keywordsHit) { this.keywordsHit = keywordsHit; }

    public double getAsrConfidence() { return asrConfidence; }
    public void setAsrConfidence(double asrConfidence) { this.asrConfidence = asrConfidence; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
}
