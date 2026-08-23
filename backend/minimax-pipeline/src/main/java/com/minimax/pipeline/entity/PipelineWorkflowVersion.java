package com.minimax.pipeline.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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

    @TableField("workflow_id")
    private Long workflowId;
    @TableField("version")
    private Integer version;
    @TableField("definition")
    private String definition;        // 快照
    @TableField("change_log")
    private String changeLog;
    @TableField("create_by")
    private Long createBy;
    @TableField("create_time")
    private LocalDateTime createTime;
}
