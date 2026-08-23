package com.minimax.deployer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 智能体模板实体 (V2.0)
 *
 * 预置的行业智能体模板, 用户选择后一键应用。
 * 模板包含: 角色描述、工具集、提示词模板、推荐模型。
 *
 * 表: agent_template
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("agent_template")
public class AgentTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板 code (如: edu-customer-service) */
    private String code;

    /** 模板名 */
    private String name;

    /** 行业分类 */
    private String industry;

    /** 描述 */
    private String description;

    /** Emoji */
    private String emoji;

    /** 主题色 (CSS gradient) */
    private String color;

    /** 智能体列表 JSON */
    private String agents;

    /** 工作流 JSON (节点 + 连线) */
    private String workflow;

    /** 工具列表 */
    private String tools;

    /** 推荐模型 */
    
    @TableField("recommended_model")
    private String recommendedModel;

    /** 使用次数 (热度排序) */
    
    @TableField("usage_count")
    private Integer usageCount;

    /** 状态: DRAFT / PUBLISHED / DEPRECATED */
    private String status;

    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
