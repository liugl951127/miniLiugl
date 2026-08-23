package com.minimax.deployer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Forge Release (V4.0)
 *
 * V4.0 清理: 删 agent_definitions / deploy_config / manifests (改用 forge_agent + forge_manifest 子表)
 * 状态由 ReleaseStateMachine 集中管理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("forge_release")
public class ForgeRelease {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("version")
    private String version;

    @TableField("title")
    private String title;

    @TableField("changelog")
    private String changelog;

    @TableField("status")
    private String status;

    @TableField("deploy_target")
    private String deployTarget;

    @TableField("replicas")
    private Integer replicas;

    @TableField("image_registry")
    private String imageRegistry;

    @TableField("image_tag")
    private String imageTag;

    @TableField("deploy_duration")
    private Integer deployDuration;

    @TableField("failure_reason")
    private String failureReason;

    @TableField("created_by")
    private Long createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deployed_at")
    private LocalDateTime deployedAt;
}
