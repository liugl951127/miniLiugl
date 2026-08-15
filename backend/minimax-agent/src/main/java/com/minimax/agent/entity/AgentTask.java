package com.minimax.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 自主任务表 (V4.3 补 entity).
 * 对应表: agent_task (在 15_v2_features.sql)
 *
 * 用途: 把 ReAct 推理步骤持久化到 DB, 替代之前的纯内存存储.
 */
@Data
@TableName("agent_task")
public class AgentTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务 UUID, 对外暴露 */
    private String taskId;

    /** 用户 ID */
    private Long userId;

    /** 用户输入目标 */
    private String goal;

    /** 当前状态: pending/running/done/failed/cancelled */
    private String status;

    /** ReAct 轮数 */
    private Integer rounds;

    /** 最终结果 */
    private String result;

    /** LLM 调用次数 */
    private Integer llmCalls;

    /** 工具调用次数 */
    private Integer toolCalls;

    /** 总 token 用量 */
    private Integer totalTokens;

    /** 错误信息 */
    private String errorMsg;

    /** 耗时 ms */
    private Long latencyMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
