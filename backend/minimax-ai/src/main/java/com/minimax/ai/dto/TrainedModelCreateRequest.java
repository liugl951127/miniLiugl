package com.minimax.ai.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 训练模型创建请求 DTO (T3-new-code-robustness)
 *
 * 字段:
 *   - code:     模型唯一编码 (必填, 字母数字下划线短横线)
 *   - name:     模型显示名 (必填)
 *   - accuracy: 准确率 0.00-1.00 (允许 null 表示未填写)
 *   - status:   ENABLED / DISABLED / DRAFT (允许 null)
 *
 * @since V7.2
 */
@Data
public class TrainedModelCreateRequest {

    @NotBlank(message = "模型编码 code 不能为空")
    @Size(min = 1, max = 64, message = "code 长度 1-64")
    @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "code 仅支持字母/数字/._-")
    private String code;

    @NotBlank(message = "模型名称 name 不能为空")
    @Size(min = 1, max = 128, message = "name 长度 1-128")
    private String name;

    /** 准确率 0-1 (e.g. 0.872 = 87.2%), 允许 null */
    @DecimalMin(value = "0.0", message = "accuracy 必须 ≥ 0")
    @DecimalMax(value = "1.0", message = "accuracy 必须 ≤ 1")
    private BigDecimal accuracy;

    /** ENABLED / DISABLED / DRAFT (允许 null 默认 DRAFT) */
    @Pattern(regexp = "^(ENABLED|DISABLED|DRAFT)$",
            message = "status 必须为 ENABLED/DISABLED/DRAFT")
    private String status;
}
