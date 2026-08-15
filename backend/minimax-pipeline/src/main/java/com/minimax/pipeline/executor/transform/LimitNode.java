package com.minimax.pipeline.executor.transform;

import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.executor.ExecutionContext;
import com.minimax.pipeline.executor.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * LIMIT 节点 (V5.32) - 限制行数
 *
 * config: {
 *   limit: 100,      // 取前 N 行
 *   offset: 0        // 可选, 跳过前 M 行
 * }
 */
@Slf4j
@Component
public class LimitNode extends NodeExecutor {

    @Override
    public NodeType supportedType() { return NodeType.LIMIT; }

    @Override
    protected List<Map<String, Object>> doExecute(String nodeId, Map<String, Object> config,
                                                   Map<String, List<Map<String, Object>>> inputs,
                                                   ExecutionContext ctx) {
        Object limObj = config.get("limit");
        if (limObj == null) throw new IllegalArgumentException("LIMIT 缺 limit");
        int limit = ((Number) limObj).intValue();
        int offset = config.get("offset") == null ? 0 : ((Number) config.get("offset")).intValue();
        log.info("[{}] Limit: offset={} limit={}", nodeId, offset, limit);

        List<Map<String, Object>> rows = inputs.values().iterator().next();
        int from = Math.min(offset, rows.size());
        int to = Math.min(offset + limit, rows.size());
        return new ArrayList<>(rows.subList(from, to));
    }
}
