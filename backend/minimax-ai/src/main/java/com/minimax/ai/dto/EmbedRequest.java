package com.minimax.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * Embedding 请求
 */
@Data
public class EmbedRequest {
    /** 单个文本或文本列表 */
    private String text;
    private List<String> texts;
}
