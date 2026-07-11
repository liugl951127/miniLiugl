package com.minimax.ai.tool.builtin;

import com.minimax.ai.datasource.DynamicDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 时间序列预测工具 (V2.8.3) - 线性回归/移动平均/指数平滑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PredictionTool extends AbstractSimpleTool {

    private final DynamicDataSource dataSource;

    @Override
    public String getCode() { return "data.predict.linear"; }

    @Override
    public String getName() { return "线性预测"; }

    @Override
    public String getDescription() { return "线性回归 + 移动平均 + 指数平滑"; }

    @Override
    public String getCategory() { return "data.analyze"; }

    @Override
    protected Map<String, Object> doExecute(Map<String, Object> input) {
        String method = (String) input.getOrDefault("method", "linear");
        int periods = ((Number) input.getOrDefault("periods", 5)).intValue();
        double[] values;

        if (input.containsKey("dataSourceId")) {
            Long id = ((Number) input.get("dataSourceId")).longValue();
            String table = (String) input.get("table");
            String column = (String) input.get("column");
            int limit = ((Number) input.getOrDefault("limit", 100)).intValue();
            List<Map<String, Object>> rows = dataSource.query(id, table, limit, null);
            Collections.reverse(rows);
            values = new double[rows.size()];
            int idx = 0;
            for (Map<String, Object> r : rows) {
                Object v = r.get(column);
                if (v instanceof Number n) values[idx++] = n.doubleValue();
            }
            if (idx < values.length) values = Arrays.copyOf(values, idx);
        } else if (input.containsKey("values")) {
            @SuppressWarnings("unchecked")
            List<Number> list = (List<Number>) input.get("values");
            values = new double[list.size()];
            for (int i = 0; i < list.size(); i++) values[i] = list.get(i).doubleValue();
        } else {
            throw new IllegalArgumentException("需要 dataSourceId 或 values");
        }
        if (values.length < 3) throw new IllegalArgumentException("样本太少 (>=3)");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("method", method);
        result.put("inputSize", values.length);
        result.put("inputStats", stats(values));
        switch (method) {
            case "linear" -> {
                Map<String, Object> r2 = linearPredict(values, periods);
                result.put("forecast", r2.get("forecast"));
                result.put("slope", r2.get("slope"));
                result.put("intercept", r2.get("intercept"));
                result.put("rSquared", r2.get("rSquared"));
                result.put("equation", r2.get("equation"));
            }
            case "ma3" -> result.put("forecast", movingAverage(values, periods, 3));
            case "ma5" -> result.put("forecast", movingAverage(values, periods, 5));
            case "ma7" -> result.put("forecast", movingAverage(values, periods, 7));
            case "exp" -> result.put("forecast", expSmooth(values, periods, 0.3));
            default -> throw new IllegalArgumentException("不支持的 method: " + method);
        }
        return result;
    }

    public Map<String, Object> linearPredict(double[] y, int periods) {
        int n = y.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) { sumX += i; sumY += y[i]; sumXY += i * y[i]; sumX2 += i * i; }
        double denom = n * sumX2 - sumX * sumX;
        if (denom == 0) return Map.of("error", "数据方差为 0");
        double a = (n * sumXY - sumX * sumY) / denom;
        double b = (sumY - a * sumX) / n;
        double meanY = sumY / n;
        double ssRes = 0, ssTot = 0;
        for (int i = 0; i < n; i++) {
            double yp = a * i + b;
            ssRes += Math.pow(y[i] - yp, 2);
            ssTot += Math.pow(y[i] - meanY, 2);
        }
        double r2 = ssTot == 0 ? 1.0 : 1.0 - ssRes / ssTot;
        double std = Math.sqrt(ssRes / Math.max(n - 2, 1));

        List<Map<String, Object>> forecast = new ArrayList<>();
        for (int p = 1; p <= periods; p++) {
            int x = n - 1 + p;
            double yp = a * x + b;
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("period", p);
            f.put("value", Math.round(yp * 100.0) / 100.0);
            f.put("lower", Math.round((yp - 1.96 * std) * 100.0) / 100.0);
            f.put("upper", Math.round((yp + 1.96 * std) * 100.0) / 100.0);
            forecast.add(f);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("forecast", forecast);
        r.put("slope", Math.round(a * 10000.0) / 10000.0);
        r.put("intercept", Math.round(b * 100.0) / 100.0);
        r.put("rSquared", Math.round(r2 * 10000.0) / 10000.0);
        r.put("residualStd", Math.round(std * 100.0) / 100.0);
        r.put("equation", "y = " + a + " * x + " + b);
        return r;
    }

    public List<Double> movingAverage(double[] y, int periods, int window) {
        List<Double> result = new ArrayList<>();
        int n = y.length;
        if (n < window) { for (int p = 1; p <= periods; p++) result.add(y[n - 1]); return result; }
        double sum = 0;
        for (int i = n - window; i < n; i++) sum += y[i];
        double last = sum / window;
        for (int p = 1; p <= periods; p++) result.add(Math.round(last * 100.0) / 100.0);
        return result;
    }

    public List<Double> expSmooth(double[] y, int periods, double alpha) {
        List<Double> result = new ArrayList<>();
        double s = y[0];
        for (int i = 1; i < y.length; i++) s = alpha * y[i] + (1 - alpha) * s;
        for (int p = 1; p <= periods; p++) result.add(Math.round(s * 100.0) / 100.0);
        return result;
    }

    private Map<String, Object> stats(double[] y) {
        double sum = 0, min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (double v : y) { sum += v; if (v < min) min = v; if (v > max) max = v; }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("mean", Math.round(sum / y.length * 100.0) / 100.0);
        r.put("min", min); r.put("max", max);
        return r;
    }
}
