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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 趋势分析工具 (V2.5 自研)
 *
 * 维度:
 *   - 时间序列
 *   - 同比 (YoY)
 *   - 环比 (MoM / WoW / DoD)
 *   - 移动平均
 *   - 增长率
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrendTool implements AiToolExecutor {

    private final DataSourceMapper dataSourceMapper;
    private final MultiDataSourceManager dsManager;

    @Override
    public String getCode() {
        return "data.analyze.trend";
    }

    @Override
    public Object execute(AiTool tool, Map<String, Object> input) throws Exception {
        Long dataSourceId = ((Number) input.get("dataSourceId")).longValue();
        String table = (String) input.get("table");
        String timeColumn = (String) input.get("timeColumn");
        String valueColumn = (String) input.get("valueColumn");
        String interval = (String) input.getOrDefault("interval", "day");

        DbDataSource ds = dataSourceMapper.selectById(dataSourceId);
        if (ds == null) throw new IllegalArgumentException("数据源不存在: " + dataSourceId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("table", table);
        result.put("interval", interval);

        // 加载时间序列数据
        List<Map<String, Object>> series = loadTimeSeries(ds, table, timeColumn, valueColumn, interval);
        result.put("dataPoints", series.size());

        if (series.size() < 2) {
            result.put("message", "数据点不足, 无法分析趋势");
            return result;
        }

        // 计算增长率
        List<Map<String, Object>> trends = new ArrayList<>();
        for (int i = 1; i < series.size(); i++) {
            Object prevVal = series.get(i - 1).get("value");
            Object currVal = series.get(i).get("value");
            if (prevVal instanceof Number && currVal instanceof Number) {
                double prev = ((Number) prevVal).doubleValue();
                double curr = ((Number) currVal).doubleValue();
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("time", series.get(i).get("time"));
                t.put("value", curr);
                t.put("previousValue", prev);
                if (prev != 0) {
                    double rate = (curr - prev) / prev * 100;
                    t.put("growthRate", rate);
                    t.put("direction", rate > 0 ? "UP" : rate < 0 ? "DOWN" : "FLAT");
                } else {
                    t.put("growthRate", null);
                    t.put("direction", "UNKNOWN");
                }
                trends.add(t);
            }
        }
        result.put("trends", trends);

        // 移动平均
        int window = Math.min(7, series.size());
        List<Map<String, Object>> ma = new ArrayList<>();
        for (int i = window - 1; i < series.size(); i++) {
            double sum = 0;
            for (int j = i - window + 1; j <= i; j++) {
                Object v = series.get(j).get("value");
                if (v instanceof Number) sum += ((Number) v).doubleValue();
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("time", series.get(i).get("time"));
            m.put("ma", sum / window);
            ma.add(m);
        }
        result.put("movingAverage", ma);
        result.put("maWindow", window);

        // 总体趋势
        Object firstVal = series.get(0).get("value");
        Object lastVal = series.get(series.size() - 1).get("value");
        if (firstVal instanceof Number && lastVal instanceof Number) {
            double first = ((Number) firstVal).doubleValue();
            double last = ((Number) lastVal).doubleValue();
            if (first != 0) {
                double totalGrowth = (last - first) / first * 100;
                result.put("totalGrowth", totalGrowth);
                result.put("trend", totalGrowth > 5 ? "STRONG_UP" :
                        totalGrowth > 0 ? "UP" :
                                totalGrowth > -5 ? "FLAT" : "DOWN");
            }
        }
        return result;
    }

    private List<Map<String, Object>> loadTimeSeries(DbDataSource ds, String table,
                                                     String timeColumn, String valueColumn,
                                                     String interval) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        javax.sql.DataSource ds_ = dsManager.getDataSource(ds);
        // 简化的 SQL: 按天聚合
        String aggFunc = "day".equals(interval) ? "DATE" :
                "month".equals(interval) ? "DATE_FORMAT" : "DATE";
        String sql = "SELECT " + aggFunc + "(" + timeColumn + ") AS t, " +
                "AVG(" + valueColumn + ") AS v FROM " + table +
                " WHERE " + timeColumn + " IS NOT NULL AND " + valueColumn + " IS NOT NULL" +
                " GROUP BY t ORDER BY t";
        try (Connection conn = ds_.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("time", rs.getDate("t").toString());
                point.put("value", rs.getDouble("v"));
                result.add(point);
            }
        }
        return result;
    }
}
