package com.minimax.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.LocalDateTime;

/** 训练过程指标历史 (每个 iter 存一条) */
@Data
@TableName("training_metric")
public class TrainingMetric {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("task_id")
    private Long taskId;
    @TableField("iter")
    private Integer iter;
    @TableField("loss")
    private Double loss;
    @TableField("accuracy")
    private Double accuracy;
    @TableField("progress")
    private Integer progress; // 0-100
    @TableField("lr")
    private String lr;
    @TableField("gpu_util")
    private Integer gpuUtil;
    @TableField("vram_gb")
    private Double vramGb;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
