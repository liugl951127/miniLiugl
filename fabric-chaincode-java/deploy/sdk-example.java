// 业务系统调用链码的 Java SDK 示例
// 用于双录业务系统 / 风控审计系统集成

import org.hyperledger.fabric.gateway.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FabricClient {

    public static void main(String[] args) throws Exception {
        // ============================================================
        // 1. 加载钱包 + 连接 Gateway
        // ============================================================
        Path walletPath = Paths.get(".", "wallet");
        Wallet wallet = Wallets.newFileSystemWallet(walletPath);

        Path connectionProfile = Paths.get(".", "connection-profile.json");
        Gateway.Builder builder = Gateway.createBuilder()
            .identity(wallet, "admin")
            .connectionProfile(connectionProfile);

        try (Gateway gateway = builder.connect()) {
            // ============================================================
            // 2. 获取通道 + 合约
            // ============================================================
            Network network = gateway.getNetwork("dual-record-channel");
            Contract evidenceContract = network.getContract("dual-record-chaincode", "EvidenceContract");
            Contract contractContract = network.getContract("dual-record-chaincode", "ContractContract");
            Contract auditContract = network.getContract("dual-record-chaincode", "AuditContract");

            // ============================================================
            // 3. 提交证据(写操作)
            // ============================================================
            String evidenceJson = "{"
                + "\"orderId\":\"ORD20260801000001\","
                + "\"customerId\":\"C001\","
                + "\"productType\":\"INSURANCE\","
                + "\"productName\":\"XX 寿险\","
                + "\"amount\":5000000,"
                + "\"videoHash\":\"2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824\","
                + "\"audioHash\":\"3e23e8160039594a33894f6564e1b1348bbd7a0088d42c4acb73eeaed59c009d\","
                + "\"contractHash\":\"b3a8e0e1f9c18a6d54c9b65a0c5e0a3b2c1d4e5f6789abcdef0123456789abcd\","
                + "\"channel\":\"H5\","
                + "\"salesUserId\":\"M001\","
                + "\"branchId\":\"2001\","
                + "\"scriptVersion\":\"V3.2\","
                + "\"customerSm2Signature\":\"abcdef...\","
                + "\"managerSm2Signature\":\"fedcba...\""
                + "}";

            byte[] result = evidenceContract.submitTransaction("submitEvidence", evidenceJson);
            System.out.println("提交结果: " + new String(result, StandardCharsets.UTF_8));

            // ============================================================
            // 4. 状态流转
            // ============================================================
            byte[] stateResult = evidenceContract.submitTransaction("updateState",
                "ORD20260801000001", "SCRIPTING", "开始话术");
            System.out.println("状态: " + new String(stateResult, StandardCharsets.UTF_8));

            // ============================================================
            // 5. 查询(免费,不上链)
            // ============================================================
            byte[] queryResult = evidenceContract.evaluateTransaction("queryEvidence", "ORD20260801000001");
            System.out.println("证据: " + new String(queryResult, StandardCharsets.UTF_8));

            // ============================================================
            // 6. 验证证据(司法取证)
            // ============================================================
            byte[] verifyResult = evidenceContract.evaluateTransaction("verifyEvidence",
                "ORD20260801000001",
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                "3e23e8160039594a33894f6564e1b1348bbd7a0088d42c4acb73eeaed59c009d",
                "b3a8e0e1f9c18a6d54c9b65a0c5e0a3b2c1d4e5f6789abcdef0123456789abcd");
            System.out.println("验证: " + new String(verifyResult, StandardCharsets.UTF_8));

            // ============================================================
            // 7. 审计查询
            // ============================================================
            byte[] auditResult = auditContract.evaluateTransaction("queryOrderAudits", "ORD20260801000001");
            System.out.println("审计: " + new String(auditResult, StandardCharsets.UTF_8));
        }
    }
}
