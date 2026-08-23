package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 训练指标历史 (V3.2.0, V8.0.3 与 minimax-model 对齐)
 *
 * <p>每次 record() 插一行, 用于回看训练曲线
 * <p>按 (taskId, iter) 索引, 查询高效
 *
 * <p>V8.0.3 修复: 与 minimax-model/TrainingMetric.java 字段对齐 (DDL 一致性)
 */
@Data
@TableName("training_metric")
public class TrainingMetric {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务 taskId (DDL: BIGINT) */
    @TableField("task_id")
    private Long taskId;
    /** 当前 iter */
    @TableField("iter")
    private Integer iter;
    /** 训练 loss */
    @TableField("loss")
    private Double loss;
    /** 准确率 (0-1) */
    @TableField("accuracy")
    private Double accuracy;
    /** 训练进度 0-100 */
    @TableField("progress")
    private Integer progress;
    /** 学习率 (字符串以支持科学计数法) */
    @TableField("lr")
    private String lr;
    /** GPU 利用率 0-100 */
    @TableField("gpu_util")
    private Integer gpuUtil;
    /** 显存占用 GB */
    @TableField("vram_gb")
    private Double vramGb;
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
