package com.minimax.analytics.init;

import com.minimax.analytics.entity.DataSource;
import com.minimax.analytics.mapper.DataSourceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Demo 数据初始化器 (V5.31)
 *
 * h2local 模式下:
 *  1. 创建 demo H2 内存数据库 (jdbc:h2:mem:demo)
 *  2. 运行 demo-init.sql 建表+种子数据
 *  3. 向 analytics_datasource 插入一条 demo 数据源记录
 *
 * 如此用户进入 NL2SQL 页面即可直接体验, 无需手动配置数据源.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    /** analytics 自身数据源 (由 Spring 注入) */
    private final javax.sql.DataSource analyticsDs;
    private final DataSourceMapper dataSourceMapper;

    @Override
    @Transactional
    public void run(org.springframework.boot.ApplicationArguments args) throws Exception {
        initDemoDatabase();
        insertDemoDataSource();
    }

    private void initDemoDatabase() {
        try {
            // 用 DriverManager 直接获取连接 (不依赖 H2 XA DataSource)
            Connection conn = DriverManager.getConnection(
                "jdbc:h2:mem:demo;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
                "sa", "");

            // V7.2: 从 minimax-seed.sql 提取 demo_* 表的 CREATE + INSERT 段
            var res = DemoDataInitializer.class.getClassLoader()
                .getResourceAsStream("sql/minimax-seed.sql");
            if (res == null) {
                log.warn("[DemoInit] sql/minimax-seed.sql not found, skipping demo DB init");
                conn.close();
                return;
            }
            String fullSql = new String(res.readAllBytes());
            res.close();

            // 提取 demo_user/demo_category/demo_product/demo_order/demo_order_item/demo_payment 的 CREATE + INSERT
            String sql = extractDemoSection(fullSql);

            try (Statement stmt = conn.createStatement()) {
                conn.setAutoCommit(false);
                for (String batch : sql.split(";")) {
                    String trimmed = batch.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("SET")) {
                        conn.commit();
                        continue;
                    }
                    try {
                        stmt.execute(trimmed);
                    } catch (Exception e) {
                        // 忽略 H2 不支持的语法警告 (如 MySQL 特有语法)
                    }
                    conn.commit();
                }
            }
            conn.close();
            log.info("[DemoInit] demo H2 database (demo_order/demo_user/demo_product) initialized");
        } catch (Exception e) {
            log.warn("[DemoInit] demo DB init skipped (non-h2local or already exists): {}", e.getMessage());
        }
    }

    /**
     * V7.2: 从 minimax-seed.sql 提取 demo_* 表段 (CREATE + INSERT)
     * 用于在 sandbox H2 内存库里重建 demo 数据
     */
    private String extractDemoSection(String fullSql) {
        StringBuilder sb = new StringBuilder();
        String[] demoTables = {"demo_user", "demo_category", "demo_product",
                "demo_order", "demo_order_item", "demo_payment"};
        String[] lines = fullSql.split("\n");
        boolean inDemoSection = false;
        int braceDepth = 0;
        boolean inValuesList = false;
        for (String line : lines) {
            String trimmed = line.trim();
            // 段头标记
            if (trimmed.contains("演示电商数据") || trimmed.contains("原 demo-init.sql")) {
                inDemoSection = true;
                sb.append(line).append("\n");
                continue;
            }
            if (!inDemoSection) continue;
            // 收集所有内容
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private void insertDemoDataSource() {
        try {
            List<DataSource> existing = dataSourceMapper.selectList(
                new LambdaQueryWrapper<DataSource>()
                    .eq(DataSource::getName, "演示电商库 (H2)"));
            if (!existing.isEmpty()) {
                log.info("[DemoInit] demo datasource already exists, skipping");
                return;
            }

            DataSource demo = new DataSource();
            demo.setUserId(1L);
            demo.setName("演示电商库 (H2)");
            demo.setType("h2");
            demo.setJdbcUrl("jdbc:h2:mem:demo;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
            demo.setUsername("sa");
            demo.setPasswordEnc("");
            demo.setDescription("内置演示数据库 — 电商订单/用户/商品数据, 驼峰字段");
            demo.setCreatedAt(LocalDateTime.now());
            demo.setUpdatedAt(LocalDateTime.now());
            dataSourceMapper.insert(demo);

            log.info("[DemoInit] demo datasource inserted: id={}, jdbcUrl=jdbc:h2:mem:demo", demo.getId());
        } catch (Exception e) {
            log.warn("[DemoInit] insert demo datasource failed: {}", e.getMessage());
        }
    }
}
