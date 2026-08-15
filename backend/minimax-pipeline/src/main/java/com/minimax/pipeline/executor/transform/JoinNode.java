package com.minimax.pipeline.executor.transform;

import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.executor.ExecutionContext;
import com.minimax.pipeline.executor.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * JOIN 节点 (V5.32) - 2 入度, Hash join
 *
 * config: {
 *   type: "INNER" | "LEFT" | "RIGHT",   // 默认 INNER
 *   leftKey: "user_id",                 // 左表 join 字段
 *   rightKey: "id",                     // 右表 join 字段
 *   prefix: "right_"                    // 右表列加前缀防冲突, 可选
 * }
 *
 * 注意: inputs 的 2 个 entry 顺序由 PipelineEngine 决定 (入度顺序, 第一个为 left, 第二个为 right)
 */
@Slf4j
@Component
public class JoinNode extends NodeExecutor {

    @Override
    public NodeType supportedType() { return NodeType.JOIN; }

    @Override
    protected List<Map<String, Object>> doExecute(String nodeId, Map<String, Object> config,
                                                   Map<String, List<Map<String, Object>>> inputs,
                                                   ExecutionContext ctx) {
        String type = ((String) config.getOrDefault("type", "INNER")).toUpperCase();
        String leftKey = (String) config.get("leftKey");
        String rightKey = (String) config.get("rightKey");
        String prefix = (String) config.getOrDefault("prefix", "right_");
        if (leftKey == null || rightKey == null) {
            throw new IllegalArgumentException("JOIN 缺 leftKey 或 rightKey");
        }
        // inputs 是 2 个 entry: left (key="left"), right (key="right")
        // 按 key 排序保证顺序稳定 (HashMap/Map.of 迭代顺序 implementation-defined)
        List<Map.Entry<String, List<Map<String, Object>>>> sortedEntries = new ArrayList<>(inputs.entrySet());
        sortedEntries.sort(Comparator.comparing(Map.Entry::getKey));
        List<Map<String, Object>> left = sortedEntries.get(0).getValue();
        List<Map<String, Object>> right = sortedEntries.get(1).getValue();

        // Build hash index on right
        Map<Object, List<Map<String, Object>>> idx = new HashMap<>();
        for (Map<String, Object> r : right) {
            idx.computeIfAbsent(r.get(rightKey), k -> new ArrayList<>()).add(r);
        }
        log.info("[{}] Join: {} left={} right={} (keys: {}=>{})", nodeId, type, left.size(), right.size(), leftKey, rightKey);

        List<Map<String, Object>> out = new ArrayList<>();
        Set<Object> rightMatched = new HashSet<>();
        for (Map<String, Object> l : left) {
            Object key = l.get(leftKey);
            List<Map<String, Object>> matches = idx.getOrDefault(key, List.of());
            if (!matches.isEmpty()) {
                for (Map<String, Object> r : matches) {
                    out.add(merge(l, r, prefix));
                    rightMatched.add(r.get(rightKey));
                }
            } else if ("LEFT".equals(type) || "FULL".equals(type)) {
                out.add(merge(l, null, prefix));
            }
        }
        // RIGHT/FULL: 补未匹配的右行
        if ("RIGHT".equals(type) || "FULL".equals(type)) {
            for (Map<String, Object> r : right) {
                if (!rightMatched.contains(r.get(rightKey))) {
                    out.add(merge(null, r, prefix));
                }
            }
        }
        return out;
    }

    private Map<String, Object> merge(Map<String, Object> left, Map<String, Object> right, String prefix) {
        Map<String, Object> row = new LinkedHashMap<>();
        if (left != null) row.putAll(left);
        if (right != null) {
            for (Map.Entry<String, Object> e : right.entrySet()) {
                String k = e.getKey();
                if (row.containsKey(k) && !k.equals(prefix + e.getKey())) {
                    // 列冲突, 加前缀
                    row.put(prefix + k, e.getValue());
                } else {
                    row.put(k, e.getValue());
                }
            }
        }
        return row;
    }
}
