package com.minimax.pipeline.executor.transform;

import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.executor.ExecutionContext;
import com.minimax.pipeline.executor.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AGGREGATE 节点 (V5.32) - GROUP BY + 聚合
 *
 * config: {
 *   groupBy: ["category", "DATE(created_at)"],
 *   aggregations: [
 *     { "col": "id", "op": "COUNT", "alias": "cnt" },
 *     { "col": "amount", "op": "SUM", "alias": "total_amount" },
 *     { "col": "score", "op": "AVG", "alias": "avg_score" },
 *     { "col": "price", "op": "MAX", "alias": "max_price" },
 *     { "col": "discount", "op": "MIN", "alias": "min_discount" }
 *   ]
 * }
 */
@Slf4j
@Component
public class AggregateNode extends NodeExecutor {

    @Override
    public NodeType supportedType() { return NodeType.AGGREGATE; }

    @Override
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> doExecute(String nodeId, Map<String, Object> config,
                                                   Map<String, List<Map<String, Object>>> inputs,
                                                   ExecutionContext ctx) {
        List<String> groupBy = (List<String>) config.get("groupBy");
        List<Map<String, Object>> aggs = (List<Map<String, Object>>) config.get("aggregations");
        if (aggs == null || aggs.isEmpty()) throw new IllegalArgumentException("AGGREGATE 缺 aggregations");
        log.info("[{}] Aggregate: groupBy={} aggs={}", nodeId, groupBy, aggs.size());

        List<Map<String, Object>> rows = inputs.values().iterator().next();
        if (rows.isEmpty()) return List.of();

        // 分组
        Map<List<Object>, List<Map<String, Object>>> groups;
        if (groupBy == null || groupBy.isEmpty()) {
            // 全表聚合, 单组
            groups = new HashMap<>();
            groups.put(List.of(), rows);
        } else {
            groups = rows.stream().collect(Collectors.groupingBy(r ->
                    groupBy.stream().map(k -> r.get(k)).collect(Collectors.toList())));
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<List<Object>, List<Map<String, Object>>> entry : groups.entrySet()) {
            Map<String, Object> result = new LinkedHashMap<>();
            // group by 字段
            if (groupBy != null) {
                for (int i = 0; i < groupBy.size(); i++) {
                    result.put(groupBy.get(i), entry.getKey().get(i));
                }
            }
            // 聚合
            for (Map<String, Object> agg : aggs) {
                String col = (String) agg.get("col");
                String op = ((String) agg.get("op")).toUpperCase();
                String alias = (String) agg.get("alias");
                if (alias == null) alias = op.toLowerCase() + "_" + (col == null ? "*" : col);
                result.put(alias, applyAgg(entry.getValue(), col, op));
            }
            out.add(result);
        }
        return out;
    }

    private Object applyAgg(List<Map<String, Object>> rows, String col, String op) {
        switch (op) {
            case "COUNT": {
                if (col == null || "*".equals(col)) return rows.size();
                return rows.stream().filter(r -> r.get(col) != null).count();
            }
            case "SUM": return numericOp(rows, col, true);
            case "AVG": {
                List<Double> vals = numericVals(rows, col);
                if (vals.isEmpty()) return null;
                return vals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            }
            case "MAX": {
                List<Double> vals = numericVals(rows, col);
                if (vals.isEmpty()) return null;
                return vals.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            }
            case "MIN": {
                List<Double> vals = numericVals(rows, col);
                if (vals.isEmpty()) return null;
                return vals.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            }
            default: throw new IllegalArgumentException("不支持的聚合操作: " + op);
        }
    }

    private double numericOp(List<Map<String, Object>> rows, String col, boolean sum) {
        return numericVals(rows, col).stream().mapToDouble(Double::doubleValue).sum();
    }

    private List<Double> numericVals(List<Map<String, Object>> rows, String col) {
        List<Double> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Object v = r.get(col);
            if (v == null) continue;
            try { out.add(Double.parseDouble(v.toString())); } catch (Exception ignored) {}
        }
        return out;
    }
}
