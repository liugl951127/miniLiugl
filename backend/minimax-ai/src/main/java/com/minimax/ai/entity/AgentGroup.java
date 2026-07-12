package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体群组实体 (V3.0.3)
 *
 * <p>持久化群组定义 + 运行历史
 */
@Data
@TableName("agent_group")
public class AgentGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 群组 ID (UUID, 业务唯一) */
    private String groupId;
    /** 群组名 */
    private String name;
    /** 描述 */
    private String description;
    /** 协作策略 (PIPELINE / DEBATE / VOTE / SWARM) */
    private String strategy;
    /** 群成员 JSON: [{agentName, role, weight, capability, order}] */
    private String membersJson;
    /** 状态 (CREATED / RUNNING / COMPLETED / FAILED) */
    private String status;
    /** 创建人 */
    private Long ownerId;
    /** 标签 (逗号分隔) */
    private String tags;
    /** 最后运行时间 */
    private LocalDateTime lastRunAt;
    /** 总运行次数 */
    private Integer runCount;
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
