package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 计费记录 (V3.3.2)
 *
 * <p>每次扣费/充值/退款生成一条, 财务对账
 */
@Data
@TableName("billing_record")
public class BillingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务 recordId (UUID) */
    @TableField("record_id")
    private String recordId;
    /** userId */
    @TableField("user_id")
    private Long userId;
    /** 关联 license (可空, 仅充值时空) */
    @TableField("license_id")
    private Long licenseId;
    /** 关联 modelEntry */
    @TableField("model_entry_id")
    private Long modelEntryId;
    /** 类型: PURCHASE / RENEW / REFUND / TOPUP / USAGE */
    @TableField("record_type")
    private String recordType;
    /** 金额 (分, 正数入账, 负数出账) */
    @TableField("amount_cents")
    private Long amountCents;
    /** 货币 (默认 CNY) */
    @TableField("currency")
    private String currency;
    /** 状态: PENDING / SUCCESS / FAILED / REFUNDED */
    @TableField("status")
    private String status;
    /** 支付方式 (alipay / wechat / stripe / ... ) */
    @TableField("payment_method")
    private String paymentMethod;
    /** 外部交易号 (支付宝/微信) */
    @TableField("external_transaction_id")
    private String externalTransactionId;
    /** 描述 */
    @TableField("description")
    private String description;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
