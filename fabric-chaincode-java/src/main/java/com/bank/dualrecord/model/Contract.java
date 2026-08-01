package com.bank.dualrecord.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * 电子合同
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Contract {

    @JsonProperty("contractId")
    private String contractId;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("contractNo")
    private String contractNo;

    @JsonProperty("contractType")
    private int contractType;  // 1-主合同 2-附加 3-风险揭示书

    @JsonProperty("templateId")
    private String templateId;

    @JsonProperty("fileUrl")
    private String fileUrl;

    @JsonProperty("fileHash")
    private String fileHash;  // SHA-256

    @JsonProperty("sm3Hash")
    private String sm3Hash;  // SM3 二次校验

    @JsonProperty("fileSize")
    private int fileSize;

    @JsonProperty("signMethod")
    private int signMethod;  // 1-CA 2-手写 3-短信

    @JsonProperty("signCert")
    private String signCert;  // PEM

    @JsonProperty("sm2Signature")
    private String sm2Signature;

    @JsonProperty("trustTime")
    private Instant trustTime;

    @JsonProperty("signedAt")
    private Instant signedAt;

    @JsonProperty("status")
    private int status;  // 0-待签 1-已签 2-已撤销

    @JsonProperty("createdAt")
    private Instant createdAt;

    public Contract() {}

    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getContractNo() { return contractNo; }
    public void setContractNo(String contractNo) { this.contractNo = contractNo; }

    public int getContractType() { return contractType; }
    public void setContractType(int contractType) { this.contractType = contractType; }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }

    public String getSm3Hash() { return sm3Hash; }
    public void setSm3Hash(String sm3Hash) { this.sm3Hash = sm3Hash; }

    public int getFileSize() { return fileSize; }
    public void setFileSize(int fileSize) { this.fileSize = fileSize; }

    public int getSignMethod() { return signMethod; }
    public void setSignMethod(int signMethod) { this.signMethod = signMethod; }

    public String getSignCert() { return signCert; }
    public void setSignCert(String signCert) { this.signCert = signCert; }

    public String getSm2Signature() { return sm2Signature; }
    public void setSm2Signature(String sm2Signature) { this.sm2Signature = sm2Signature; }

    public Instant getTrustTime() { return trustTime; }
    public void setTrustTime(Instant trustTime) { this.trustTime = trustTime; }

    public Instant getSignedAt() { return signedAt; }
    public void setSignedAt(Instant signedAt) { this.signedAt = signedAt; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
