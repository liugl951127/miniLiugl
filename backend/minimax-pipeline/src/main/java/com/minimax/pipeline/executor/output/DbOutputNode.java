package com.minimax.pipeline.executor.output;

import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.executor.ExecutionContext;
import com.minimax.pipeline.executor.NodeExecutor;
import com.minimax.pipeline.service.datasource.PipelineDataSourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

/**
 * DB OUTPUT 节点 (V5.32) - 写入数据库表
 *
 * config: {
 *   datasourceId: 1,
 *   database: "minimax_platform",
 *   table: "user_aggregate",
 *   mode: "INSERT" | "REPLACE" | "TRUNCATE_INSERT"  // 默认 INSERT
 *   batchSize: 500
 * }
 *
 * V5.32 简化: 不自动建表, 用户先在业务库 CREATE TABLE
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DbOutputNode extends NodeExecutor {

    private final PipelineDataSourceService dataSourceService;

    @Override
    public NodeType supportedType() { return NodeType.DB_OUTPUT; }

    @Override
    protected List<Map<String, Object>> doExecute(String nodeId, Map<String, Object> config,
                                                   Map<String, List<Map<String, Object>>> inputs,
                                                   ExecutionContext ctx) throws Exception {
        Long dsId = ((Number) config.get("datasourceId")).longValue();
        String database = (String) config.get("database");
        String table = (String) config.get("table");
        String mode = (String) config.getOrDefault("mode", "INSERT");
        int batchSize = config.get("batchSize") == null ? 500 : ((Number) config.get("batchSize")).intValue();

        List<Map<String, Object>> rows = inputs.values().iterator().next();
        if (rows.isEmpty()) {
            log.info("[{}] DB output: 0 rows, skip", nodeId);
            return rows;
        }
        log.info("[{}] DB output: {} mode={} -> {}.{}", nodeId, rows.size(), mode, database, table);

        javax.sql.DataSource ds = dataSourceService.getDataSource(dsId);
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            if ("TRUNCATE_INSERT".equals(mode)) {
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("TRUNCATE TABLE `" + database + "`.`" + table + "`");
                }
            }
            List<String> cols = new ArrayList<>(rows.get(0).keySet());
            String sql = buildInsertSql(database, table, cols, mode);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int count = 0;
                for (Map<String, Object> row : rows) {
                    for (int i = 0; i < cols.size(); i++) {
                        ps.setObject(i + 1, row.get(cols.get(i)));
                    }
                    ps.addBatch();
                    if (++count % batchSize == 0) ps.executeBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
        return rows;
    }

    private String buildInsertSql(String db, String table, List<String> cols, String mode) {
        String colList = String.join(",", cols);
        String placeholders = String.join(",", Collections.nCopies(cols.size(), "?"));
        String verb = switch (mode) {
            case "REPLACE" -> "REPLACE INTO";
            case "TRUNCATE_INSERT" -> "INSERT INTO";
            default -> "INSERT INTO";
        };
        return verb + " `" + db + "`.`" + table + "` (" + colList + ") VALUES (" + placeholders + ")";
    }
}
