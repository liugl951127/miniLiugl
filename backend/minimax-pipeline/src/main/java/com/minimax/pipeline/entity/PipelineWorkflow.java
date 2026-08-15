package com.minimax.pipeline.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流定义 (V5.32)
 *
 * 画布 JSON: { nodes: [...], edges: [...], viewport: {...} }
 */
@Data
@TableName("pipeline_workflow")
public class PipelineWorkflow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private String definition;        // JSON: nodes + edges + viewport
    private Integer version;
    private Integer status;           // 1=启用 0=禁用

    private Long createBy;
    private Long updateBy;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
