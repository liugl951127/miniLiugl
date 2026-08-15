package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
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
    private String recordId;
    /** userId */
    private Long userId;
    /** 关联 license (可空, 仅充值时空) */
    private Long licenseId;
    /** 关联 modelEntry */
    private Long modelEntryId;
    /** 类型: PURCHASE / RENEW / REFUND / TOPUP / USAGE */
    private String recordType;
    /** 金额 (分, 正数入账, 负数出账) */
    private Long amountCents;
    /** 货币 (默认 CNY) */
    private String currency;
    /** 状态: PENDING / SUCCESS / FAILED / REFUNDED */
    private String status;
    /** 支付方式 (alipay / wechat / stripe / ... ) */
    private String paymentMethod;
    /** 外部交易号 (支付宝/微信) */
    private String externalTransactionId;
    /** 描述 */
    private String description;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
