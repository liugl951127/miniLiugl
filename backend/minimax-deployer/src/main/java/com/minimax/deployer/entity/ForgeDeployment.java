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
 * Forge Deployment (V4.0)
 *
 * V4.0 清理:
 *  - 删 stages (JSON 字符串) — 由 forge_deployment_log 子表承担
 *  - 删 logs (TEXT) — 同上
 *  - 加 current_stage (单字段, 表示当前在哪一阶段)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("forge_deployment")
public class ForgeDeployment {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("release_id")
    private Long releaseId;

    @TableField("instance_name")
    private String instanceName;

    @TableField("current_stage")
    private String currentStage;

    @TableField("status")
    private String status;

    @TableField("target")
    private String target;

    @TableField("namespace")
    private String namespace;

    @TableField("running_replicas")
    private Integer runningReplicas;

    @TableField("desired_replicas")
    private Integer desiredReplicas;

    @TableField("current_qps")
    private Double currentQps;

    @TableField("error_message")
    private String errorMessage;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
