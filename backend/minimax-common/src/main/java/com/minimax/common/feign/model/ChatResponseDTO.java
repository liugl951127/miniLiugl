package com.minimax.common.feign.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 跨服务共享的 LLM 对话响应 DTO
 * model → analytics 通过 HTTP/Feign 传递
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String model;
    private String content;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private String finishReason;
    private Long latencyMs;
    private String providerCode;
    /** 透传 OpenAI 风格完整响应 */
    private Map<String, Object> raw;
}
