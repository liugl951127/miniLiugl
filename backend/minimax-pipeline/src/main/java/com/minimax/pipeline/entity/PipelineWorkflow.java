package com.minimax.pipeline.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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

    @TableField("name")
    private String name;
    @TableField("description")
    private String description;
    @TableField("definition")
    private String definition;        // JSON: nodes + edges + viewport
    @TableField("version")
    private Integer version;
    @TableField("status")
    private Integer status;           // 1=启用 0=禁用

    @TableField("create_by")
    private Long createBy;
    @TableField("update_by")
    private Long updateBy;

    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
