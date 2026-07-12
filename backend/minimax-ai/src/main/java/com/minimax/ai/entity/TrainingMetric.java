package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 训练指标历史 (V3.2.0)
 *
 * <p>每次 record() 插一行, 用于回看训练曲线
 * <p>按 (taskId, step) 索引, 查询高效
 */
@Data
@TableName("training_metric")
public class TrainingMetric {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务 taskId */
    private String taskId;
    /** 当前 epoch */
    private Integer epoch;
    /** 当前 step */
    private Integer step;
    /** 训练 loss */
    private Double loss;
    /** 验证 loss */
    private Double valLoss;
    /** 准确率 (0-1) */
    private Double accuracy;
    /** 学习率 */
    private Double learningRate;
    /** 累计耗时 ms */
    private Long elapsedMs;
    /** 时间戳 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime timestamp;
}
