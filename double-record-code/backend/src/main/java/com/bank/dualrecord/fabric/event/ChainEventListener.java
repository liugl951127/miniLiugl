package com.bank.dualrecord.fabric.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * 链码事件(标准化)
 *
 * <p>对应 Java 链码中的 SetEvent 调用:
 * <pre>
 *   ctx.getStub().setEvent("EvidenceSubmitted", payload);
 * </pre>
 *
 * <p>事件格式:Kafka topic = fabric.{channelName}.{eventName}
 * <p>分区键:orderId(保证同一订单事件顺序)
 *
 * @author Mavis
 */
@Slf4j
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChainEventListener {

    /** 事件名(EvidenceSubmitted / StateChanged / ContractSigned / AuditRecorded / Finalized) */
    @JsonProperty("eventName")
    private String eventName;

    /** 链上交易 ID */
    @JsonProperty("txId")
    private String txId;

    /** 关联订单 */
    @JsonProperty("orderId")
    private String orderId;

    /** 关联会话 */
    @JsonProperty("sessionId")
    private String sessionId;

    /** 通道 ID */
    @JsonProperty("channelId")
    private String channelId;

    /** 链 ID */
    @JsonProperty("chaincodeId")
    private String chaincodeId;

    /** 区块号 */
    @JsonProperty("blockNumber")
    private long blockNumber;

    /** 事件载荷(原始 JSON) */
    @JsonProperty("payload")
    private String payload;

    /** 事件时间(从链码读取) */
    @JsonProperty("eventTime")
    private Instant eventTime;

    /** 接收时间(本服务) */
    @JsonProperty("receivedAt")
    private Instant receivedAt = Instant.now();

    /**
     * 解析原始 payload(JSON 字符串)为对象
     */
    public <T> T parsePayload(Class<T> clazz) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            om.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return om.readValue(payload, clazz);
        } catch (Exception e) {
            log.error("解析事件载荷失败: {}", e.getMessage());
            return null;
        }
    }

    public byte[] toBytes() {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            om.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return om.writeValueAsBytes(this);
        } catch (Exception e) {
            log.error("序列化失败", e);
            return new byte[0];
        }
    }

    public static ChainEventListener fromBytes(byte[] data) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            om.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return om.readValue(new String(data, StandardCharsets.UTF_8), ChainEventListener.class);
        } catch (Exception e) {
            log.error("反序列化失败", e);
            return null;
        }
    }
}
