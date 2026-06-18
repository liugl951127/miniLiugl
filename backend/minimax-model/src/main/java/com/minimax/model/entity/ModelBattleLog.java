package com.minimax.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型对决日志 (V4).
 * 对应表: model_battle_log
 *
 * @see com.minimax.model.controller.RealAiTestController
 */
@Data
@TableName("model_battle_log")
public class ModelBattleLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("battle_id")
    private String battleId;

    @TableField("user_id")
    private Long userId;

    @TableField("model_id")
    private Long modelId;

    @TableField("model_code")
    private String modelCode;

    private String prompt;
    private String response;
    @TableField("prompt_tokens")
    private Integer promptTokens;
    @TableField("completion_tokens")
    private Integer completionTokens;
    @TableField("latency_ms")
    private Integer latencyMs;
    private String status;
    @TableField("error_msg")
    private String errorMsg;
    private Integer score;
    @TableField("judge_model")
    private String judgeModel;
    @TableField("judge_reason")
    private String judgeReason;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
