package com.minimax.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ChatResponse {
    private String id;
    private String model;
    private String content;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private String finishReason;
    private Long latencyMs;
    private String providerCode;
    /** 透传 OpenAI 风格完整响应（供调用方扩展）。 */
    private Map<String, Object> raw;
    private List<Map<String, Object>> messages;  // 如有需要可附
}
