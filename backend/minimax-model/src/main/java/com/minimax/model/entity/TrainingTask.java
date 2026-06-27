package com.minimax.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 训练任务实体 (Day 23) */
@Data
@TableName("training_task")
public class TrainingTask {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;
  private String modelName;
  private String corpusPath;

  // 超参数
  private Integer nLayer;
  private Integer nHead;
  private Integer nEmbd;
  private Integer blockSize;
  private Integer maxIters;
  private Integer batchSize;
  private Double learningRate;

  // 状态: PENDING / TRAINING / COMPLETED / FAILED
  private String status;
  private Integer progress;       // 0-100
  private Double currentLoss;
  private Integer currentIter;
  private String errorMessage;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;

  private LocalDateTime completedAt;

  /** 任务状态枚举 */
  public static final String STATUS_PENDING   = "PENDING";
  public static final String STATUS_TRAINING  = "TRAINING";
  public static final String STATUS_COMPLETED = "COMPLETED";
  public static final String STATUS_FAILED    = "FAILED";
}
