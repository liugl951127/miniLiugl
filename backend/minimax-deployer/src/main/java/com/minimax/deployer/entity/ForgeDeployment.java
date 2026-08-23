package com.minimax.deployer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Agent Forge 部署实例实体 (V2.0)
 *
 * 一个 release 可以有多个部署实例 (蓝绿/灰度/多集群)。
 * 每个实例记录完整的部署阶段日志 + 状态。
 *
 * 表: forge_deployment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("forge_deployment")
public class ForgeDeployment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属 release */
    private Long releaseId;

    /** 部署实例名 (用户可指定, 如: prod-blue, staging) */
    private String instanceName;

    /** 部署阶段 JSON 数组, 如:
     *  [
     *    {"name":"代码校验","status":"done","duration":1},
     *    {"name":"构建镜像","status":"running","duration":0}
     *  ]
     */
    private String stages;

    /** 实时日志 (TEXT 字段, 累积) */
    private String logs;

    /** 状态: PENDING / BUILDING / PUSHING / DEPLOYING / RUNNING / FAILED / ROLLED_BACK */
    private String status;

    /** 集群/目标标识 */
    private String target;

    /** 命名空间 (K8s) / 容器名 (Docker) */
    private String namespace;

    /** 实际副本数 (running) */
    private Integer runningReplicas;

    /** 期望副本数 (desired) */
    private Integer desiredReplicas;

    /** QPS 当前值 (用于实时监控) */
    private Double currentQps;

    /** 错误信息 (失败时) */
    private String errorMessage;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
