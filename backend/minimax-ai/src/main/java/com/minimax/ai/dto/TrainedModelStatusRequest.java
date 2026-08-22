package com.minimax.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 训练模型状态切换 DTO (T3-new-code-robustness)
 *
 * @since V7.2
 */
@Data
public class TrainedModelStatusRequest {

    @NotBlank(message = "status 不能为空")
    @Pattern(regexp = "^(ENABLED|DISABLED|DRAFT)$",
            message = "status 必须为 ENABLED/DISABLED/DRAFT")
    private String status;
}
