package com.minimax.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.LocalDateTime;

/** 训练任务实体 (Day 23) */
@Data
@TableName("training_task")
public class TrainingTask {

  @TableId(type = IdType.AUTO)
  private Long id;

  @TableField("user_id")
  private Long userId;
  @TableField("model_name")
  private String modelName;
  @TableField("corpus_path")
  private String corpusPath;

  // 超参数
  @TableField("n_layer")
  private Integer nLayer;
  @TableField("n_head")
  private Integer nHead;
  @TableField("n_embd")
  private Integer nEmbd;
  @TableField("block_size")
  private Integer blockSize;
  @TableField("max_iters")
  private Integer maxIters;
  @TableField("batch_size")
  private Integer batchSize;
  @TableField("learning_rate")
  private Double learningRate;

  // 状态: PENDING / TRAINING / COMPLETED / FAILED
  @TableField("status")
  private String status;
  @TableField("progress")
  private Integer progress;       // 0-100
  @TableField("current_loss")
  private Double currentLoss;
  @TableField("current_iter")
  private Integer currentIter;
  @TableField("error_message")
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
