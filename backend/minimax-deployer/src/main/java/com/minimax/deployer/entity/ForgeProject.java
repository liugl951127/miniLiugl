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
 * Agent Forge 项目实体 (V2.0)
 *
 * 存储用户从客户需求文档 / AI 对话 / 模板创建的项目。
 * 一个项目可以有多个 release (版本), 每个 release 可以独立部署/回滚。
 *
 * 表: forge_project
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("forge_project")
public class ForgeProject {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 项目名 (用户自定义) */
    private String name;

    /** 行业领域 (教育/电商/金融/医疗/...) */
    private String industry;

    /** 业务场景描述 (LLM 解析后填充) */
    private String scenario;

    /** 原始需求文本 (用户输入) */
    private String rawRequirements;

    /** 解析后 JSON (LLM 输出, 包含 features/scale/compliance/integrations) */
    private String parsedRequirements;

    /** 推荐智能体 JSON (LLM 输出的 agent 列表) */
    private String recommendedAgents;

    /** 当前最新 release ID (冗余字段, 加速查询) */
    private Long currentReleaseId;

    /** 项目状态: DRAFT(草稿) / ANALYZED(已解析) / DEPLOYED(已部署) / ARCHIVED(归档) */
    private String status;

    /** 创建用户 ID */
    private Long ownerId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
