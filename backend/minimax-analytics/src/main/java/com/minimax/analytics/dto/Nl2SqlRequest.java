package com.minimax.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * NL2SQL 请求 DTO
 */
@Data
public class Nl2SqlRequest {

    @NotNull
    private Long dataSourceId;

    @NotBlank
    private String question;          // 用户自然语言问题

    private String database;          // 限定数据库 (可选)
    private String model;             // 覆盖默认模型
    private Boolean autoExecute;      // 是否自动执行 (默认 true)
}
