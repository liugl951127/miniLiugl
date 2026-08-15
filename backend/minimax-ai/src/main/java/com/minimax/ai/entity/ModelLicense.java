package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
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
    private String licenseKey;
    /** 关联 modelEntry */
    private Long modelEntryId;
    /** 关联 modelVersion (可空, 表示任意版本) */
    private Long modelVersionId;
    /** 持有者 userId */
    private Long userId;
    /** 类型: TRIAL / PERSONAL / COMMERCIAL / ENTERPRISE */
    private String licenseType;
    /** 状态: ACTIVE / EXPIRED / REVOKED */
    private String status;
    /** 配额 (调用次数, 0=无限) */
    private Long quotaCalls;
    /** 已用次数 */
    private Long usedCalls;
    /** 起始时间 */
    private LocalDateTime startAt;
    /** 到期时间 (空=永久) */
    private LocalDateTime expireAt;
    /** 价格 (分) */
    private Long priceCents;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
