package com.minimax.ai.marketplace;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户上传的自定义 Agent (V2.9.0 AI Agent Marketplace)
 *
 * <p>用户可上传自己的 Agent 到市场, 共享给其他用户使用, 也可评分/评论.</p>
 *
 * <h3>设计</h3>
 * <ul>
 *   <li>definitionJson: Agent 的 JSON 定义 (含 capabilities/permissions/tools)</li>
 *   <li>visibility: PUBLIC (公开) / PRIVATE (私有) / UNLISTED (凭链接访问)</li>
 *   <li>审核: 状态机 (PENDING → APPROVED → PUBLISHED / REJECTED)</li>
 *   <li>评分: 1-5 星, 多次评分取平均</li>
 * </ul>
 *
 * @author MiniMax
 * @since V2.9.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent_marketplace")
public class MarketplaceAgent {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** Agent 唯一标识 (URL slug) */
    @TableField("agentKey")
    private String agentKey;

    /** 显示名 */
    @TableField("name")
    private String name;

    /** 描述 */
    @TableField("description")
    private String description;

    /** 分类: SHOPPING/HOTEL/ENTERTAINMENT/EDUCATION/PRODUCTIVITY/CUSTOM */
    @TableField("category")
    private String category;

    /** 图标 emoji */
    @TableField("icon")
    private String icon;

    /** 作者用户 ID */
    @TableField("authorId")
    private Long authorId;

    /** 作者用户名 */
    @TableField("authorName")
    private String authorName;

    /** Agent 定义 JSON (含 capabilities/permissions/tools) */
    @TableField("definitionJson")
    private String definitionJson;

    /** 版本 (semver) */
    @TableField("version")
    private String version;

    /** 可见性: PUBLIC / PRIVATE / UNLISTED */
    @TableField("visibility")
    private String visibility;

    /** 状态: PENDING/APPROVED/PUBLISHED/REJECTED */
    @TableField("status")
    private String status;

    /** 下载/使用次数 */
    @TableField("usageCount")
    private Long usageCount;

    /** 平均评分 (1-5) */
    @TableField("avgRating")
    private Double avgRating;

    /** 评分次数 */
    @TableField("ratingCount")
    private Long ratingCount;

    /** 标签 (逗号分隔) */
    @TableField("tags")
    private String tags;

    /** 关联的 capability (逗号分隔) */
    @TableField("capabilities")
    private String capabilities;

    @TableField(value = "createdAt", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updatedAt", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("publishedAt")
    private LocalDateTime publishedAt;
}
