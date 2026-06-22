package com.minimax.pipeline.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.common.exception.BizException;
import com.minimax.pipeline.dto.WorkflowDto;
import com.minimax.pipeline.entity.PipelineNodeLog;
import com.minimax.pipeline.entity.PipelineRun;
import com.minimax.pipeline.entity.PipelineWorkflow;
import com.minimax.pipeline.enums.NodeStatus;
import com.minimax.pipeline.enums.RunStatus;
import com.minimax.pipeline.executor.ExecutionContext;
import com.minimax.pipeline.executor.NodeExecutor;
import com.minimax.pipeline.executor.NodeExecutorFactory;
import com.minimax.pipeline.mapper.PipelineNodeLogMapper;
import com.minimax.pipeline.mapper.PipelineRunMapper;
import com.minimax.pipeline.mapper.PipelineWorkflowMapper;
import com.minimax.pipeline.vo.DagValidationVo;
import com.minimax.pipeline.vo.RunDetailVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 工作流执行引擎 (V5.32)
 *
 * 流程:
 *   1. triggerRun() → 同步创建 Run, 异步 executeRun()
 *   2. executeRun() 按拓扑层执行, 每节点:
 *      - 收集上游输入 (从 ctx.getOutput)
 *      - factory.get(type).execute()
 *      - 写 PipelineNodeLog
 *   3. 节点失败 → 整 run 标 FAILED, 下游 SKIPPED
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineEngine {

    private final PipelineWorkflowMapper workflowMapper;
    private final PipelineRunMapper runMapper;
    private final PipelineNodeLogMapper nodeLogMapper;
    private final NodeExecutorFactory executorFactory;
    private final ObjectMapper json = new ObjectMapper();

    @Value("${pipeline.max-rows-in-memory:100000}")
    private int maxRows;

    @Value("${pipeline.output-preview-rows:100}")
    private int outputPreviewRows;

    /**
     * 同步触发 (实际异步执行)
     * @return runId
     */
    public Long triggerRun(Long workflowId, Long userId) {
        PipelineWorkflow wf = workflowMapper.selectById(workflowId);
        if (wf == null) throw new BizException("工作流不存在: " + workflowId);
        WorkflowDto dto = parseDto(wf.getDefinition());
        DagValidator.quickValidate(dto);

        PipelineRun run = new PipelineRun();
        run.setWorkflowId(workflowId);
        run.setWorkflowName(wf.getName());
        run.setStatus(RunStatus.PENDING.name());
        run.setTriggerBy(userId);
        run.setTriggerType("MANUAL");
        run.setDefinitionSnapshot(wf.getDefinition());
        run.setCreateTime(LocalDateTime.now());
        runMapper.insert(run);
        log.info("Pipeline run {} created for workflow {} (userId={})", run.getId(), workflowId, userId);

        asyncExecute(run.getId(), dto, userId);
        return run.getId();
    }

    @Async
    public void asyncExecute(Long runId, WorkflowDto dto, Long userId) {
        executeRun(runId, dto, userId);
    }

    void executeRun(Long runId, WorkflowDto dto, Long userId) {
        PipelineRun run = runMapper.selectById(runId);
        if (run == null) {
            log.error("run {} 不存在, 跳过执行", runId);
            return;
        }
        run.setStatus(RunStatus.RUNNING.name());
        run.setStartTime(LocalDateTime.now());
        runMapper.updateById(run);

        ExecutionContext ctx = new ExecutionContext(runId,
                dto == null ? 0L : 0L, userId, maxRows, outputPreviewRows);
        DagValidationVo vo = DagValidator.validate(dto);

        // 预构建: nodeId → inbound edges
        Map<String, List<WorkflowDto.WorkflowEdge>> inbound = new HashMap<>();
        for (WorkflowDto.WorkflowEdge e : dto.getEdges()) {
            inbound.computeIfAbsent(e.getTo(), k -> new ArrayList<>()).add(e);
        }

        // 预创建节点日志
        Map<String, PipelineNodeLog> logs = new HashMap<>();
        for (WorkflowDto.WorkflowNode n : dto.getNodes()) {
            PipelineNodeLog log = new PipelineNodeLog();
            log.setRunId(runId);
            log.setNodeId(n.getId());
            log.setNodeType(n.getType().name());
            log.setNodeName(n.getName());
            log.setStatus(NodeStatus.PENDING.name());
            log.setConfigSnapshot(safeJson(n.getConfig()));
            nodeLogMapper.insert(log);
            logs.put(n.getId(), log);
        }

        boolean failed = false;
        String errorMsg = null;
        try {
            for (List<String> level : vo.getExecutionOrder()) {
                for (String nodeId : level) {
                    if (failed) {
                        skipNode(logs.get(nodeId));
                        continue;
                    }
                    PipelineNodeLog nodeLog = logs.get(nodeId);
                    WorkflowDto.WorkflowNode node = findNode(dto, nodeId);
                    try {
                        executeSingleNode(node, inbound, ctx, nodeLog);
                    } catch (Exception e) {
                        log.error("节点 {} 执行失败: {}", nodeId, e.getMessage());
                        failed = true;
                        errorMsg = e.getMessage();
                        markFailed(nodeLog, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Run {} 整体异常: {}", runId, e.getMessage(), e);
            failed = true;
            errorMsg = e.getMessage();
        }

        run.setEndTime(LocalDateTime.now());
        run.setDurationMs(Duration.between(run.getStartTime(), run.getEndTime()).toMillis());
        run.setStatus(failed ? RunStatus.FAILED.name() : RunStatus.SUCCESS.name());
        if (errorMsg != null) run.setErrorMessage(errorMsg);
        run.setResultSummary(buildResultSummary(dto, ctx));
        runMapper.updateById(run);
        log.info("Run {} 结束, status={}, duration={}ms", runId, run.getStatus(), run.getDurationMs());
    }

    private void executeSingleNode(WorkflowDto.WorkflowNode node,
                                    Map<String, List<WorkflowDto.WorkflowEdge>> inbound,
                                    ExecutionContext ctx, PipelineNodeLog nodeLog) {
        long t0 = System.currentTimeMillis();
        nodeLog.setStartTime(LocalDateTime.now());
        nodeLog.setStatus(NodeStatus.RUNNING.name());
        nodeLogMapper.updateById(nodeLog);

        Map<String, List<Map<String, Object>>> inputs = new HashMap<>();
        for (WorkflowDto.WorkflowEdge e : inbound.getOrDefault(node.getId(), List.of())) {
            inputs.put(e.getFrom(), ctx.getOutput(e.getFrom()));
        }
        nodeLog.setInputRows(inputs.values().stream().mapToInt(List::size).sum());

        NodeExecutor exec = executorFactory.get(node.getType());
        List<Map<String, Object>> out = exec.execute(node.getId(), node.getConfig(), inputs, ctx);

        nodeLog.setOutputRows(out.size());
        try { nodeLog.setOutputPreview(json.writeValueAsString(ctx.preview(out))); } catch (Exception ignored) {}
        nodeLog.setStatus(NodeStatus.SUCCESS.name());
        nodeLog.setEndTime(LocalDateTime.now());
        nodeLog.setDurationMs(System.currentTimeMillis() - t0);
        nodeLogMapper.updateById(nodeLog);
        log.debug("节点 {} ({}) 完成: {} 行", node.getId(), node.getType(), out.size());
    }

    private void skipNode(PipelineNodeLog log) {
        log.setStatus(NodeStatus.SKIPPED.name());
        log.setErrorMessage("上游节点失败, 跳过");
        log.setEndTime(LocalDateTime.now());
        nodeLogMapper.updateById(log);
    }

    private void markFailed(PipelineNodeLog log, Exception e) {
        log.setStatus(NodeStatus.FAILED.name());
        log.setErrorMessage(e.getMessage());
        log.setEndTime(LocalDateTime.now());
        nodeLogMapper.updateById(log);
    }

    private WorkflowDto.WorkflowNode findNode(WorkflowDto dto, String nodeId) {
        for (WorkflowDto.WorkflowNode n : dto.getNodes()) {
            if (n.getId().equals(nodeId)) return n;
        }
        throw new BizException("节点不存在: " + nodeId);
    }

    private String buildResultSummary(WorkflowDto dto, ExecutionContext ctx) {
        Map<String, Object> summary = new LinkedHashMap<>();
        List<Map<String, Object>> outputs = new ArrayList<>();
        for (WorkflowDto.WorkflowNode n : dto.getNodes()) {
            if (n.getType().isOutput()) {
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("nodeId", n.getId());
                out.put("type", n.getType().name());
                out.put("rows", ctx.getOutput(n.getId()).size());
                outputs.add(out);
            }
        }
        summary.put("outputs", outputs);
        return safeJson(summary);
    }

    private String safeJson(Object o) {
        try { return json.writeValueAsString(o); } catch (Exception e) { return null; }
    }

    private WorkflowDto parseDto(String jsonStr) {
        try {
            return json.readValue(jsonStr, WorkflowDto.class);
        } catch (Exception e) {
            throw new BizException("工作流定义 JSON 解析失败: " + e.getMessage());
        }
    }
}
