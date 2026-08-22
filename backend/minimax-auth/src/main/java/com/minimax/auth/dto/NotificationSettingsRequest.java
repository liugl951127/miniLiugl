package com.minimax.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 通知设置保存 DTO (T3-new-code-robustness)
 *
 * 字段:
 *   - channels:   逗号分隔的渠道列表 (必填, CSV 格式)
 *   - events:     逗号分隔的事件列表 (必填, CSV 格式)
 *   - quietStart: 免打扰开始 HH:mm (必填)
 *   - quietEnd:   免打扰结束 HH:mm (必填)
 *
 * @since V7.2
 */
@Data
public class NotificationSettingsRequest {

    @NotBlank(message = "channels 不能为空")
    @Size(max = 256, message = "channels 过长 (≤256)")
    private String channels;

    @NotBlank(message = "events 不能为空")
    @Size(max = 256, message = "events 过长 (≤256)")
    private String events;

    @NotBlank(message = "quietStart 不能为空")
    @Pattern(regexp = "^\\d{2}:\\d{2}$", message = "quietStart 必须为 HH:mm 格式")
    private String quietStart;

    @NotBlank(message = "quietEnd 不能为空")
    @Pattern(regexp = "^\\d{2}:\\d{2}$", message = "quietEnd 必须为 HH:mm 格式")
    private String quietEnd;
}
