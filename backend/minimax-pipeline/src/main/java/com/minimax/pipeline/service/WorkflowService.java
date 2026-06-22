package com.minimax.pipeline.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.common.exception.BizException;
import com.minimax.common.result.Result;
import com.minimax.pipeline.dto.WorkflowDto;
import com.minimax.pipeline.entity.PipelineNodeLog;
import com.minimax.pipeline.entity.PipelineRun;
import com.minimax.pipeline.entity.PipelineWorkflow;
import com.minimax.pipeline.entity.PipelineWorkflowVersion;
import com.minimax.pipeline.mapper.PipelineNodeLogMapper;
import com.minimax.pipeline.mapper.PipelineRunMapper;
import com.minimax.pipeline.mapper.PipelineWorkflowMapper;
import com.minimax.pipeline.mapper.PipelineWorkflowVersionMapper;
import com.minimax.pipeline.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作流服务 (V5.32)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final PipelineWorkflowMapper workflowMapper;
    private final PipelineWorkflowVersionMapper versionMapper;
    private final PipelineRunMapper runMapper;
    private final PipelineNodeLogMapper nodeLogMapper;
    private final PipelineEngine engine;
    private final ObjectMapper json = new ObjectMapper();

    @Transactional
    public Long create(WorkflowDto dto, Long userId) {
        DagValidator.quickValidate(dto);
        PipelineWorkflow wf = new PipelineWorkflow();
        wf.setName(dto.getName());
        wf.setDescription(dto.getDescription());
        wf.setDefinition(toJson(dto));
        wf.setVersion(1);
        wf.setStatus(1);
        wf.setCreateBy(userId);
        wf.setUpdateBy(userId);
        LocalDateTime now = LocalDateTime.now();
        wf.setCreateTime(now);
        wf.setUpdateTime(now);
        workflowMapper.insert(wf);
        log.info("Workflow created: id={} name='{}'", wf.getId(), wf.getName());
        return wf.getId();
    }

    public WorkflowDetailVo getById(Long id) {
        PipelineWorkflow wf = workflowMapper.selectById(id);
        if (wf == null) throw new BizException("工作流不存在");
        WorkflowDetailVo vo = new WorkflowDetailVo();
        vo.setId(wf.getId());
        vo.setName(wf.getName());
        vo.setDescription(wf.getDescription());
        vo.setVersion(wf.getVersion());
        vo.setStatus(wf.getStatus());
        vo.setCreateBy(wf.getCreateBy());
        vo.setCreateTime(wf.getCreateTime());
        vo.setUpdateTime(wf.getUpdateTime());
        try {
            vo.setDefinition(json.readValue(wf.getDefinition(), WorkflowDto.class));
        } catch (Exception e) {
            throw new BizException("工作流定义解析失败: " + e.getMessage());
        }
        return vo;
    }

    public Page<WorkflowVo> list(int page, int size, String keyword) {
        LambdaQueryWrapper<PipelineWorkflow> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) q.like(PipelineWorkflow::getName, keyword);
        q.orderByDesc(PipelineWorkflow::getUpdateTime);
        Page<PipelineWorkflow> p = workflowMapper.selectPage(new Page<>(page, size), q);
        List<WorkflowVo> records = p.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        // V5.32 简化: 累计执行次数
        records.forEach(r -> r.setRunCount(runMapper.selectCount(
                new LambdaQueryWrapper<PipelineRun>().eq(PipelineRun::getWorkflowId, r.getId()))));
        return new Page<WorkflowVo>(p.getCurrent(), p.getSize(), p.getTotal()).setRecords(records);
    }

    @Transactional
    public void update(Long id, WorkflowDto dto, Long userId) {
        DagValidator.quickValidate(dto);
        PipelineWorkflow wf = workflowMapper.selectById(id);
        if (wf == null) throw new BizException("工作流不存在");
        // 写版本历史
        PipelineWorkflowVersion ver = new PipelineWorkflowVersion();
        ver.setWorkflowId(id);
        ver.setVersion(wf.getVersion());
        ver.setDefinition(wf.getDefinition());
        ver.setChangeLog("update " + LocalDateTime.now());
        ver.setCreateBy(userId);
        ver.setCreateTime(LocalDateTime.now());
        versionMapper.insert(ver);
        // 更新
        wf.setName(dto.getName());
        wf.setDescription(dto.getDescription());
        wf.setDefinition(toJson(dto));
        wf.setVersion(wf.getVersion() + 1);
        wf.setUpdateBy(userId);
        wf.setUpdateTime(LocalDateTime.now());
        workflowMapper.updateById(wf);
    }

    @Transactional
    public void softDelete(Long id, Long userId) {
        PipelineWorkflow wf = workflowMapper.selectById(id);
        if (wf == null) throw new BizException("工作流不存在");
        workflowMapper.deleteById(id);  // @TableLogic 自动软删
    }

    public Long triggerRun(Long id, Long userId) {
        return engine.triggerRun(id, userId);
    }

    public List<Map<String, Object>> listRuns(Long workflowId, int limit) {
        List<PipelineRun> runs = runMapper.selectList(new LambdaQueryWrapper<PipelineRun>()
                .eq(PipelineRun::getWorkflowId, workflowId)
                .orderByDesc(PipelineRun::getCreateTime)
                .last("LIMIT " + limit));
        return runs.stream().map(this::toRunMap).collect(Collectors.toList());
    }

    public RunDetailVo getRunDetail(Long runId) {
        PipelineRun run = runMapper.selectById(runId);
        if (run == null) throw new BizException("执行不存在: " + runId);
        List<PipelineNodeLog> logs = nodeLogMapper.selectList(
                new LambdaQueryWrapper<PipelineNodeLog>().eq(PipelineNodeLog::getRunId, runId)
                        .orderByAsc(PipelineNodeLog::getId));
        RunDetailVo.RunDetailVoBuilder b = RunDetailVo.builder()
                .id(run.getId())
                .workflowId(run.getWorkflowId())
                .workflowName(run.getWorkflowName())
                .status(run.getStatus())
                .triggerBy(run.getTriggerBy())
                .triggerType(run.getTriggerType())
                .startTime(run.getStartTime())
                .endTime(run.getEndTime())
                .durationMs(run.getDurationMs())
                .errorMessage(run.getErrorMessage())
                .nodeLogs(logs);
        if (run.getResultSummary() != null) {
            try { b.resultSummary(json.readValue(run.getResultSummary(), new TypeReference<Map<String, Object>>() {})); }
            catch (Exception ignored) {}
        }
        return b.build();
    }

    public List<Map<String, Object>> getRunResult(Long runId, String outputNodeId) {
        PipelineNodeLog nodeLog = nodeLogMapper.selectOne(new LambdaQueryWrapper<PipelineNodeLog>()
                .eq(PipelineNodeLog::getRunId, runId)
                .eq(PipelineNodeLog::getNodeId, outputNodeId));
        if (nodeLog == null) throw new BizException("节点日志不存在");
        if (nodeLog.getOutputPreview() == null) return List.of();
        try {
            return json.readValue(nodeLog.getOutputPreview(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("outputPreview 解析失败: {}", e.getMessage());
            return List.of();
        }
    }

    public DagValidationVo validate(WorkflowDto dto) {
        return DagValidator.validate(dto);
    }

    // ---- helpers ----

    private WorkflowVo toVo(PipelineWorkflow wf) {
        return WorkflowVo.builder()
                .id(wf.getId()).name(wf.getName()).description(wf.getDescription())
                .version(wf.getVersion()).status(wf.getStatus())
                .createBy(wf.getCreateBy())
                .createTime(wf.getCreateTime()).updateTime(wf.getUpdateTime())
                .build();
    }

    private Map<String, Object> toRunMap(PipelineRun r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("workflowId", r.getWorkflowId());
        m.put("workflowName", r.getWorkflowName());
        m.put("status", r.getStatus());
        m.put("triggerBy", r.getTriggerBy());
        m.put("durationMs", r.getDurationMs());
        m.put("startTime", r.getStartTime());
        m.put("endTime", r.getEndTime());
        m.put("errorMessage", r.getErrorMessage());
        return m;
    }

    private String toJson(Object o) {
        try { return json.writeValueAsString(o); } catch (Exception e) { return "{}"; }
    }
}
