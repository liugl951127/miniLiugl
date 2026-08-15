package com.minimax.pipeline.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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

    private Long runId;
    private String nodeId;             // 画布中的节点 id
    private String nodeType;
    private String nodeName;
    private String status;             // PENDING/RUNNING/SUCCESS/FAILED/SKIPPED
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private Integer inputRows;
    private Integer outputRows;
    private String outputPreview;      // 前 100 行 JSON
    private String errorMessage;
    private String configSnapshot;     // 节点 config JSON
}
