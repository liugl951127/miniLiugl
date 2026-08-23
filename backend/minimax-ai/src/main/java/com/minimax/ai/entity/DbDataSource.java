package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField("name")
    private String name;

    /** 类型: mysql/postgresql/oracle/sqlserver/mongodb/clickhouse/doris */
    @TableField("type")
    private String type;

    /** JDBC URL */
    @TableField("jdbc_url")
    private String jdbcUrl;

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    /** 驱动类 (可自动推断) */
    @TableField("driver_class")
    private String driverClass;

    @TableField("pool_size")
    private Integer poolSize;

    @TableField("min_idle")
    private Integer minIdle;

    @TableField("max_lifetime")
    private Integer maxLifetime;

    @TableField("enabled")
    private Integer enabled;

    /** UNKNOWN / OK / FAILED */
    @TableField("test_status")
    private String testStatus;

    @TableField("test_message")
    private String testMessage;

    @TableField("last_test_at")
    private LocalDateTime lastTestAt;

    @TableField("description")
    private String description;

    @TableField("tags")
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
