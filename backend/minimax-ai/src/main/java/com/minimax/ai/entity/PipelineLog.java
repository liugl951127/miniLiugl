package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 流水线执行日志实体 (V2.8.5)
 *
 * <h3>记录 13 个阶段的全链路信息</h3>
 * 用于: 调试, 性能分析, 用户回溯, 计费
 */
@Data
@TableName("pipeline_log")
public class PipelineLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 会话 ID */
    @TableField("session_id")
    private String sessionId;
    /** 用户 ID */
    @TableField("user_id")
    private Long userId;
    /** 客户端 IP */
    @TableField("client_ip")
    private String clientIp;
    /** 用户输入 (原始) */
    @TableField("input_text")
    private String inputText;
    /** 输入模态 (TEXT/IMAGE/AUDIO/VIDEO/FILE) */
    @TableField("input_modality")
    private String inputModality;
    /** 意图 (GENERATE_CHART/...) */
    @TableField("intent")
    private String intent;
    /** 模型输出 (格式化后) */
    @TableField("output_text")
    private String outputText;
    /** 生成 token 数 */
    @TableField("output_tokens")
    private Integer outputTokens;
    /** 设备 (CPU/GPU) */
    @TableField("compute_device")
    private String computeDevice;
    /** 计算模式 (CPU/GPU/AUTO) */
    @TableField("compute_mode")
    private String computeMode;
    /** 总耗时 ms */
    @TableField("total_cost_ms")
    private Long totalCostMs;
    /** 各阶段耗时 (JSON: {stage1: 5, stage2: 10, ...}) */
    @TableField("stage_costs")
    private String stageCosts;
    /** 风控等级 (SAFE/LOW/MEDIUM/HIGH/BLOCKED) */
    @TableField("risk_level")
    private String riskLevel;
    /** 后置风控是否需要审查 */
    @TableField("needs_review")
    private Boolean needsReview;
    /** RAG 命中数 */
    @TableField("rag_hits")
    private Integer ragHits;
    /** 工具调用数 */
    @TableField("tool_calls")
    private Integer toolCalls;
    /** 错误信息 (如有) */
    @TableField("error_message")
    private String errorMessage;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
