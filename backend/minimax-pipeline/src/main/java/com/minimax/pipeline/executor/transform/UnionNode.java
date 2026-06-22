package com.minimax.pipeline.executor.transform;

import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.executor.ExecutionContext;
import com.minimax.pipeline.executor.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * UNION 节点 (V5.32) - 2 入度, 合并 (去重/不去重)
 *
 * config: {
 *   type: "UNION" | "UNION_ALL"   // 默认 UNION_ALL
 * }
 */
@Slf4j
@Component
public class UnionNode extends NodeExecutor {

    @Override
    public NodeType supportedType() { return NodeType.UNION; }

    @Override
    protected List<Map<String, Object>> doExecute(String nodeId, Map<String, Object> config,
                                                   Map<String, List<Map<String, Object>>> inputs,
                                                   ExecutionContext ctx) {
        String type = ((String) config.getOrDefault("type", "UNION_ALL")).toUpperCase();
        boolean all = "UNION_ALL".equals(type);
        log.info("[{}] Union: {}", nodeId, type);

        Iterator<List<Map<String, Object>>> it = inputs.values().iterator();
        List<Map<String, Object>> a = it.next(), b = it.next();
        List<Map<String, Object>> out = new ArrayList<>(a);
        out.addAll(b);
        if (!all) {
            // 去重 (按行 toString 简单去重)
            Set<String> seen = new HashSet<>();
            List<Map<String, Object>> dedup = new ArrayList<>();
            for (Map<String, Object> r : out) {
                if (seen.add(r.toString())) dedup.add(r);
            }
            return dedup;
        }
        return out;
    }
}
