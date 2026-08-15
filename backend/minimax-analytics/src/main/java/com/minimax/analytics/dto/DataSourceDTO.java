package com.minimax.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 数据源创建/更新 DTO
 */
@Data
public class DataSourceDTO {

    @NotBlank
    private String name;

    @NotBlank
    private String type;             // mysql / h2

    @NotBlank
    private String jdbcUrl;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    private String description;
}
