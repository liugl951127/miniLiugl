package com.minimax.chat.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 生成请求 (V9.0)
 */
@Data
public class AiGenerateRequest {
    /** 用户当前消息 */
    private String content;

    /** 系统提示词 (可选) */
    private String system;

    /** 历史消息 (可选, 顺序: user/assistant 交替) */
    private List<HistoryItem> history;

    @Data
    public static class HistoryItem {
        private String role;
        private String content;
    }
}
