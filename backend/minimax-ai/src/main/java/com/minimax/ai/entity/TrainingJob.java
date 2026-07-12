package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 训练任务实体 (V3.2.0 持久化)
 *
 * <p>每个训练任务持久化一行, 历史 metric 单独表 TrainingMetric (1:N)
 */
@Data
@TableName("training_job")
public class TrainingJob {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务 taskId (UUID) */
    private String taskId;
    /** 任务名 */
    private String name;
    /** 模型名 (e.g. "transformer-base") */
    private String model;
    /** 状态: PENDING / RUNNING / COMPLETED / FAILED / CANCELLED */
    private String status;
    /** 总 epoch 数 */
    private Integer totalEpochs;
    /** 当前 epoch */
    private Integer currentEpoch;
    /** 当前 step */
    private Integer currentStep;
    /** 起始时间戳 (毫秒) */
    private Long startTimeMs;
    /** 结束时间戳 (毫秒, 0=未结束) */
    private Long endTimeMs;
    /** 配置文件 JSON */
    private String config;
    /** 错误信息 */
    private String error;
    /** 创建人 */
    private Long ownerId;
    /** 标签 */
    private String tags;
    /** 最新 loss */
    private Double lastLoss;
    /** 最新 val_loss */
    private Double lastValLoss;
    /** 最新 accuracy */
    private Double lastAccuracy;
    /** 总步数 (终态时填) */
    private Integer totalSteps;
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
