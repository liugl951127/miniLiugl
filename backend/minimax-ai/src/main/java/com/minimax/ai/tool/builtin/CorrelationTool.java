package com.minimax.ai.tool.builtin;

import com.minimax.ai.datasource.DynamicDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 相关性分析工具 (V2.8.3) - Pearson / Spearman
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CorrelationTool extends AbstractSimpleTool {

    private final DynamicDataSource dataSource;

    @Override
    public String getCode() { return "data.analyze.correlation"; }

    @Override
    public String getName() { return "相关性分析"; }

    @Override
    public String getDescription() { return "Pearson / Spearman 相关系数"; }

    @Override
    public String getCategory() { return "data.analyze"; }

    @Override
    protected Map<String, Object> doExecute(Map<String, Object> input) {
        Long dataSourceId = ((Number) input.get("dataSourceId")).longValue();
        String table = (String) input.get("table");
        @SuppressWarnings("unchecked")
        List<String> columns = (List<String>) input.get("columns");
        String method = (String) input.getOrDefault("method", "pearson");
        int limit = ((Number) input.getOrDefault("limit", 1000)).intValue();
        if (columns == null || columns.size() < 2) throw new IllegalArgumentException("至少需要 2 列");

        String cols = String.join(",", columns);
        List<Map<String, Object>> rows = dataSource.query(dataSourceId, table, limit, null);
        if (rows.size() < 3) throw new IllegalArgumentException("样本太少 (>=3)");

        Map<String, double[]> data = new LinkedHashMap<>();
        for (String c : columns) {
            double[] arr = new double[rows.size()];
            int valid = 0;
            for (int i = 0; i < rows.size(); i++) {
                Object v = rows.get(i).get(c);
                if (v instanceof Number n) arr[valid++] = n.doubleValue();
            }
            if (valid < 3) throw new IllegalArgumentException("列 " + c + " 有效数值太少");
            data.put(c, Arrays.copyOf(arr, valid));
        }

        List<Map<String, Object>> pairs = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            for (int j = i + 1; j < columns.size(); j++) {
                String a = columns.get(i), b = columns.get(j);
                double[] va = data.get(a), vb = data.get(b);
                int n = Math.min(va.length, vb.length);
                double r = method.equalsIgnoreCase("spearman")
                        ? spearman(Arrays.copyOf(va, n), Arrays.copyOf(vb, n))
                        : pearson(Arrays.copyOf(va, n), Arrays.copyOf(vb, n));
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("colA", a);
                p.put("colB", b);
                p.put("coefficient", Math.round(r * 10000.0) / 10000.0);
                p.put("strength", strength(r));
                p.put("n", n);
                pairs.add(p);
            }
        }
        List<Map<String, Object>> strong = pairs.stream()
                .filter(p -> Math.abs((double) p.get("coefficient")) > 0.7)
                .sorted((a, b) -> Double.compare(Math.abs((double) b.get("coefficient")), Math.abs((double) a.get("coefficient"))))
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("method", method);
        result.put("table", table);
        result.put("rowCount", rows.size());
        result.put("pairs", pairs);
        result.put("strongCorrelations", strong);
        return result;
    }

    public double pearson(double[] x, double[] y) {
        int n = x.length;
        if (n != y.length || n < 2) return 0;
        double sumX = 0, sumY = 0;
        for (int i = 0; i < n; i++) { sumX += x[i]; sumY += y[i]; }
        double meanX = sumX / n, meanY = sumY / n;
        double num = 0, dx2 = 0, dy2 = 0;
        for (int i = 0; i < n; i++) {
            double dx = x[i] - meanX, dy = y[i] - meanY;
            num += dx * dy; dx2 += dx * dx; dy2 += dy * dy;
        }
        if (dx2 == 0 || dy2 == 0) return 0;
        return num / Math.sqrt(dx2 * dy2);
    }

    public double spearman(double[] x, double[] y) {
        return pearson(rank(x), rank(y));
    }

    private double[] rank(double[] arr) {
        int n = arr.length;
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, Comparator.comparingDouble(i -> arr[i]));
        double[] r = new double[n];
        for (int i = 0; i < n; i++) r[idx[i]] = i + 1;
        return r;
    }

    private String strength(double r) {
        double a = Math.abs(r);
        if (a >= 0.9) return "VERY_STRONG";
        if (a >= 0.7) return "STRONG";
        if (a >= 0.5) return "MODERATE";
        if (a >= 0.3) return "WEAK";
        return "VERY_WEAK";
    }
}
