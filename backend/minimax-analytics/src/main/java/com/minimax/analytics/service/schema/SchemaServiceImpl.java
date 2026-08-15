package com.minimax.analytics.service.schema;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.minimax.analytics.vo.ColumnInfo;
import com.minimax.analytics.vo.IndexInfo;
import com.minimax.analytics.vo.TableInfo;
import com.minimax.common.exception.BizException;
import com.minimax.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.time.Duration;
import java.util.*;

/**
 * Schema 服务实现 (V5.31)
 *
 * 直接用 JDBC 读 information_schema, 避免引入 Hibernate/JPA
 * Caffeine 缓存表结构 (TTL 1h), profile 走异步
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaServiceImpl implements SchemaService {

    private final com.minimax.analytics.service.datasource.DataSourceService dataSourceService;

    @Value("${analytics.schema.cache-ttl-minutes:60}")
    private int cacheTtlMinutes;

    @Value("${analytics.schema.profile-sample-size:1000}")
    private int profileSampleSize;

    /** 表结构缓存: key=dsId+db+table, value=TableInfo */
    private final Cache<String, TableInfo> tableCache = Caffeine.newBuilder()
            .maximumSize(2000)
            .expireAfterWrite(Duration.ofMinutes(60))
            .build();

    @Override
    public List<String> listDatabases(Long dataSourceId) {
        return executeQuery(dataSourceId, "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA " +
                "WHERE SCHEMA_NAME NOT IN ('mysql','information_schema','performance_schema','sys') " +
                "ORDER BY SCHEMA_NAME", rs -> {
            List<String> list = new ArrayList<>();
            while (rs.next()) list.add(rs.getString(1));
            return list;
        });
    }

    @Override
    public List<TableInfo> listTables(Long dataSourceId, String database, String keyword) {
        String sql = "SELECT TABLE_NAME, TABLE_COMMENT, ENGINE, TABLE_ROWS, " +
                "DATA_LENGTH, INDEX_LENGTH, CREATE_TIME, UPDATE_TIME " +
                "FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? " +
                (keyword != null && !keyword.isBlank() ? "AND TABLE_NAME LIKE ?" : "") +
                " ORDER BY TABLE_NAME";
        return executeQuery(dataSourceId, sql, rs -> {
            List<TableInfo> list = new ArrayList<>();
            int idx = 1;
            String db = database;
            while (rs.next()) {
                if (keyword != null && !keyword.isBlank()) {
                    if (idx == 1) continue; // skip first empty row
                }
                idx++;
                list.add(TableInfo.builder()
                        .name(rs.getString("TABLE_NAME"))
                        .comment(rs.getString("TABLE_COMMENT"))
                        .engine(rs.getString("ENGINE"))
                        .rowCount(rs.getLong("TABLE_ROWS"))
                        .dataSize(rs.getLong("DATA_LENGTH"))
                        .indexSize(rs.getLong("INDEX_LENGTH"))
                        .createTime(rs.getString("CREATE_TIME"))
                        .updateTime(rs.getString("UPDATE_TIME"))
                        .build());
            }
            return list;
        }, database, keyword == null ? null : "%" + keyword + "%");
    }

    @Override
    public TableInfo describeTable(Long dataSourceId, String database, String tableName) {
        String cacheKey = dataSourceId + ":" + database + ":" + tableName + ":desc";
        TableInfo cached = tableCache.getIfPresent(cacheKey);
        if (cached != null) return cached;

        // 1. 表基础信息
        TableInfo info = executeQuery(dataSourceId,
                "SELECT TABLE_NAME, TABLE_COMMENT, ENGINE, TABLE_ROWS, " +
                "DATA_LENGTH, INDEX_LENGTH, CREATE_TIME, UPDATE_TIME " +
                "FROM information_schema.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME=?",
                rs -> {
                    if (!rs.next()) throw new BizException(ResultCode.NOT_FOUND, "表不存在: " + database + "." + tableName);
                    return TableInfo.builder()
                            .name(rs.getString("TABLE_NAME"))
                            .comment(rs.getString("TABLE_COMMENT"))
                            .engine(rs.getString("ENGINE"))
                            .rowCount(rs.getLong("TABLE_ROWS"))
                            .dataSize(rs.getLong("DATA_LENGTH"))
                            .indexSize(rs.getLong("INDEX_LENGTH"))
                            .createTime(rs.getString("CREATE_TIME"))
                            .updateTime(rs.getString("UPDATE_TIME"))
                            .build();
                }, database, tableName);

        // 2. 列信息
        List<ColumnInfo> columns = executeQuery(dataSourceId,
                "SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT, IS_NULLABLE, " +
                "COLUMN_DEFAULT, COLUMN_KEY, ORDINAL_POSITION " +
                "FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=? AND TABLE_NAME=? " +
                "ORDER BY ORDINAL_POSITION",
                rs -> {
                    List<ColumnInfo> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(ColumnInfo.builder()
                                .name(rs.getString("COLUMN_NAME"))
                                .type(rs.getString("COLUMN_TYPE"))
                                .comment(rs.getString("COLUMN_COMMENT"))
                                .nullable("YES".equals(rs.getString("IS_NULLABLE")))
                                .defaultValue(rs.getString("COLUMN_DEFAULT"))
                                .keyType(rs.getString("COLUMN_KEY"))
                                .ordinalPosition(rs.getLong("ORDINAL_POSITION"))
                                .build());
                    }
                    return list;
                }, database, tableName);
        info.setColumns(columns);

        // 3. 索引
        List<IndexInfo> indexes = executeQuery(dataSourceId,
                "SELECT INDEX_NAME, NON_UNIQUE, INDEX_TYPE, SEQ_IN_INDEX, COLUMN_NAME " +
                "FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=? AND TABLE_NAME=? " +
                "ORDER BY INDEX_NAME, SEQ_IN_INDEX",
                rs -> {
                    List<IndexInfo> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(IndexInfo.builder()
                                .name(rs.getString("INDEX_NAME"))
                                .unique(rs.getInt("NON_UNIQUE") == 0)
                                .type(rs.getString("INDEX_TYPE"))
                                .seqInIndex(rs.getInt("SEQ_IN_INDEX"))
                                .columnName(rs.getString("COLUMN_NAME"))
                                .build());
                    }
                    return list;
                }, database, tableName);
        info.setIndexes(indexes);

        // 4. DDL (V5.31: SHOW CREATE TABLE)
        String ddl = executeQuery(dataSourceId, "SHOW CREATE TABLE `" + database + "`.`" + tableName + "`", rs -> {
            if (rs.next()) return rs.getString(2);
            return null;
        });
        info.setDdl(ddl);

        // 5. 样本 (前 5 行)
        try {
            List<Map<String, Object>> sample = executeQuery(dataSourceId,
                    "SELECT * FROM `" + database + "`.`" + tableName + "` LIMIT 5",
                    rs -> {
                        ResultSetMetaData md = rs.getMetaData();
                        int n = md.getColumnCount();
                        List<Map<String, Object>> rows = new ArrayList<>();
                        while (rs.next()) {
                            Map<String, Object> row = new LinkedHashMap<>();
                            for (int i = 1; i <= n; i++) row.put(md.getColumnLabel(i), rs.getObject(i));
                            rows.add(row);
                        }
                        return rows;
                    });
            info.setSample(sample);
        } catch (Exception e) {
            log.warn("采样前 5 行失败: {}", e.getMessage());
            info.setSample(List.of());
        }

        tableCache.put(cacheKey, info);
        return info;
    }

    @Override
    public TableInfo profileTable(Long dataSourceId, String database, String tableName) {
        // V5.31: 复用 describe 拿结构, 然后每列跑统计
        TableInfo info = describeTable(dataSourceId, database, tableName);
        List<ColumnInfo> columns = info.getColumns();
        if (columns == null) return info;

        // 估算行数 (V5.31: 用 information_schema.TABLES.TABLE_ROWS 即可)
        long rowCount = info.getRowCount() != null ? info.getRowCount() : profileSampleSize;
        long sampleSize = Math.min(rowCount, profileSampleSize);

        for (ColumnInfo col : columns) {
            String colName = col.getName();
            String colType = col.getType() == null ? "" : col.getType().toLowerCase();
            try {
                // 空值数 + 不同值数
                Map<String, Object> stats = executeQuery(dataSourceId,
                        "SELECT " +
                        "  SUM(CASE WHEN `" + colName + "` IS NULL THEN 1 ELSE 0 END) AS nulls, " +
                        "  COUNT(DISTINCT `" + colName + "`) AS distincts " +
                        "FROM (SELECT * FROM `" + database + "`.`" + tableName + "` LIMIT " + (int) sampleSize + ") t",
                        rs -> {
                            if (rs.next()) {
                                Map<String, Object> m = new HashMap<>();
                                m.put("nulls", rs.getObject("nulls"));
                                m.put("distincts", rs.getObject("distincts"));
                                return m;
                            }
                            return Map.of("nulls", 0, "distincts", 0);
                        });
                Object nullsObj = stats.get("nulls");
                col.setNullCount(nullsObj == null ? 0L : ((Number) nullsObj).longValue());
                Object distObj = stats.get("distincts");
                col.setDistinctCount(distObj == null ? 0L : ((Number) distObj).longValue());
                col.setNullRate(sampleSize > 0 ? (double) col.getNullCount() / sampleSize : 0.0);

                // 数值列额外算 min/max/avg
                if (colType.contains("int") || colType.contains("decimal") || colType.contains("double") || colType.contains("float")) {
                    Map<String, Object> numStats = executeQuery(dataSourceId,
                            "SELECT MIN(`" + colName + "`) mn, MAX(`" + colName + "`) mx, AVG(`" + colName + "`) av " +
                            "FROM `" + database + "`.`" + tableName + "` WHERE `" + colName + "` IS NOT NULL",
                            rs -> {
                                if (rs.next()) {
                                    Map<String, Object> m = new HashMap<>();
                                    m.put("mn", rs.getObject("mn"));
                                    m.put("mx", rs.getObject("mx"));
                                    m.put("av", rs.getObject("av"));
                                    return m;
                                }
                                return Map.of();
                            });
                    col.setMinValue(numStats.get("mn"));
                    col.setMaxValue(numStats.get("mx"));
                    col.setAvgValue(numStats.get("av") == null ? null : ((Number) numStats.get("av")).doubleValue());
                }

                // top 5 values (V5.31: 简易版, 不分类型)
                List<String> top = executeQuery(dataSourceId,
                        "SELECT CAST(`" + colName + "` AS CHAR) AS v, COUNT(*) c " +
                        "FROM `" + database + "`.`" + tableName + "` " +
                        "WHERE `" + colName + "` IS NOT NULL " +
                        "GROUP BY v ORDER BY c DESC LIMIT 5",
                        rs -> {
                            List<String> list = new ArrayList<>();
                            while (rs.next()) {
                                Object v = rs.getObject("v");
                                list.add(v == null ? "" : v.toString());
                            }
                            return list;
                        });
                col.setTopValues(top);
            } catch (Exception e) {
                log.warn("列画像失败 {}.{}: {}", tableName, colName, e.getMessage());
            }
        }
        return info;
    }

    @Override
    public Map<String, Object> buildErGraph(Long dataSourceId, String database) {
        // V5.31: 读 KEY_COLUMN_USAGE + REFERENTIAL_CONSTRAINTS, 生成 ECharts graph data
        List<Map<String, Object>> edges = executeQuery(dataSourceId,
                "SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME " +
                "FROM information_schema.KEY_COLUMN_USAGE " +
                "WHERE TABLE_SCHEMA=? AND REFERENCED_TABLE_NAME IS NOT NULL",
                rs -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    while (rs.next()) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("source", rs.getString("TABLE_NAME") + "." + rs.getString("COLUMN_NAME"));
                        m.put("target", rs.getString("REFERENCED_TABLE_NAME") + "." + rs.getString("REFERENCED_COLUMN_NAME"));
                        list.add(m);
                    }
                    return list;
                }, database);

        // 收集 nodes
        Set<String> nodes = new LinkedHashSet<>();
        for (Map<String, Object> e : edges) {
            nodes.add((String) e.get("source"));
            nodes.add((String) e.get("target"));
        }

        List<Map<String, Object>> nodeList = new ArrayList<>();
        for (String n : nodes) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", n);
            node.put("name", n);
            nodeList.add(node);
        }

        Map<String, Object> graph = new HashMap<>();
        graph.put("nodes", nodeList);
        graph.put("links", edges);
        graph.put("categories", List.of(Map.of("name", "Tables")));
        return graph;
    }

    // ----- helpers -----

    @FunctionalInterface
    private interface RsHandler<T> { T handle(ResultSet rs) throws Exception; }

    private <T> T executeQuery(Long dataSourceId, String sql, RsHandler<T> handler, Object... params) {
        DataSource ds = dataSourceService.getDataSource(dataSourceId);
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] == null) ps.setObject(i + 1, null);
                else ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return handler.handle(rs);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Schema 查询失败: {}", e.getMessage(), e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "Schema 查询失败: " + e.getMessage());
        }
    }
}
