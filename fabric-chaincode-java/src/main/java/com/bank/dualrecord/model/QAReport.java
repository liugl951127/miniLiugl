package com.bank.dualrecord.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * 质检报告
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QAReport {

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("verdict")
    private QAVerdict verdict;

    @JsonProperty("totalScore")
    private double totalScore;

    @JsonProperty("completeness")
    private double completeness;  // 完整度

    @JsonProperty("compliance")
    private double compliance;  // 合规度

    @JsonProperty("clarity")
    private double clarity;  // 清晰度

    @JsonProperty("riskDisclosure")
    private double riskDisclosure;  // 风险揭示充分度

    @JsonProperty("missingNodes")
    private java.util.List<String> missingNodes;

    @JsonProperty("riskItems")
    private java.util.List<String> riskItems;

    @JsonProperty("aiComments")
    private String aiComments;

    @JsonProperty("createdAt")
    private Instant createdAt;

    public QAReport() {}

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public QAVerdict getVerdict() { return verdict; }
    public void setVerdict(QAVerdict verdict) { this.verdict = verdict; }

    public double getTotalScore() { return totalScore; }
    public void setTotalScore(double totalScore) { this.totalScore = totalScore; }

    public double getCompleteness() { return completeness; }
    public void setCompleteness(double completeness) { this.completeness = completeness; }

    public double getCompliance() { return compliance; }
    public void setCompliance(double compliance) { this.compliance = compliance; }

    public double getClarity() { return clarity; }
    public void setClarity(double clarity) { this.clarity = clarity; }

    public double getRiskDisclosure() { return riskDisclosure; }
    public void setRiskDisclosure(double riskDisclosure) { this.riskDisclosure = riskDisclosure; }

    public java.util.List<String> getMissingNodes() { return missingNodes; }
    public void setMissingNodes(java.util.List<String> missingNodes) { this.missingNodes = missingNodes; }

    public java.util.List<String> getRiskItems() { return riskItems; }
    public void setRiskItems(java.util.List<String> riskItems) { this.riskItems = riskItems; }

    public String getAiComments() { return aiComments; }
    public void setAiComments(String aiComments) { this.aiComments = aiComments; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
