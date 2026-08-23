package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField("task_id")
    private String taskId;
    /** checkpoint ID (UUID) */
    @TableField("checkpoint_id")
    private String checkpointId;
    /** 名称 (用户可命名, e.g. "best-val-loss") */
    @TableField("name")
    private String name;
    /** epoch */
    @TableField("epoch")
    private Integer epoch;
    /** step */
    @TableField("step")
    private Integer step;
    /** 文件路径 (相对 ${MINIMAX_MODEL_DIR}/checkpoints/) */
    @TableField("file_path")
    private String filePath;
    /** 文件大小 (字节) */
    @TableField("size_bytes")
    private Long sizeBytes;
    /** SHA256 校验 */
    @TableField("sha256")
    private String sha256;
    /** val_loss (用于 best 排序) */
    @TableField("val_loss")
    private Double valLoss;
    /** accuracy (用于 best 排序) */
    @TableField("accuracy")
    private Double accuracy;
    /** 标签 (e.g. "best", "latest", "milestone") */
    @TableField("tags")
    private String tags;
    /** 元数据 JSON */
    @TableField("metadata")
    private String metadata;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
