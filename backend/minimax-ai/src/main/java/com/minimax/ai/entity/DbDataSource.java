package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据源连接 (V2.5 多数据库支持)
 *
 * 支持: mysql / postgresql / oracle / sqlserver / mongodb / clickhouse / doris
 */
@Data
@TableName("data_source")
public class DbDataSource {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 数据源名称 */
    private String name;

    /** 类型: mysql/postgresql/oracle/sqlserver/mongodb/clickhouse/doris */
    private String type;

    /** JDBC URL */
    private String jdbcUrl;

    private String username;

    private String password;

    /** 驱动类 (可自动推断) */
    private String driverClass;

    private Integer poolSize;

    private Integer minIdle;

    private Integer maxLifetime;

    private Integer enabled;

    /** UNKNOWN / OK / FAILED */
    private String testStatus;

    private String testMessage;

    private LocalDateTime lastTestAt;

    private String description;

    private String tags;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
