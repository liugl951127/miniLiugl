package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
    private String sessionId;
    /** 用户 ID */
    private Long userId;
    /** 客户端 IP */
    private String clientIp;
    /** 用户输入 (原始) */
    private String inputText;
    /** 输入模态 (TEXT/IMAGE/AUDIO/VIDEO/FILE) */
    private String inputModality;
    /** 意图 (GENERATE_CHART/...) */
    private String intent;
    /** 模型输出 (格式化后) */
    private String outputText;
    /** 生成 token 数 */
    private Integer outputTokens;
    /** 设备 (CPU/GPU) */
    private String computeDevice;
    /** 计算模式 (CPU/GPU/AUTO) */
    private String computeMode;
    /** 总耗时 ms */
    private Long totalCostMs;
    /** 各阶段耗时 (JSON: {stage1: 5, stage2: 10, ...}) */
    private String stageCosts;
    /** 风控等级 (SAFE/LOW/MEDIUM/HIGH/BLOCKED) */
    private String riskLevel;
    /** 后置风控是否需要审查 */
    private Boolean needsReview;
    /** RAG 命中数 */
    private Integer ragHits;
    /** 工具调用数 */
    private Integer toolCalls;
    /** 错误信息 (如有) */
    private String errorMessage;
    private LocalDateTime createdAt;
}
