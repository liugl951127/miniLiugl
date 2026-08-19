package com.minimax.common.feign.analytics;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 跨服务共享的数据源配置 DTO
 * analytics → pipeline 通过 HTTP 传递（或 pipeline 直接查 analytics_datasource 表）
 */
@Data
public class DataSourceDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    /** 用户给的名字, 如 "生产DB" */
    private String name;
    /** mysql / h2 / postgresql */
    private String type;
    /** jdbc:mysql://host:3306/db */
    private String jdbcUrl;
    private String username;
    /** AES 加密存储的密码 */
    private String passwordEnc;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 数据库类型常量 */
    public static final String TYPE_MYSQL = "mysql";
    public static final String TYPE_H2 = "h2";
    public static final String TYPE_POSTGRESQL = "postgresql";
}
