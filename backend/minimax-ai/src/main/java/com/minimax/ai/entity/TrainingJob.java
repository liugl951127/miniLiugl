package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField("task_id")
    private String taskId;
    /** 任务名 */
    @TableField("name")
    private String name;
    /** 模型名 (e.g. "transformer-base") */
    @TableField("model")
    private String model;
    /** 状态: PENDING / RUNNING / COMPLETED / FAILED / CANCELLED */
    @TableField("status")
    private String status;
    /** 总 epoch 数 */
    @TableField("total_epochs")
    private Integer totalEpochs;
    /** 当前 epoch */
    @TableField("current_epoch")
    private Integer currentEpoch;
    /** 当前 step */
    @TableField("current_step")
    private Integer currentStep;
    /** 起始时间戳 (毫秒) */
    @TableField("start_time_ms")
    private Long startTimeMs;
    /** 结束时间戳 (毫秒, 0=未结束) */
    @TableField("end_time_ms")
    private Long endTimeMs;
    /** 配置文件 JSON */
    @TableField("config")
    private String config;
    /** 错误信息 */
    @TableField("error")
    private String error;
    /** 创建人 */
    @TableField("owner_id")
    private Long ownerId;
    /** 标签 */
    @TableField("tags")
    private String tags;
    /** 最新 loss */
    @TableField("last_loss")
    private Double lastLoss;
    /** 最新 val_loss */
    @TableField("last_val_loss")
    private Double lastValLoss;
    /** 最新 accuracy */
    @TableField("last_accuracy")
    private Double lastAccuracy;
    /** 总步数 (终态时填) */
    @TableField("total_steps")
    private Integer totalSteps;
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
