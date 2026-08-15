package com.minimax.pipeline.service;

import com.minimax.common.exception.BizException;
import com.minimax.pipeline.dto.WorkflowDto;
import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.vo.DagValidationVo;

import java.util.*;

/**
 * DAG 校验器 (V5.32) - Kahn 拓扑排序
 */
public final class DagValidator {

    private DagValidator() {}

    public static DagValidationVo validate(WorkflowDto dto) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<List<String>> executionOrder = new ArrayList<>();

        if (dto.getNodes() == null || dto.getNodes().isEmpty()) {
            return DagValidationVo.builder()
                    .valid(false).errors(List.of("工作流至少需要一个节点"))
                    .warnings(warnings).executionOrder(executionOrder)
                    .build();
        }

        Map<String, WorkflowDto.WorkflowNode> nodeMap = new HashMap<>();
        for (WorkflowDto.WorkflowNode n : dto.getNodes()) {
            if (nodeMap.put(n.getId(), n) != null) {
                errors.add("节点 id 重复: " + n.getId());
                return DagValidationVo.builder()
                        .valid(false).errors(errors).warnings(warnings)
                        .executionOrder(executionOrder).build();
            }
        }

        // 1. 入度表 + 邻接表
        Map<String, Integer> inDeg = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();
        for (String id : nodeMap.keySet()) {
            inDeg.put(id, 0);
            adj.put(id, new ArrayList<>());
        }
        for (WorkflowDto.WorkflowEdge e : dto.getEdges()) {
            if (!nodeMap.containsKey(e.getFrom())) {
                errors.add("边 " + e.getId() + " 的 source 节点不存在: " + e.getFrom());
                continue;
            }
            if (!nodeMap.containsKey(e.getTo())) {
                errors.add("边 " + e.getId() + " 的 target 节点不存在: " + e.getTo());
                continue;
            }
            adj.get(e.getFrom()).add(e.getTo());
            inDeg.merge(e.getTo(), 1, Integer::sum);
        }

        // 2. INPUT 节点入度必须 0
        for (WorkflowDto.WorkflowNode n : dto.getNodes()) {
            if (n.getType().isInput() && inDeg.get(n.getId()) > 0) {
                errors.add("INPUT 节点 " + n.getId() + " 不应接收上游 (入度=" + inDeg.get(n.getId()) + ")");
            }
            if (n.getType().isOutput() && !adj.get(n.getId()).isEmpty()) {
                warnings.add("OUTPUT 节点 " + n.getId() + " 有 " + adj.get(n.getId()).size() + " 个下游, 数据将被忽略");
            }
        }

        // 3. 孤立节点
        for (WorkflowDto.WorkflowNode n : dto.getNodes()) {
            if (inDeg.get(n.getId()) == 0 && adj.get(n.getId()).isEmpty()) {
                warnings.add("孤立节点 " + n.getId() + " (" + n.getName() + ") 无连线, 不会执行");
            }
        }

        // 4. 拓扑排序
        Queue<String> q = new LinkedList<>();
        for (Map.Entry<String, Integer> e : inDeg.entrySet()) {
            if (e.getValue() == 0) q.offer(e.getKey());
        }

        int processed = 0;
        while (!q.isEmpty()) {
            List<String> level = new ArrayList<>();
            int sz = q.size();
            for (int i = 0; i < sz; i++) {
                String n = q.poll();
                level.add(n);
                processed++;
                for (String next : adj.get(n)) {
                    if (inDeg.merge(next, -1, Integer::sum) == 0) q.offer(next);
                }
            }
            executionOrder.add(level);
        }

        // 5. 环检测
        if (processed != nodeMap.size()) {
            errors.add("DAG 存在环依赖, 已处理 " + processed + "/" + nodeMap.size() + " 节点");
        }

        return DagValidationVo.builder()
                .valid(errors.isEmpty())
                .errors(errors).warnings(warnings)
                .executionOrder(executionOrder)
                .inDegree(new HashMap<>(inDeg))
                .build();
    }

    public static void quickValidate(WorkflowDto dto) {
        DagValidationVo vo = validate(dto);
        if (!vo.isValid()) {
            throw new BizException(String.join("; ", vo.getErrors()));
        }
    }
}
