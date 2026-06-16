package com.minimax.model.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 兼容 OpenAI Chat Completions 请求体。
 * 简化版：只支持 messages 数组，其他字段（temperature/top_p/...）可选。
 */
@Data
public class ChatRequest {

    @NotBlank
    private String model;

    private List<Map<String, String>> messages;

    private Double temperature;
    private Integer maxTokens;
    private Boolean stream = false;

    /** 手动校验：messages 非空。 */
    @AssertTrue(message = "messages 不能为空")
    public boolean isMessagesValid() {
        return messages != null && !messages.isEmpty();
    }
}
