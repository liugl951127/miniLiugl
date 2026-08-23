package com.minimax.deployer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 需求解析请求 DTO (V2.0)
 *
 * 用户提交需求文本 (来自文档/对话/模板), 后端调用 LLM 进行解析。
 */
@Data
public class ParseRequirementsRequest {

    /** 需求来源: DOCUMENT / CHAT / TEMPLATE */
    @NotBlank
    private String source;

    /** 原始需求文本 (CHAT 模式直接传, DOCUMENT 模式为解析后的文本) */
    @NotBlank
    private String content;

    /** 文档名 (可选, 用于 DOCUMENT 模式) */
    private String documentName;

    /** 模板 code (可选, TEMPLATE 模式) */
    private String templateCode;

    /** 用户 ID (从 JWT 解析) */
    private Long userId;
}
