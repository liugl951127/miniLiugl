package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
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
    private String nodeId;
    /** 节点名 */
    private String name;
    /** host:port */
    private String address;
    /** 区域 (e.g. cn-hangzhou, us-west) */
    private String region;
    /** 可用区 */
    private String zone;
    /** 节点能力 (逗号分隔: gpu,fp16,llm,vlm) */
    private String capabilities;
    /** 总 CPU 核数 */
    private Integer totalCores;
    /** 总内存 MB */
    private Long totalMemoryMb;
    /** 总 GPU 数 */
    private Integer totalGpus;
    /** 当前 CPU 占用 0-1 */
    private Double cpuUsage;
    /** 当前内存占用 0-1 */
    private Double memoryUsage;
    /** 当前 GPU 占用 0-1 */
    private Double gpuUsage;
    /** 当前负载任务数 */
    private Integer activeTasks;
    /** 状态: ACTIVE / INACTIVE / DRAINING / OFFLINE */
    private String status;
    /** 是否 leader */
    private Boolean isLeader;
    /** 最后心跳时间 */
    private LocalDateTime lastHeartbeat;
    /** 启动时间 */
    private LocalDateTime startedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
