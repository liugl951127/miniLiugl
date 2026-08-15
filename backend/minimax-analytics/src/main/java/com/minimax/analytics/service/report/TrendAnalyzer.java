package com.minimax.analytics.service.report;

import com.minimax.analytics.vo.QueryResult;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 趋势分析器 (V5.31) - 移动平均 + 同比环比
 */
@Component
public class TrendAnalyzer {

    public static class TrendPoint {
        public String x;       // x 值 (时间/类别)
        public double raw;     // 原始值
        public double ma3;     // 3 点移动平均
        public Double yoy;     // 同比 (%)
        public Double qoq;     // 环比 (%)
    }

    /** 简单 3 点移动平均 */
    public List<TrendPoint> analyze(QueryResult result, String xCol, String yCol) {
        List<TrendPoint> out = new ArrayList<>();
        if (result.getRows() == null) return out;
        List<Double> ys = new ArrayList<>();
        for (Map<String, Object> r : result.getRows()) {
            Object v = r.get(yCol);
            try { ys.add(v == null ? 0 : Double.parseDouble(v.toString())); } catch (Exception e) { ys.add(0.0); }
        }
        for (int i = 0; i < result.getRows().size(); i++) {
            TrendPoint p = new TrendPoint();
            Object x = result.getRows().get(i).get(xCol);
            p.x = x == null ? "" : x.toString();
            p.raw = ys.get(i);
            if (i >= 2) p.ma3 = (ys.get(i) + ys.get(i - 1) + ys.get(i - 2)) / 3.0;
            else p.ma3 = ys.get(i);
            // 同比环比: V5.31 简化, 假设数据点足够时计算
            if (i >= 1) p.qoq = (ys.get(i) - ys.get(i - 1)) / (ys.get(i - 1) == 0 ? 1 : ys.get(i - 1)) * 100;
            if (i >= 12) p.yoy = (ys.get(i) - ys.get(i - 12)) / (ys.get(i - 12) == 0 ? 1 : ys.get(i - 12)) * 100;
            out.add(p);
        }
        return out;
    }
}
