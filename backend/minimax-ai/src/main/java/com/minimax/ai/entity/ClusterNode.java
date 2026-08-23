package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 集群节点 (V3.3.0)
 *
 * <p>每个 AI 节点启动时注册一行, 心跳续约, 离线时标记 INACTIVE
 */
@Data
@TableName("cluster_node")
public class ClusterNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 节点 ID (UUID) */
    @TableField("node_id")
    private String nodeId;
    /** 节点名 */
    @TableField("name")
    private String name;
    /** host:port */
    @TableField("address")
    private String address;
    /** 区域 (e.g. cn-hangzhou, us-west) */
    @TableField("region")
    private String region;
    /** 可用区 */
    @TableField("zone")
    private String zone;
    /** 节点能力 (逗号分隔: gpu,fp16,llm,vlm) */
    @TableField("capabilities")
    private String capabilities;
    /** 总 CPU 核数 */
    @TableField("total_cores")
    private Integer totalCores;
    /** 总内存 MB */
    @TableField("total_memory_mb")
    private Long totalMemoryMb;
    /** 总 GPU 数 */
    @TableField("total_gpus")
    private Integer totalGpus;
    /** 当前 CPU 占用 0-1 */
    @TableField("cpu_usage")
    private Double cpuUsage;
    /** 当前内存占用 0-1 */
    @TableField("memory_usage")
    private Double memoryUsage;
    /** 当前 GPU 占用 0-1 */
    @TableField("gpu_usage")
    private Double gpuUsage;
    /** 当前负载任务数 */
    @TableField("active_tasks")
    private Integer activeTasks;
    /** 状态: ACTIVE / INACTIVE / DRAINING / OFFLINE */
    @TableField("status")
    private String status;
    /** 是否 leader */
    @TableField("is_leader")
    private Boolean isLeader;
    /** 最后心跳时间 */
    @TableField("last_heartbeat")
    private LocalDateTime lastHeartbeat;
    /** 启动时间 */
    @TableField("started_at")
    private LocalDateTime startedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
