package com.minimax.ai.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 协作房间邀请 DTO (T3-new-code-robustness)
 *
 * 字段:
 *   - email: 必填, 必须是合法邮箱
 *
 * roomId 通过 @PathVariable 传入, 不在此 DTO 中重复声明。
 *
 * @since V7.2
 */
@Data
public class CollabInviteRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不合法")
    @Size(max = 128, message = "邮箱长度 ≤ 128")
    private String email;
}
