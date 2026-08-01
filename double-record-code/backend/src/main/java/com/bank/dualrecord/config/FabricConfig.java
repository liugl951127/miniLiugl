package com.bank.dualrecord.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Fabric 区块链配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "fabric")
public class FabricConfig {
    /** 是否启用 Fabric */
    private boolean enabled = true;
    /** MSP ID */
    private String mspId = "BankMSP";
    /** 通道名 */
    private String channel = "dual-record-channel";
    /** 链码名 */
    private String chaincode = "dual-record-chaincode";
    /** connection-profile 路径 */
    private String networkConfigPath = "classpath:fabric/connection-profile.yaml";
    /** 钱包路径 */
    private String walletPath = "./wallet";
    /** 身份标签 */
    private String identityLabel = "admin";
    /** 事件监听 */
    private EventListener eventListener = new EventListener();

    @Data
    public static class EventListener {
        private boolean enabled = true;
        private long pollInterval = 1000L;
    }
}
