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
 * 分布分析工具 (V2.7)
 *
 * <p>分析数值列的分布特征:
 *   - 直方图 (Histogram)
 *   - 分位数 (Quartiles: Q1/Q2/Q3)
 *   - 五数概括 (Min/Q1/Median/Q3/Max)
 *   - 偏度 (Skewness) + 峰度 (Kurtosis)
 *   - 频率分布 (Frequency Distribution, 用于分类列)
 *   - 箱线图数据 (Box Plot)</p>
 *
 * <h3>算法</h3>
 * <ul>
 *   <li>直方图: 桶数 = sqrt(n) 或指定 buckets
 *   <li>分位数: 排序后线性插值
 *   <li>偏度: E[((X-mean)/std)^3]
 *   <li>峰度: E[((X-mean)/std)^4] - 3
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributionTool implements AiToolExecutor {

    private final DataSourceMapper dataSourceMapper;
    private final MultiDataSourceManager dsManager;

    @Override
    public String getCode() {
        return "data.analyze.distribution";
    }

    @Override
    public Object execute(AiTool tool, Map<String, Object> input) throws Exception {
        Long dataSourceId = ((Number) input.get("dataSourceId")).longValue();
        String table = (String) input.get("table");
        String column = (String) input.get("column");
        Integer buckets = input.get("buckets") != null ? ((Number) input.get("buckets")).intValue() : 10;
        Integer limit = input.get("limit") != null ? ((Number) input.get("limit")).intValue() : 10000;

        if (column == null) throw new IllegalArgumentException("column is required");

        DbDataSource ds = dataSourceMapper.selectById(dataSourceId);
        if (ds == null) throw new IllegalArgumentException("Data source not found");

        long start = System.currentTimeMillis();

        // 1. 拉取数据
        String sql = "SELECT " + column + " FROM " + table + " LIMIT " + limit;
        List<Double> values = new ArrayList<>();
        javax.sql.DataSource ds_ = dsManager.getDataSource(ds);
        try (Connection conn = ds_.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Object v = rs.getObject(1);
                if (v instanceof Number) values.add(((Number) v).doubleValue());
            }
        }

        if (values.isEmpty()) {
            return Map.of("error", "no data", "rowCount", 0);
        }

        // 2. 排序
        Collections.sort(values);
        int n = values.size();
        double min = values.get(0);
        double max = values.get(n - 1);
        double sum = 0;
        for (double v : values) sum += v;
        double mean = sum / n;

        // 3. 基础统计
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rowCount", n);
        result.put("min", min);
        result.put("max", max);
        result.put("mean", mean);
        result.put("range", max - min);

        // 4. 分位数 (Q1, Q2=median, Q3)
        result.put("q1", quantile(values, 0.25));
        result.put("median", quantile(values, 0.5));
        result.put("q3", quantile(values, 0.75));
        result.put("iqr", (double) result.get("q3") - (double) result.get("q1"));

        // 5. 五数概括 (用于箱线图)
        Map<String, Object> five = new LinkedHashMap<>();
        five.put("min", min);
        five.put("q1", result.get("q1"));
        five.put("median", result.get("median"));
        five.put("q3", result.get("q3"));
        five.put("max", max);
        // 离群点边界 (1.5 * IQR)
        double iqr = (double) result.get("iqr");
        five.put("lowerFence", (double) result.get("q1") - 1.5 * iqr);
        five.put("upperFence", (double) result.get("q3") + 1.5 * iqr);
        result.put("fiveNumberSummary", five);

        // 6. 方差 / 标准差
        double variance = 0;
        for (double v : values) variance += (v - mean) * (v - mean);
        variance /= n;
        result.put("variance", variance);
        result.put("stdDev", Math.sqrt(variance));

        // 7. 偏度 (Skewness)
        double skewness = 0;
        if (variance > 0) {
            for (double v : values) {
                double d = (v - mean) / Math.sqrt(variance);
                skewness += d * d * d;
            }
            skewness /= n;
        }
        result.put("skewness", skewness);

        // 8. 峰度 (Kurtosis)
        double kurtosis = 0;
        if (variance > 0) {
            for (double v : values) {
                double d = (v - mean) / Math.sqrt(variance);
                kurtosis += d * d * d * d;
            }
            kurtosis = kurtosis / n - 3;  // 减 3 得到 excess kurtosis
        }
        result.put("kurtosis", kurtosis);

        // 9. 直方图 (n 桶)
        List<Map<String, Object>> histogram = new ArrayList<>();
        if (max > min) {
            double bucketSize = (max - min) / buckets;
            int[] counts = new int[buckets];
            for (double v : values) {
                int idx = Math.min(buckets - 1, (int) ((v - min) / bucketSize));
                counts[idx]++;
            }
            for (int i = 0; i < buckets; i++) {
                Map<String, Object> bin = new LinkedHashMap<>();
                bin.put("rangeStart", min + i * bucketSize);
                bin.put("rangeEnd", min + (i + 1) * bucketSize);
                bin.put("count", counts[i]);
                bin.put("frequency", (double) counts[i] / n);
                histogram.add(bin);
            }
        }
        result.put("histogram", histogram);

        result.put("durationMs", System.currentTimeMillis() - start);
        return result;
    }

    /**
     * 分位数 (线性插值)
     */
    private double quantile(List<Double> sortedValues, double q) {
        if (sortedValues.isEmpty()) return 0;
        int n = sortedValues.size();
        double pos = q * (n - 1);
        int lower = (int) Math.floor(pos);
        int upper = (int) Math.ceil(pos);
        if (lower == upper) return sortedValues.get(lower);
        double frac = pos - lower;
        return sortedValues.get(lower) * (1 - frac) + sortedValues.get(upper) * frac;
    }
}
