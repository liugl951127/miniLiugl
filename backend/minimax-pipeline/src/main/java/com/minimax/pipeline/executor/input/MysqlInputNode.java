package com.minimax.pipeline.executor.input;

import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.executor.ExecutionContext;
import com.minimax.pipeline.executor.NodeExecutor;
import com.minimax.pipeline.service.datasource.PipelineDataSourceService;
import com.minimax.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

/**
 * MySQL INPUT 节点 (V5.32)
 *
 * config: {
 *   datasourceId: 1,           // 数据源 (minimax-analytics.DataSource)
 *   database: "minimax_platform",
 *   table: "user",             // 或 sql: "SELECT * FROM user WHERE ..."
 *   columns: ["id","name"],    // 可选, 默认 *
 *   where: "id > 0",           // 可选
 *   limit: 1000                // 可选
 * }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MysqlInputNode extends NodeExecutor {

    private final PipelineDataSourceService dataSourceService;

    @Override
    public NodeType supportedType() { return NodeType.MYSQL_INPUT; }

    @Override
    protected List<Map<String, Object>> doExecute(String nodeId, Map<String, Object> config,
                                                   Map<String, List<Map<String, Object>>> inputs,
                                                   ExecutionContext ctx) throws Exception {
        Long dsId = ((Number) config.get("datasourceId")).longValue();
        String database = (String) config.get("database");
        String table = (String) config.get("table");
        String sql = (String) config.get("sql");
        if (sql == null) {
            StringBuilder sb = new StringBuilder("SELECT ");
            Object cols = config.get("columns");
            sb.append(cols == null ? "*" : String.join(",", (List<String>) cols));
            sb.append(" FROM `").append(database).append("`.`").append(table).append("`");
            if (config.get("where") != null) sb.append(" WHERE ").append(config.get("where"));
            Object limit = config.get("limit");
            sb.append(" LIMIT ").append(limit == null ? 1000 : ((Number) limit).intValue());
            sql = sb.toString();
        }
        log.info("[{}] MySQL input: {}", nodeId, sql);
        javax.sql.DataSource ds = dataSourceService.getDataSource(dsId);
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setMaxRows(100000);
            ps.setQueryTimeout(60);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                List<String> cols = new ArrayList<>();
                for (int i = 1; i <= n; i++) cols.add(md.getColumnLabel(i));
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= n; i++) row.put(cols.get(i - 1), rs.getObject(i));
                    rows.add(row);
                }
                return rows;
            }
        }
    }
}
