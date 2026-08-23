package com.minimax.deployer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Forge Project (V4.0)
 *
 * V4.0 清理: 删 parsed_requirements / recommended_agents (改用 forge_agent + forge_workflow_step 子表)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("forge_project")
public class ForgeProject {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("industry")
    private String industry;

    @TableField("scenario")
    private String scenario;

    @TableField("raw_requirements")
    private String rawRequirements;

    @TableField("current_release_id")
    private Long currentReleaseId;

    @TableField("status")
    private String status;

    @TableField("owner_id")
    private Long ownerId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
