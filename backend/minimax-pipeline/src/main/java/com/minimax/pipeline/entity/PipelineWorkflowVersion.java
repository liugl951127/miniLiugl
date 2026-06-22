package com.minimax.pipeline.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流版本历史 (V5.32)
 *
 * 每次 PUT /workflows/{id} 自动 +1 version, 写快照
 */
@Data
@TableName("pipeline_workflow_version")
public class PipelineWorkflowVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long workflowId;
    private Integer version;
    private String definition;        // 快照
    private String changeLog;
    private Long createBy;
    private LocalDateTime createTime;
}
