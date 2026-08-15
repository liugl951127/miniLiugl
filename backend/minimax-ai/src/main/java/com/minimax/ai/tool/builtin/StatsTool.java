package com.minimax.ai.tool.builtin;

import com.minimax.ai.datasource.MultiDataSourceManager;
import com.minimax.ai.entity.AiTool;
import com.minimax.ai.entity.DbDataSource;
import com.minimax.ai.mapper.DataSourceMapper;
import com.minimax.ai.tool.AiToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;

/**
 * 描述统计工具 (V2.5 自研)
 *
 * 对数值列计算:
 *   - count / min / max / mean / sum / std
 *   - quartiles (25%, 50%, 75%)
 *   - variance
 *
 * 支持多数据库 (MySQL/PostgreSQL/Oracle/H2)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsTool implements AiToolExecutor {

    private final DataSourceMapper dataSourceMapper;
    private final MultiDataSourceManager dsManager;

    @Override
    public String getCode() {
        return "data.analyze.stats";
    }

    @Override
    public Object execute(AiTool tool, Map<String, Object> input) throws Exception {
        Long dataSourceId = ((Number) input.get("dataSourceId")).longValue();
        String table = (String) input.get("table");
        String column = (String) input.get("column");

        DbDataSource ds = dataSourceMapper.selectById(dataSourceId);
        if (ds == null) throw new IllegalArgumentException("数据源不存在: " + dataSourceId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dataSource", ds.getName());
        result.put("table", table);
        result.put("column", column);

        // 1. 原始数据
        List<Double> values = loadColumn(ds, table, column);
        result.put("count", values.size());

        if (values.isEmpty()) {
            result.put("message", "无数据");
            return result;
        }

        // 2. 基础统计
        double sum = 0, min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        for (double v : values) {
            sum += v;
            if (v < min) min = v;
            if (v > max) max = v;
        }
        double mean = sum / values.size();

        // 3. 标准差 / 方差
        double variance = 0;
        for (double v : values) variance += (v - mean) * (v - mean);
        variance /= values.size();
        double std = Math.sqrt(variance);

        // 4. 排序 + 分位数
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        double q25 = percentile(sorted, 25);
        double q50 = percentile(sorted, 50);
        double q75 = percentile(sorted, 75);

        result.put("sum", sum);
        result.put("mean", mean);
        result.put("min", min);
        result.put("max", max);
        result.put("range", max - min);
        result.put("variance", variance);
        result.put("std", std);
        result.put("q25", q25);
        result.put("q50_median", q50);
        result.put("q75", q75);
        result.put("iqr", q75 - q25);
        result.put("cv", mean != 0 ? std / Math.abs(mean) : 0); // 变异系数

        log.info("StatsTool: table={} column={} n={} mean={} std={}",
                table, column, values.size(), mean, std);
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
        if (sorted.isEmpty()) return 0;
        int n = sorted.size();
        double rank = p / 100.0 * (n - 1);
        int lower = (int) Math.floor(rank);
        int upper = (int) Math.ceil(rank);
        if (lower == upper) return sorted.get(lower);
        return sorted.get(lower) + (rank - lower) * (sorted.get(upper) - sorted.get(lower));
    }
}
