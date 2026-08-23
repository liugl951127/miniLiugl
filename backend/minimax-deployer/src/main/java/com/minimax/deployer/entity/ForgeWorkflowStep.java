package com.minimax.deployer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Forge Workflow Step (V4.1)
 *
 * V4.1: 删 project_id 字段, 只绑 release_id (与 ForgeAgent 保持一致)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("forge_workflow_step")
public class ForgeWorkflowStep implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("release_id")
    private Long releaseId;

    @TableField("step_no")
    private Integer stepNo;

    @TableField("name")
    private String name;

    @TableField("type")
    private String type;

    @TableField("agent_id")
    private Long agentId;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
