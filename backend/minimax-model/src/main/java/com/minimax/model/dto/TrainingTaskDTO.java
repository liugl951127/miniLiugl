package com.minimax.model.dto;

import lombok.Data;

/** 创建训练任务请求 DTO (Day 23) */
@Data
public class TrainingTaskDTO {
  private Long userId;
  private String modelName;
  private String corpusPath;
  private Integer nLayer = 12;
  private Integer nHead = 12;
  private Integer nEmbd = 768;
  private Integer blockSize = 128;
  private Integer maxIters = 100;
  private Integer batchSize = 32;
  private Double learningRate = 0.0003;
}
