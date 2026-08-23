package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField("group_id")
    private String groupId;
    /** 群组名 */
    @TableField("name")
    private String name;
    /** 描述 */
    @TableField("description")
    private String description;
    /** 协作策略 (PIPELINE / DEBATE / VOTE / SWARM) */
    @TableField("strategy")
    private String strategy;
    /** 群成员 JSON: [{agentName, role, weight, capability, order}] */
    @TableField("members_json")
    private String membersJson;
    /** 状态 (CREATED / RUNNING / COMPLETED / FAILED) */
    @TableField("status")
    private String status;
    /** 创建人 */
    @TableField("owner_id")
    private Long ownerId;
    /** 标签 (逗号分隔) */
    @TableField("tags")
    private String tags;
    /** 最后运行时间 */
    @TableField("last_run_at")
    private LocalDateTime lastRunAt;
    /** 总运行次数 */
    @TableField("run_count")
    private Integer runCount;
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
