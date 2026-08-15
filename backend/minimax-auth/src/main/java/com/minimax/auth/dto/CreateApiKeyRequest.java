package com.minimax.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 创建 API Key 请求 (V5.33 Day 18) */
@Data
public class CreateApiKeyRequest {

    /** Key 名称，如 "生产环境 Key" */
    private String name;

    /** 权限范围，逗号分隔，默认 "chat:send,chat:stream" */
    private String scopes;

    /** 过期时间，NULL 表示永不过期 */
    private LocalDateTime expiresAt;
}
