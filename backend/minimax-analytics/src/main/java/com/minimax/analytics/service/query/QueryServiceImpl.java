package com.minimax.analytics.service.query;

import com.minimax.analytics.dto.QueryRequest;
import com.minimax.analytics.service.datasource.DataSourceService;
import com.minimax.analytics.service.nlsql.SqlSafetyChecker;
import com.minimax.analytics.vo.QueryResult;
import com.minimax.common.exception.BizException;
import com.minimax.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * SQL 查询服务实现 (V5.31)
 *
 * 流程: 校验 → 拿连接 → setQueryTimeout → setMaxRows → 执行 → 转 Map
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryServiceImpl implements QueryService {

    private final DataSourceService dataSourceService;
    private final SqlSafetyChecker safetyChecker;

    @Override
    public QueryResult execute(QueryRequest request) {
        SqlSafetyChecker.SafetyResult safety = safetyChecker.check(request.getSql(), request.getMaxRows());
        safety.throwIfFail();

        long t0 = System.currentTimeMillis();
        DataSource ds = dataSourceService.getDataSource(request.getDataSourceId());
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(safety.sql())) {
            ps.setQueryTimeout(request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : 30);
            ps.setMaxRows(safety.maxRows());
            ps.setFetchSize(100);

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

                return QueryResult.builder()
                        .columns(cols)
                        .rows(rows)
                        .rowCount((long) rows.size())
                        .durationMs(System.currentTimeMillis() - t0)
                        .build();
            }
        } catch (SQLException e) {
            log.error("SQL 执行失败: {}", e.getMessage());
            throw new BizException(ResultCode.SYSTEM_ERROR, "SQL 执行失败: " + e.getMessage());
        }
    }

    @Override
    public QueryResult explain(QueryRequest request) {
        // EXPLAIN 不走 SqlSafetyChecker (因为 EXPLAIN 不是 SELECT)
        long t0 = System.currentTimeMillis();
        DataSource ds = dataSourceService.getDataSource(request.getDataSourceId());
        String explainSql = "EXPLAIN " + request.getSql();
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(explainSql)) {
            ps.setQueryTimeout(10);
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
                QueryResult qr = QueryResult.builder()
                        .columns(cols)
                        .rows(rows)
                        .rowCount((long) rows.size())
                        .durationMs(System.currentTimeMillis() - t0)
                        .build();
                qr.setExplain("EXPLAIN: " + explainSql);
                return qr;
            }
        } catch (SQLException e) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "EXPLAIN 失败: " + e.getMessage());
        }
    }
}
