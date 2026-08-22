package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自研训练模型 (T1-backend-apis / P0)
 *
 * 与 minimax-model 模块的 model_config 不同:
 *   - model_config: 第三方/外部模型配置 (OpenAI, DeepSeek, ...)
 *   - trained_model: 平台自研训练产出的模型, 由前端 "模型训练" 页面管理
 *
 * 状态: ENABLED (启用) / DISABLED (禁用) / DRAFT (草稿, 未发布)
 *
 * @since V7.2
 */
@Data
@TableName("trained_model")
public class TrainedModel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模型唯一编码 (前端引用, e.g. mmx-mini-v1) */
    private String code;

    /** 显示名称 */
    private String name;

    /** 准确率 0-1 (e.g. 0.872 = 87.2%) */
    private BigDecimal accuracy;

    /** ENABLED / DISABLED / DRAFT */
    private String status;

    /** 发布时间 (只有发布过的模型才有) */
    private LocalDateTime publishedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
