package com.minimax.pipeline.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 规则定义 创建/更新 DTO (T3-new-code-robustness)
 *
 * 由 RuleController 接收 @RequestBody, 字段校验:
 *   - name: 不能为空, 1-128 字符
 *   - json: 不能为空, 长度 ≤ 65535 (LONGTEXT 兜底)
 *   - scope: 必须是 GLOBAL / TENANT / USER
 *   - enabled: 0 或 1 (前端会传 0/1)
 *
 * @since V7.2
 */
@Data
public class RuleUpsertRequest {

    @NotBlank(message = "规则名不能为空")
    @Size(min = 1, max = 128, message = "规则名长度 1-128")
    private String name;

    @NotBlank(message = "规则 JSON 不能为空")
    @Size(max = 65535, message = "规则 JSON 过长 (≤65535)")
    private String json;

    /** 作用域: GLOBAL / TENANT / USER */
    @Pattern(regexp = "^(GLOBAL|TENANT|USER)$",
            message = "scope 必须为 GLOBAL/TENANT/USER")
    private String scope;

    /** 0=禁用 1=启用 */
    @Min(value = 0, message = "enabled 必须为 0 或 1")
    @Max(value = 1, message = "enabled 必须为 0 或 1")
    private Integer enabled;
}
