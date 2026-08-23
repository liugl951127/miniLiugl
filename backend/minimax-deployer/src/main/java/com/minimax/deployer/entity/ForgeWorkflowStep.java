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
 * Forge Workflow Step (V4.0) - 独立子表
 *
 * 替代 ForgeProject 中存的 workflow JSON 字符串
 * 流程步骤独立成行, 便于展示/编辑/排序
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("forge_workflow_step")
public class ForgeWorkflowStep implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("release_id")
    private Long releaseId;

    /** 步骤序号 (从 1 开始) */
    @TableField("step_no")
    private Integer stepNo;

    /** 步骤名称 */
    @TableField("name")
    private String name;

    /** 步骤类型: input/agent/decision/output */
    @TableField("type")
    private String type;

    /** 关联 agent id (可选) */
    @TableField("agent_id")
    private Long agentId;

    /** 备注 */
    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
