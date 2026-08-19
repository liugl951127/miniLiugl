package com.minimax.common.feign.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 跨服务共享的 LLM 对话请求 DTO
 * analytics → model 通过 HTTP/Feign 传递
 */
@Data
public class ChatRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 模型标识，如 "gpt-4o-mini" */
    private String model;

    /** 消息列表，格式: [{"role":"user","content":"..."}] */
    private List<Map<String, Object>> messages;

    /** 温度参数 */
    private Double temperature;

    /** 最大 token 数 */
    private Integer maxTokens;

    /** 是否流式（默认 false） */
    private Boolean stream = false;
}
