package com.bank.dualrecord.contract;

import com.bank.dualrecord.model.AuditRecord;
import com.bank.dualrecord.util.ContextUtil;
import com.bank.dualrecord.util.JsonUtil;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审计追溯合约(只增不改)
 *
 * @author Mavis
 */
@Contract(
    name = "AuditContract",
    info = @Info(
        title = "审计追溯合约",
        description = "只增不改的操作日志,支持司法举证",
        version = "1.0.0",
        license = @License(name = "Apache-2.0")
    )
)
public class AuditContract implements ContractInterface {

    private static final Logger log = LoggerFactory.getLogger(AuditContract.class);

    private static final String IDX_ORDER = "AUDIT_ORDER~";
    private static final String IDX_ACTION = "AUDIT_ACTION~";

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String recordAudit(@Default() Context ctx, String auditJson) {
        AuditRecord audit = JsonUtil.fromJson(auditJson, AuditRecord.class);

        if (audit.getId() == null) {
            audit.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
        }
        audit.setTimestamp(ContextUtil.getCurrentTime(ctx));
        audit.setTxId(ContextUtil.getTxId(ctx));
        if (audit.getOperator() == null) {
            audit.setOperator(ContextUtil.getSubmitterCN(ctx));
        }
        if (audit.getOperatorOrg() == null) {
            audit.setOperatorOrg(ContextUtil.getSubmitterMspId(ctx));
        }

        String key = "AUDIT~" + audit.getOrderId() + "~" + audit.getId();
        ctx.getStub().putState(key, JsonUtil.toJson(audit).getBytes(StandardCharsets.UTF_8));

        appendIndex(ctx, IDX_ORDER + audit.getOrderId(), audit.getId());
        appendIndex(ctx, IDX_ACTION + audit.getAction(), audit.getId());

        ctx.getStub().setEvent("AuditRecorded", JsonUtil.toJson(audit).getBytes(StandardCharsets.UTF_8));
        return audit.getId();
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryOrderAudits(@Default() Context ctx, String orderId) {
        byte[] idx = ctx.getStub().getState(IDX_ORDER + orderId);
        if (idx == null || idx.length == 0) return "[]";

        String[] ids = new String(idx, StandardCharsets.UTF_8).split(",");
        List<AuditRecord> list = new ArrayList<>();
        for (String id : ids) {
            byte[] bytes = ctx.getStub().getState("AUDIT~" + orderId + "~" + id);
            if (bytes != null && bytes.length > 0) {
                list.add(JsonUtil.fromJson(new String(bytes, StandardCharsets.UTF_8), AuditRecord.class));
            }
        }
        return JsonUtil.toJson(list);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryByAction(@Default() Context ctx, String action, int pageSize) {
        byte[] idx = ctx.getStub().getState(IDX_ACTION + action);
        if (idx == null) return "{\"items\":[],\"bookmark\":\"\"}";

        String[] ids = new String(idx, StandardCharsets.UTF_8).split(",");
        int end = Math.min(pageSize > 0 ? pageSize : 100, ids.length);

        List<Map<String, String>> list = new ArrayList<>();
        for (int i = 0; i < end; i++) {
            Map<String, String> item = new HashMap<>();
            item.put("auditId", ids[i]);
            item.put("action", action);
            list.add(item);
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("items", list);
        resp.put("bookmark", end >= ids.length ? "" : String.valueOf(end));
        return JsonUtil.toJson(resp);
    }

    private void appendIndex(Context ctx, String key, String id) {
        byte[] idx = ctx.getStub().getState(key);
        List<String> ids = new ArrayList<>();
        if (idx != null && idx.length > 0) {
            for (String s : new String(idx, StandardCharsets.UTF_8).split(",")) {
                if (!s.isEmpty()) ids.add(s);
            }
        }
        ids.add(id);
        ctx.getStub().putState(key, String.join(",", ids).getBytes(StandardCharsets.UTF_8));
    }
}
