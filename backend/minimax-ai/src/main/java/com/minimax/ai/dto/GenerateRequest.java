package com.minimax.ai.dto;

import lombok.Data;

/**
 * 文本生成请求
 */
@Data
public class GenerateRequest {
    /** 输入提示 */
    private String prompt;
    /** 最大生成长度 (默认 50) */
    private Integer maxLength;
    /** 温度 (0.1-2.0, 默认 0.8) */
    private Double temperature;
    /** 是否流式输出 (SSE) */
    private Boolean stream;
}
