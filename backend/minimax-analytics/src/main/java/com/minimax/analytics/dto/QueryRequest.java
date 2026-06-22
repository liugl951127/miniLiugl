package com.minimax.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * SQL 执行请求 DTO
 */
@Data
public class QueryRequest {

    @NotNull
    private Long dataSourceId;

    @NotBlank
    private String sql;

    private Integer maxRows = 1000;
    private Integer timeoutSeconds = 30;
}
