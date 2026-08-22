package com.minimax.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 系统设置保存 DTO (T3-new-code-robustness)
 *
 * 字段均为可选 (PATCH 语义), 仅在非 null 时做校验:
 *   - siteName:        站点名 (≤ 128)
 *   - siteLogo:        站点 Logo URL (≤ 512)
 *   - maintenanceMode: 0=正常 1=维护
 *   - allowRegister:   0=禁止注册 1=允许
 *   - defaultModelCode: 默认模型编码 (≤ 64)
 *   - description:     描述 (≤ 1024)
 *   - contactEmail:    联系邮箱 (≤ 128)
 *
 * @since V7.2
 */
@Data
public class SystemSettingsRequest {

    @Size(max = 128, message = "siteName 长度 ≤ 128")
    private String siteName;

    @Size(max = 512, message = "siteLogo URL 长度 ≤ 512")
    private String siteLogo;

    @Min(value = 0, message = "maintenanceMode 必须为 0 或 1")
    @Max(value = 1, message = "maintenanceMode 必须为 0 或 1")
    private Integer maintenanceMode;

    @Min(value = 0, message = "allowRegister 必须为 0 或 1")
    @Max(value = 1, message = "allowRegister 必须为 0 或 1")
    private Integer allowRegister;

    @Size(max = 64, message = "defaultModelCode 长度 ≤ 64")
    private String defaultModelCode;

    @Size(max = 1024, message = "description 长度 ≤ 1024")
    private String description;

    @Size(max = 128, message = "contactEmail 长度 ≤ 128")
    private String contactEmail;
}
