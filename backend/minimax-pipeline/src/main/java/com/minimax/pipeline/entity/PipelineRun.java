package com.minimax.pipeline.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流执行历史 (V5.32)
 */
@Data
@TableName("pipeline_run")
public class PipelineRun {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long workflowId;
    private String workflowName;       // 冗余
    private String status;             // PENDING/RUNNING/SUCCESS/FAILED
    private Long triggerBy;
    private String triggerType;        // MANUAL/CRON/API
    private String definitionSnapshot; // JSON 快照 (避免画布改了之后历史结果对不上)
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private String errorMessage;
    private String resultSummary;      // JSON: { outputs: [{ nodeId, rows, path }] }
    private LocalDateTime createTime;
}
