package com.bank.dualrecord;

import com.bank.dualrecord.contract.AuditContract;
import com.bank.dualrecord.contract.ContractContract;
import com.bank.dualrecord.contract.EvidenceContract;
import org.hyperledger.fabric.contract.ContractRouter;
import org.hyperledger.fabric.shim.Shim;

/**
 * 双录一体化平台链码入口
 *
 * <p>启动后向 peer 注册 3 个合约:
 * <ul>
 *   <li>EvidenceContract - 证据上链</li>
 *   <li>ContractContract - 合同存证</li>
 *   <li>AuditContract - 审计追溯</li>
 * </ul>
 */
public class Main {

    public static void main(String[] args) {
        // ChaincodeBase 模式:为每个合约创建独立 router(支持独立背书策略)
        // 这里使用多合约模式,共享一个 chaincode 进程
        if (args.length == 0) {
            // 默认启动:注册全部合约
            new ChaincodeApp().start();
        } else if ("-ccid".equals(args[0]) && args.length > 1) {
            // 指定 chaincode id
            new ChaincodeApp().start();
        } else {
            new ChaincodeApp().start();
        }
    }

    /**
     * 链码应用(注册所有合约)
     */
    public static class ChaincodeApp {

        public void start() {
            ContractRouter router = new ContractRouter("dual-record-chaincode");
            router.addContract("EvidenceContract", new EvidenceContract());
            router.addContract("ContractContract", new ContractContract());
            router.addContract("AuditContract", new AuditContract());

            Shim.start(router);
        }
    }
}
