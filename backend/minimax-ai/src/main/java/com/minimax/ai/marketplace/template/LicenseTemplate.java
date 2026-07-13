package com.minimax.ai.marketplace.template;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型 License 模板 (V3.5.2 自研)
 *
 * <h3>背景</h3>
 * V3.3.2 的 ModelLicense 字段写死 (TRIAL=7天/PERSONAL=30天 等),
 * 业务方无法新增 license 类型. V3.5.2 用 LicenseTemplate 解耦:
 *   - 模板定义: 名称/类型/配额/价格/特性/限制
 *   - 用户购买: 从模板签发 ModelLicense
 *   - 模板可克隆 / 复用 / 上线 / 下线
 *
 * <h3>字段</h3>
 * <ul>
 *   <li>name       - 模板名 (e.g. "个人免费版")</li>
 *   <li>type       - 4 种类型 (TRIAL/PERSONAL/COMMERCIAL/ENTERPRISE)</li>
 *   <li>quotaCalls - 调用配额 (0=无限)</li>
 *   <li>quotaDays  - 有效天数 (0=永久)</li>
 *   <li>priceCents - 价格 (分, 0=免费)</li>
 *   <li>features   - 启用的特性 (JSON 数组, e.g. ["inference","rag","agent"])</li>
 *   <li>limits     - 限制 (JSON, e.g. {"qps":10,"rpm":100})</li>
 *   <li>description- 描述</li>
 *   <li>isPublic   - 是否公开 (公开的可在用户端看到)</li>
 *   <li>isActive   - 是否启用 (下架后不展示)</li>
 *   <li>version    - 模板版本 (变更递增)</li>
 *   <li>createdBy  - 创建人 ID</li>
 * </ul>
 */
@Data
@TableName("license_template")
public class LicenseTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板唯一 key (e.g. "trial-7d", "personal-monthly") */
    private String templateKey;

    /** 显示名 */
    private String name;

    /** 类型: TRIAL / PERSONAL / COMMERCIAL / ENTERPRISE */
    private String licenseType;

    /** 描述 */
    private String description;

    /** 调用配额 (0 = 无限) */
    private Long quotaCalls;

    /** 有效天数 (0 = 永久) */
    private Integer quotaDays;

    /** 价格 (分, 0 = 免费) */
    private Long priceCents;

    /** 启用特性 (JSON 数组字符串) */
    private String features;

    /** 限制 (JSON 字符串, e.g. {"qps":10}) */
    private String limits;

    /** 是否公开 (用户端可见) */
    private Integer isPublic;

    /** 是否启用 */
    private Integer isActive;

    /** 模板版本 (变更时递增) */
    private Integer version;

    /** 创建人 ID */
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
