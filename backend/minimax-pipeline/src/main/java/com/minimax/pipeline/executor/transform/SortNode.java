package com.minimax.pipeline.executor.transform;

import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.executor.ExecutionContext;
import com.minimax.pipeline.executor.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * SORT 节点 (V5.32) - 内存排序
 *
 * config: {
 *   orders: [
 *     { "col": "created_at", "dir": "DESC" },
 *     { "col": "score", "dir": "ASC" }
 *   ]
 * }
 *
 * V5.32.x: 限制 10w 行, 大数据走临时表
 */
@Slf4j
@Component
public class SortNode extends NodeExecutor {

    @Override
    public NodeType supportedType() { return NodeType.SORT; }

    @Override
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> doExecute(String nodeId, Map<String, Object> config,
                                                   Map<String, List<Map<String, Object>>> inputs,
                                                   ExecutionContext ctx) {
        List<Map<String, Object>> orders = (List<Map<String, Object>>) config.get("orders");
        if (orders == null || orders.isEmpty()) throw new IllegalArgumentException("SORT 缺 orders");
        log.info("[{}] Sort: {} orders", nodeId, orders.size());

        List<Map<String, Object>> rows = new ArrayList<>(inputs.values().iterator().next());
        rows.sort((a, b) -> {
            for (Map<String, Object> order : orders) {
                String col = (String) order.get("col");
                String dir = ((String) order.getOrDefault("dir", "ASC")).toUpperCase();
                Object va = a.get(col), vb = b.get(col);
                int cmp = compareValues(va, vb);
                if (cmp != 0) return "DESC".equals(dir) ? -cmp : cmp;
            }
            return 0;
        });
        return rows;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private int compareValues(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        if (a instanceof Comparable) return ((Comparable) a).compareTo(b);
        return a.toString().compareTo(b.toString());
    }
}
