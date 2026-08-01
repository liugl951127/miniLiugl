package com.bank.dualrecord.fabric;

import com.bank.dualrecord.config.FabricConfig;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Fabric Gateway 管理器
 *
 * <p>负责:
 * <ul>
 *   <li>启动时连接 Gateway
 *   <li>获取 Network / Contract
 *   <li>优雅关闭
 * </ul>
 */
@Slf4j
@Component
public class FabricGatewayManager {

    @Autowired
    private FabricConfig fabricConfig;

    private Gateway gateway;
    private Network network;
    private Contract evidenceContract;
    private Contract contractContract;
    private Contract auditContract;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (!fabricConfig.isEnabled()) {
            log.warn("Fabric 区块链未启用,跳过连接");
            return;
        }
        try {
            connect();
            log.info("Fabric Gateway 已连接: channel={}, chaincode={}",
                fabricConfig.getChannel(), fabricConfig.getChaincode());
        } catch (Exception e) {
            log.error("Fabric Gateway 连接失败: {}", e.getMessage(), e);
        }
    }

    private void connect() throws IOException {
        // 1. 加载钱包
        Wallet wallet = Wallets.newFileSystemWallet(Paths.get(fabricConfig.getWalletPath()));

        // 2. 加载 connection-profile
        Path configPath = resolveConfigPath();
        String yaml = readFile(configPath);

        // 3. 构建 Gateway
        Gateway.Builder builder = Gateway.createBuilder()
            .identity(wallet, fabricConfig.getIdentityLabel())
            .networkConfig(yaml);

        this.gateway = builder.connect();
        this.network = gateway.getNetwork(fabricConfig.getChannel());
        this.evidenceContract = network.getContract(fabricConfig.getChaincode(), "EvidenceContract");
        this.contractContract = network.getContract(fabricConfig.getChaincode(), "ContractContract");
        this.auditContract = network.getContract(fabricConfig.getChaincode(), "AuditContract");

        log.info("合约已注册: EvidenceContract, ContractContract, AuditContract");
    }

    public Contract getEvidenceContract() {
        return evidenceContract;
    }

    public Contract getContractContract() {
        return contractContract;
    }

    public Contract getAuditContract() {
        return auditContract;
    }

    public Network getNetwork() {
        return network;
    }

    @PreDestroy
    public void close() {
        if (gateway != null) {
            gateway.close();
            log.info("Fabric Gateway 已关闭");
        }
    }

    private Path resolveConfigPath() {
        String p = fabricConfig.getNetworkConfigPath();
        if (p.startsWith("classpath:")) {
            // classpath 资源 → 临时文件
            String resource = p.substring("classpath:".length());
            try (var in = getClass().getClassLoader().getResourceAsStream(resource)) {
                if (in == null) throw new IOException("classpath 资源不存在: " + resource);
                Path tmp = Files.createTempFile("connection-profile-", ".yaml");
                Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                tmp.toFile().deleteOnExit();
                return tmp;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return Paths.get(p);
    }

    private String readFile(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[1024];
            int n;
            while ((n = reader.read(buf)) > 0) {
                sb.append(buf, 0, n);
            }
            return sb.toString();
        }
    }
}
