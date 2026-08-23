package com.minimax.deployer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Forge Agent (V4.0) - 独立子表
 *
 * 替代 ForgeRelease.agentDefinitions (JSON 字符串) 和 ForgeProject.recommendedAgents
 * 每个智能体是独立行, 可索引/可查询
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("forge_agent")
public class ForgeAgent implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属 release id */
    @TableField("release_id")
    private Long releaseId;

    /** 所属 project id (冗余便于查询) */
    @TableField("project_id")
    private Long projectId;

    /** 智能体名称 */
    @TableField("name")
    private String name;

    /** 角色 (如: 课程顾问 / 退费专员) */
    @TableField("role")
    private String role;

    /** emoji 图标 */
    @TableField("emoji")
    private String emoji;

    /** 描述 */
    @TableField("description")
    private String description;

    /** 品牌色 (CSS gradient) */
    @TableField("color")
    private String color;

    /** 工具 (逗号分隔, 简单场景; 复杂用独立表) */
    @TableField("tools")
    private String tools;

    /** 推荐模型 */
    @TableField("model")
    private String model;

    /** 排序 */
    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
