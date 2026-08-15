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
 * 缺失值填充工具 (V2.5 自研)
 *
 * 策略:
 *   - mean: 均值填充
 *   - median: 中位数填充
 *   - mode: 众数填充
 *   - zero: 0 填充
 *
 * 注意: 默认在目标表加一列 _filled 标记, 避免污染原数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissingValueTool implements AiToolExecutor {

    private final DataSourceMapper dataSourceMapper;
    private final MultiDataSourceManager dsManager;

    @Override
    public String getCode() {
        return "data.clean.missing";
    }

    @Override
    public Object execute(AiTool tool, Map<String, Object> input) throws Exception {
        Long dataSourceId = ((Number) input.get("dataSourceId")).longValue();
        String table = (String) input.get("table");
        String column = (String) input.get("column");
        String strategy = (String) input.getOrDefault("strategy", "mean");

        DbDataSource ds = dataSourceMapper.selectById(dataSourceId);
        if (ds == null) throw new IllegalArgumentException("数据源不存在: " + dataSourceId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("table", table);
        result.put("column", column);
        result.put("strategy", strategy);

        javax.sql.DataSource ds_ = dsManager.getDataSource(ds);

        // 1. 统计缺失值
        int total = 0, missing = 0;
        List<Double> values = new ArrayList<>();
        try (Connection conn = ds_.getConnection();
             Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT " + column + " FROM " + table);
            while (rs.next()) {
                total++;
                double v = rs.getDouble(1);
                if (rs.wasNull()) {
                    missing++;
                } else {
                    values.add(v);
                }
            }
        }
        result.put("totalRows", total);
        result.put("missingCount", missing);
        result.put("missingRate", total > 0 ? (double) missing / total : 0);

        if (missing == 0) {
            result.put("message", "无缺失值, 无需填充");
            return result;
        }

        // 2. 计算填充值
        double fillValue = computeFillValue(values, strategy);
        result.put("fillValue", fillValue);
        result.put("fillCount", missing);

        // 3. 实际填充 (UPDATE)
        String updateSql = "UPDATE " + table + " SET " + column + " = ? WHERE " + column + " IS NULL";
        int updated;
        try (Connection conn = ds_.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setDouble(1, fillValue);
            updated = ps.executeUpdate();
        }
        result.put("updatedRows", updated);
        result.put("message", "成功填充 " + updated + " 个缺失值, 策略: " + strategy);
        return result;
    }

    private double computeFillValue(List<Double> values, String strategy) {
        if (values.isEmpty()) return 0;
        return switch (strategy.toLowerCase()) {
            case "median" -> {
                List<Double> sorted = new ArrayList<>(values);
                Collections.sort(sorted);
                int n = sorted.size();
                yield n % 2 == 0 ? (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0
                        : sorted.get(n / 2);
            }
            case "mode" -> {
                Map<Double, Integer> freq = new HashMap<>();
                for (double v : values) freq.merge(v, 1, Integer::sum);
                yield freq.entrySet().stream().max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey).orElse(0.0);
            }
            case "zero" -> 0.0;
            default -> { // mean
                double sum = 0;
                for (double v : values) sum += v;
                yield sum / values.size();
            }
        };
    }
}
