package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 训练检查点 (V3.2.0)
 *
 * <p>训练过程中保存的模型快照, 用于:
 *   - 恢复训练 (断点续训)
 *   - 模型版本管理
 *   - 推理部署
 */
@Data
@TableName("training_checkpoint")
public class TrainingCheckpoint {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务 taskId */
    private String taskId;
    /** checkpoint ID (UUID) */
    private String checkpointId;
    /** 名称 (用户可命名, e.g. "best-val-loss") */
    private String name;
    /** epoch */
    private Integer epoch;
    /** step */
    private Integer step;
    /** 文件路径 (相对 ${MINIMAX_MODEL_DIR}/checkpoints/) */
    private String filePath;
    /** 文件大小 (字节) */
    private Long sizeBytes;
    /** SHA256 校验 */
    private String sha256;
    /** val_loss (用于 best 排序) */
    private Double valLoss;
    /** accuracy (用于 best 排序) */
    private Double accuracy;
    /** 标签 (e.g. "best", "latest", "milestone") */
    private String tags;
    /** 元数据 JSON */
    private String metadata;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
