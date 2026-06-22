package com.minimax.pipeline.executor;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行上下文 (V5.32)
 *
 * 节点间用 List<Map<String, Object>> 传递数据 (内存, 限制行数)
 * key = upstreamNodeId, value = rows
 */
@Data
public class ExecutionContext {

    private final Long runId;
    private final Long workflowId;
    private final Long userId;
    private final int maxRows;
    private final int outputPreviewRows;

    /** 节点间数据缓存: nodeId → rows */
    private final Map<String, List<Map<String, Object>>> nodeOutputs = new ConcurrentHashMap<>();

    /** 全局变量 (V5.32.x) */
    private final Map<String, Object> globalVars = new ConcurrentHashMap<>();

    public ExecutionContext(Long runId, Long workflowId, Long userId, int maxRows, int outputPreviewRows) {
        this.runId = runId;
        this.workflowId = workflowId;
        this.userId = userId;
        this.maxRows = maxRows;
        this.outputPreviewRows = outputPreviewRows;
    }

    public void putOutput(String nodeId, List<Map<String, Object>> rows) {
        if (rows == null) {
            nodeOutputs.put(nodeId, List.of());
            return;
        }
        if (rows.size() > maxRows) {
            throw new IllegalStateException(
                "节点 " + nodeId + " 输出 " + rows.size() + " 行, 超过内存上限 " + maxRows +
                ". 建议先加 LIMIT 节点或加 WHERE 过滤. (V5.32.x 支持临时表)");
        }
        nodeOutputs.put(nodeId, rows);
    }

    public List<Map<String, Object>> getOutput(String nodeId) {
        return nodeOutputs.getOrDefault(nodeId, List.of());
    }

    public List<Map<String, Object>> preview(List<Map<String, Object>> rows) {
        if (rows == null) return List.of();
        return rows.size() <= outputPreviewRows ? rows : rows.subList(0, outputPreviewRows);
    }
}
