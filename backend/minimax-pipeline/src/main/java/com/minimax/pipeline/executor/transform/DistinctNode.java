package com.minimax.pipeline.executor.transform;

import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.executor.ExecutionContext;
import com.minimax.pipeline.executor.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * DISTINCT 节点 (V5.32) - 按指定列去重
 *
 * config: {
 *   columns: ["user_id", "action"]   // 按这些列联合去重
 * }
 */
@Slf4j
@Component
public class DistinctNode extends NodeExecutor {

    @Override
    public NodeType supportedType() { return NodeType.DISTINCT; }

    @Override
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> doExecute(String nodeId, Map<String, Object> config,
                                                   Map<String, List<Map<String, Object>>> inputs,
                                                   ExecutionContext ctx) {
        List<String> cols = (List<String>) config.get("columns");
        log.info("[{}] Distinct: cols={}", nodeId, cols);

        List<Map<String, Object>> rows = inputs.values().iterator().next();
        if (cols == null || cols.isEmpty()) {
            // 全行去重
            Set<String> seen = new HashSet<>();
            List<Map<String, Object>> out = new ArrayList<>();
            for (Map<String, Object> r : rows) {
                if (seen.add(r.toString())) out.add(r);
            }
            return out;
        }
        Set<String> seen = new HashSet<>();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String key = cols.stream().map(c -> String.valueOf(r.get(c))).reduce((a, b) -> a + "\0" + b).orElse("");
            if (seen.add(key)) out.add(r);
        }
        return out;
    }
}
