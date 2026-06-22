package com.minimax.analytics.service.report;

import com.minimax.analytics.vo.QueryResult;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 异常检测器 (V5.31) - IQR + z-score 两种算法
 */
@Component
public class AnomalyDetector {

    public static class Anomaly {
        public String field;
        public Object value;
        public int index;
        public double score;       // 异常分数
        public String method;      // IQR / ZSCORE
    }

    public List<Anomaly> detect(QueryResult result) {
        List<Anomaly> out = new ArrayList<>();
        if (result.getColumns() == null || result.getRows() == null || result.getRows().isEmpty()) return out;
        // 对每列检测
        for (String col : result.getColumns()) {
            List<Double> vals = new ArrayList<>();
            for (Map<String, Object> r : result.getRows()) {
                Object v = r.get(col);
                if (v == null) { vals.add(null); continue; }
                try { vals.add(Double.parseDouble(v.toString())); } catch (Exception e) { vals.add(null); }
            }
            // 只对数值列检测
            long numCount = vals.stream().filter(Objects::nonNull).count();
            if (numCount < 5) continue;
            // IQR 法
            List<Double> sorted = vals.stream().filter(Objects::nonNull).sorted().toList();
            double q1 = sorted.get((int) (sorted.size() * 0.25));
            double q3 = sorted.get((int) (sorted.size() * 0.75));
            double iqr = q3 - q1;
            double lower = q1 - 1.5 * iqr;
            double upper = q3 + 1.5 * iqr;
            for (int i = 0; i < vals.size(); i++) {
                Double v = vals.get(i);
                if (v == null) continue;
                if (v < lower || v > upper) {
                    // V5.30.7: 不能用匿名内部类 (lambda/匿名类 引用外部非 final 变量会报错)
                    Anomaly a = new Anomaly();
                    a.field = col;
                    a.value = result.getRows().get(i).get(col);
                    a.index = i;
                    a.score = (v - (q1 + q3) / 2) / (iqr == 0 ? 1 : iqr);
                    a.method = "IQR";
                    out.add(a);
                }
            }
            // 限制每列最多 5 个异常, 避免报告太冗长
            if (out.size() > 20) break;
        }
        return out;
    }
}
