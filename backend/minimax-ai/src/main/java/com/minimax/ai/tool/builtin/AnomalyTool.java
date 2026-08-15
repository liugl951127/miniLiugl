package com.minimax.ai.tool.builtin;

import com.minimax.ai.datasource.MultiDataSourceManager;
import com.minimax.ai.entity.AiTool;
import com.minimax.ai.entity.DbDataSource;
import com.minimax.ai.mapper.DataSourceMapper;
import com.minimax.ai.tool.AiToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

/**
 * 异常检测工具 (V2.5 自研)
 *
 * 方法:
 *   - Z-Score: |x - mean| / std > threshold 视为异常
 *   - IQR: x < Q1 - threshold*IQR 或 x > Q3 + threshold*IQR
 *
 * 输出:
 *   - 异常值列表
 *   - 异常率
 *   - 异常索引
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnomalyTool implements AiToolExecutor {

    private final DataSourceMapper dataSourceMapper;
    private final MultiDataSourceManager dsManager;

    @Override
    public String getCode() {
        return "data.analyze.anomaly";
    }

    @Override
    public Object execute(AiTool tool, Map<String, Object> input) throws Exception {
        Long dataSourceId = ((Number) input.get("dataSourceId")).longValue();
        String table = (String) input.get("table");
        String column = (String) input.get("column");
        String method = (String) input.getOrDefault("method", "zscore");
        double threshold = ((Number) input.getOrDefault("threshold", 3.0)).doubleValue();

        DbDataSource ds = dataSourceMapper.selectById(dataSourceId);
        if (ds == null) throw new IllegalArgumentException("数据源不存在: " + dataSourceId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("method", method);
        result.put("threshold", threshold);
        result.put("table", table);
        result.put("column", column);

        // 加载数据
        List<Double> values = loadColumn(ds, table, column);
        result.put("totalCount", values.size());

        if (values.isEmpty()) {
            result.put("anomalies", List.of());
            return result;
        }

        List<Map<String, Object>> anomalies = new ArrayList<>();

        if ("iqr".equalsIgnoreCase(method)) {
            // IQR 方法
            List<Double> sorted = new ArrayList<>(values);
            Collections.sort(sorted);
            double q1 = percentile(sorted, 25);
            double q3 = percentile(sorted, 75);
            double iqr = q3 - q1;
            double lower = q1 - threshold * iqr;
            double upper = q3 + threshold * iqr;

            for (int i = 0; i < values.size(); i++) {
                double v = values.get(i);
                if (v < lower || v > upper) {
                    Map<String, Object> a = new LinkedHashMap<>();
                    a.put("index", i);
                    a.put("value", v);
                    a.put("type", v < lower ? "LOW" : "HIGH");
                    a.put("bound", v < lower ? lower : upper);
                    anomalies.add(a);
                }
            }
            result.put("q1", q1);
            result.put("q3", q3);
            result.put("iqr", iqr);
            result.put("lowerBound", lower);
            result.put("upperBound", upper);
        } else {
            // Z-Score 方法
            double sum = 0;
            for (double v : values) sum += v;
            double mean = sum / values.size();
            double variance = 0;
            for (double v : values) variance += (v - mean) * (v - mean);
            variance /= values.size();
            double std = Math.sqrt(variance);

            for (int i = 0; i < values.size(); i++) {
                double v = values.get(i);
                if (std > 0) {
                    double z = Math.abs((v - mean) / std);
                    if (z > threshold) {
                        Map<String, Object> a = new LinkedHashMap<>();
                        a.put("index", i);
                        a.put("value", v);
                        a.put("zScore", z);
                        anomalies.add(a);
                    }
                }
            }
            result.put("mean", mean);
            result.put("std", std);
        }

        result.put("anomalyCount", anomalies.size());
        result.put("anomalyRate", (double) anomalies.size() / values.size());
        result.put("anomalies", anomalies);
        return result;
    }

    private List<Double> loadColumn(DbDataSource ds, String table, String column) throws Exception {
        List<Double> result = new ArrayList<>();
        javax.sql.DataSource ds_ = dsManager.getDataSource(ds);
        String sql = "SELECT " + column + " FROM " + table + " WHERE " + column + " IS NOT NULL";
        try (Connection conn = ds_.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double v = rs.getDouble(1);
                if (!rs.wasNull()) result.add(v);
            }
        }
        return result;
    }

    private double percentile(List<Double> sorted, double p) {
        int n = sorted.size();
        if (n == 0) return 0;
        double rank = p / 100.0 * (n - 1);
        int lower = (int) Math.floor(rank);
        int upper = (int) Math.ceil(rank);
        if (lower == upper) return sorted.get(lower);
        return sorted.get(lower) + (rank - lower) * (sorted.get(upper) - sorted.get(lower));
    }
}
