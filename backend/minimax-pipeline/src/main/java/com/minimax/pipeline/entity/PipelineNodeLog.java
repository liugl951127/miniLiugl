package com.minimax.pipeline.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 节点执行日志 (V5.32)
 */
@Data
@TableName("pipeline_node_log")
public class PipelineNodeLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("run_id")
    private Long runId;
    @TableField("node_id")
    private String nodeId;             // 画布中的节点 id
    @TableField("node_type")
    private String nodeType;
    @TableField("node_name")
    private String nodeName;
    @TableField("status")
    private String status;             // PENDING/RUNNING/SUCCESS/FAILED/SKIPPED
    @TableField("start_time")
    private LocalDateTime startTime;
    @TableField("end_time")
    private LocalDateTime endTime;
    @TableField("duration_ms")
    private Long durationMs;
    @TableField("input_rows")
    private Integer inputRows;
    @TableField("output_rows")
    private Integer outputRows;
    @TableField("output_preview")
    private String outputPreview;      // 前 100 行 JSON
    @TableField("error_message")
    private String errorMessage;
    @TableField("config_snapshot")
    private String configSnapshot;     // 节点 config JSON
}
