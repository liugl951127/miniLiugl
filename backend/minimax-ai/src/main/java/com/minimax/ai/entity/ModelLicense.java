package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型许可证 (V3.3.2)
 *
 * <p>用户购买/订阅模型后生成一条 license, 用于鉴权调用
 */
@Data
@TableName("model_license")
public class ModelLicense {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务 licenseKey (UUID) */
    @TableField("license_key")
    private String licenseKey;
    /** 关联 modelEntry */
    @TableField("model_entry_id")
    private Long modelEntryId;
    /** 关联 modelVersion (可空, 表示任意版本) */
    @TableField("model_version_id")
    private Long modelVersionId;
    /** 持有者 userId */
    @TableField("user_id")
    private Long userId;
    /** 类型: TRIAL / PERSONAL / COMMERCIAL / ENTERPRISE */
    @TableField("license_type")
    private String licenseType;
    /** 状态: ACTIVE / EXPIRED / REVOKED */
    @TableField("status")
    private String status;
    /** 配额 (调用次数, 0=无限) */
    @TableField("quota_calls")
    private Long quotaCalls;
    /** 已用次数 */
    @TableField("used_calls")
    private Long usedCalls;
    /** 起始时间 */
    @TableField("start_at")
    private LocalDateTime startAt;
    /** 到期时间 (空=永久) */
    @TableField("expire_at")
    private LocalDateTime expireAt;
    /** 价格 (分) */
    @TableField("price_cents")
    private Long priceCents;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
