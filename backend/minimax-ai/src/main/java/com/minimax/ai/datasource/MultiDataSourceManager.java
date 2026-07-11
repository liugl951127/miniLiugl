package com.minimax.ai.datasource;

import com.minimax.ai.entity.DbDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MultiDataSourceManager {

    private final Map<Long, HikariDataSource> cache = new ConcurrentHashMap<>();

    private static final Map<String, String> DRIVERS = Map.of(
            "mysql", "com.mysql.cj.jdbc.Driver",
            "postgresql", "org.postgresql.Driver",
            "oracle", "oracle.jdbc.OracleDriver",
            "sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver",
            "h2", "org.h2.Driver",
            "clickhouse", "ru.yandex.clickhouse.ClickHouseDriver",
            "doris", "com.mysql.cj.jdbc.Driver"
    );

    public boolean isDriverAvailable(String type) {
        String driver = DRIVERS.get(type.toLowerCase());
        if (driver == null) return false;
        try {
            Class.forName(driver);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** 获取或创建数据源 (Hikari DataSource) */
    public DataSource getDataSource(DbDataSource entity) {
        return cache.computeIfAbsent(entity.getId(), id -> createDataSource(entity));
    }

    private HikariDataSource createDataSource(DbDataSource entity) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(entity.getJdbcUrl());
        config.setUsername(entity.getUsername());
        config.setPassword(entity.getPassword());
        config.setDriverClassName(entity.getDriverClass() != null ? entity.getDriverClass()
                : DRIVERS.getOrDefault(entity.getType().toLowerCase(), ""));
        config.setMaximumPoolSize(entity.getPoolSize() != null ? entity.getPoolSize() : 10);
        config.setMinimumIdle(entity.getMinIdle() != null ? entity.getMinIdle() : 2);
        config.setMaxLifetime(entity.getMaxLifetime() != null ? entity.getMaxLifetime() * 1000L : 1800000L);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setPoolName("AI-DS-" + entity.getId() + "-" + entity.getName());
        config.setKeepaliveTime(30000);

        log.info("创建数据源: id={} name={} type={} url={}",
                entity.getId(), entity.getName(), entity.getType(), entity.getJdbcUrl());
        return new HikariDataSource(config);
    }

    public TestResult testConnection(DbDataSource entity) {
        try {
            String driver = entity.getDriverClass() != null ? entity.getDriverClass()
                    : DRIVERS.getOrDefault(entity.getType().toLowerCase(), "");
            if (driver.isEmpty()) {
                return TestResult.fail("未知数据库类型: " + entity.getType());
            }
            try (Connection conn = DriverManager.getConnection(
                    entity.getJdbcUrl(), entity.getUsername(), entity.getPassword())) {
                boolean valid = conn.isValid(5);
                return valid ? TestResult.ok(conn.getMetaData().getDatabaseProductName()
                        + " " + conn.getMetaData().getDatabaseProductVersion())
                        : TestResult.fail("连接无效");
            }
        } catch (Exception e) {
            log.error("测试连接失败", e);
            return TestResult.fail(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    public void close(Long id) {
        HikariDataSource ds = cache.remove(id);
        if (ds != null && !ds.isClosed()) {
            ds.close();
            log.info("关闭数据源: id={}", id);
        }
    }

    public void closeAll() {
        cache.forEach((id, ds) -> { if (!ds.isClosed()) ds.close(); });
        cache.clear();
    }

    public Set<Long> activeIds() {
        return cache.keySet();
    }

    public static class TestResult {
        public boolean success;
        public String message;

        public static TestResult ok(String msg) {
            TestResult r = new TestResult();
            r.success = true;
            r.message = msg;
            return r;
        }

        public static TestResult fail(String msg) {
            TestResult r = new TestResult();
            r.success = false;
            r.message = msg;
            return r;
        }
    }
}
