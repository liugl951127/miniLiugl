package com.minimax.pipeline.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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

    @TableField("workflow_id")
    private Long workflowId;
    @TableField("workflow_name")
    private String workflowName;       // 冗余
    @TableField("status")
    private String status;             // PENDING/RUNNING/SUCCESS/FAILED
    @TableField("trigger_by")
    private Long triggerBy;
    @TableField("trigger_type")
    private String triggerType;        // MANUAL/CRON/API
    @TableField("definition_snapshot")
    private String definitionSnapshot; // JSON 快照 (避免画布改了之后历史结果对不上)
    @TableField("start_time")
    private LocalDateTime startTime;
    @TableField("end_time")
    private LocalDateTime endTime;
    @TableField("duration_ms")
    private Long durationMs;
    @TableField("error_message")
    private String errorMessage;
    @TableField("result_summary")
    private String resultSummary;      // JSON: { outputs: [{ nodeId, rows, path }] }
    @TableField("create_time")
    private LocalDateTime createTime;
}
