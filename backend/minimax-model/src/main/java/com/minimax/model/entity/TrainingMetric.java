package com.minimax.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 训练过程指标历史 (每个 iter 存一条) */
@Data
@TableName("training_metric")
public class TrainingMetric {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Integer iter;
    private Double loss;
    private Double accuracy;
    private Integer progress; // 0-100
    private String lr;
    private Integer gpuUtil;
    private Double vramGb;
    private LocalDateTime createdAt;
}
