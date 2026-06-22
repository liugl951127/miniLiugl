package com.minimax.auth.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** API Key 响应 VO (V5.33 Day 18) */
@Data
@Builder
public class ApiKeyResponse {

    private Long id;
    private String name;
    private String keyPrefix;      // mmx_a1b2****
    private String scopes;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private Long useCount;
    private Integer enabled;
    private LocalDateTime createdAt;

    /**
     * 原始 Key，仅在创建时返回一次，之后不再显示。
     * 创建成功后前端负责存储。
     */
    private String rawKey;
}
