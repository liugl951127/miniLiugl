package com.minimax.analytics.service.chart;

import com.minimax.analytics.vo.QueryResult;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 图表类型决策器 (V5.31)
 *
 * 规则:
 *   - 1 列 数值 → 柱状图
 *   - 2 列 (类别+数值) → 柱状图/折线图 (按类别数: ≤10 柱, >10 折线)
 *   - 1 列 类别 (1 行多列) → 饼图
 *   - 2 列 (时间+数值) → 折线图
 *   - 多列数值 → 多 series 折线
 */
@Component
public class ChartTypeDecider {

    public enum ChartType { BAR, LINE, PIE, SCATTER, TABLE }

    public static class Decision {
        public ChartType type;
        public String xField;       // x 轴字段
        public String yField;       // y 轴字段 (单系列)
        public List<String> yFields; // 多系列
        public String title;
    }

    public Decision decide(QueryResult result) {
        Decision d = new Decision();
        if (result.getColumns() == null || result.getColumns().isEmpty() || result.getRows() == null || result.getRows().isEmpty()) {
            d.type = ChartType.TABLE;
            return d;
        }
        List<String> cols = result.getColumns();
        // 找数值列
        List<String> numericCols = new ArrayList<>();
        String categoryCol = null;
        String dateCol = null;
        for (String c : cols) {
            Class<?> type = inferType(result.getRows(), c);
            if (type == Number.class) numericCols.add(c);
            else if (type == Date.class) dateCol = c;
            else if (categoryCol == null) categoryCol = c;
        }
        if (numericCols.isEmpty()) {
            d.type = ChartType.TABLE;
            return d;
        }
        if (dateCol != null) {
            d.type = ChartType.LINE;
            d.xField = dateCol;
            d.yFields = numericCols;
        } else if (numericCols.size() == 1) {
            String y = numericCols.get(0);
            String x = categoryCol != null ? categoryCol : cols.get(0).equals(y) ? (cols.size() > 1 ? cols.get(1) : cols.get(0)) : cols.get(0);
            d.type = result.getRows().size() <= 10 ? ChartType.BAR : ChartType.LINE;
            d.xField = x;
            d.yField = y;
        } else {
            d.type = ChartType.LINE;
            d.xField = categoryCol != null ? categoryCol : cols.get(0);
            d.yFields = numericCols;
        }
        return d;
    }

    private Class<?> inferType(List<Map<String, Object>> rows, String col) {
        int numCnt = 0, dateCnt = 0, total = 0;
        for (Map<String, Object> r : rows) {
            Object v = r.get(col);
            if (v == null) continue;
            total++;
            String s = v.toString();
            if (s.matches("-?\\d+(\\.\\d+)?")) numCnt++;
            else if (s.matches("\\d{4}-\\d{2}-\\d{2}.*")) dateCnt++;
        }
        if (total == 0) return String.class;
        if (numCnt == total) return Number.class;
        if (dateCnt == total) return Date.class;
        return String.class;
    }
}
