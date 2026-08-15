package com.minimax.ai.push.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推送结果 (V3.5.1 真实集成)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushResult {
    /** 是否成功 */
    private boolean success;
    /** 平台 */
    private Platform platform;
    /** 平台返回的消息 ID (用于追踪) */
    private String messageId;
    /** HTTP 状态码 */
    private int statusCode;
    /** 错误码 (失败时) */
    private String errorCode;
    /** 错误信息 (失败时) */
    private String errorMessage;
    /** 响应原始数据 (调试用) */
    private String rawResponse;

    public enum Platform { WEB_PUSH, APNS, FCM }

    public static PushResult ok(Platform p, String messageId, int statusCode, String raw) {
        return PushResult.builder()
                .success(true)
                .platform(p)
                .messageId(messageId)
                .statusCode(statusCode)
                .rawResponse(raw)
                .build();
    }

    public static PushResult fail(Platform p, int statusCode, String errCode, String errMsg, String raw) {
        return PushResult.builder()
                .success(false)
                .platform(p)
                .statusCode(statusCode)
                .errorCode(errCode)
                .errorMessage(errMsg)
                .rawResponse(raw)
                .build();
    }
}
