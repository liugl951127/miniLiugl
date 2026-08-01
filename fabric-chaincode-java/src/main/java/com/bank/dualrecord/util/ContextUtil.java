package com.bank.dualrecord.util;

import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyModification;

import java.time.Instant;
import java.util.List;

/**
 * 链码上下文工具
 *
 * <p>从 Fabric Context 提取交易信息:TxID / 提交人 / MSP / 区块号
 */
public final class ContextUtil {

    private ContextUtil() {
    }

    /**
     * 获取提交方 MSP ID
     */
    public static String getSubmitterMspId(Context ctx) {
        try {
            ClientIdentity id = ctx.getClientIdentity();
            return id.getMspId();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 获取提交方 CN
     */
    public static String getSubmitterCN(Context ctx) {
        try {
            ClientIdentity id = ctx.getClientIdentity();
            return id.getId();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 获取当前交易 ID
     */
    public static String getTxId(Context ctx) {
        return ctx.getStub().getTxId();
    }

    /**
     * 获取当前通道名
     */
    public static String getChannelName(Context ctx) {
        return ctx.getStub().getChannelId();
    }

    /**
     * 获取当前时间戳
     */
    public static Instant getCurrentTime(Context ctx) {
        return ctx.getStub().getTxTimestamp();
    }

    /**
     * 获取历史修改
     */
    public static List<KeyModification> getHistory(Context ctx, String key) {
        return ctx.getStub().getHistoryForKey(key);
    }
}
