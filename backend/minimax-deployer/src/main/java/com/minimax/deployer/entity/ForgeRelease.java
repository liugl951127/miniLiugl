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
 * Agent Forge 发布版本实体 (V2.0)
 *
 * 语义化版本 (SemVer): MAJOR.MINOR.PATCH
 *  - MAJOR: 重大变更, 不向后兼容
 *  - MINOR: 新增功能, 向后兼容
 *  - PATCH: Bug 修复
 *
 * 每个 release 包含完整的智能体定义 + 部署配置 (YAML 序列化)。
 * 支持 diff/rollback/changelog。
 *
 * 表: forge_release
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("forge_release")
public class ForgeRelease {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目 ID */
    
    @TableField("project_id")
    private Long projectId;

    /** 语义化版本号, 如: 1.0.0 */
    private String version;

    /** 发布标题 */
    private String title;

    /** 变更日志 (Markdown) */
    private String changelog;

    /** 智能体定义 JSON (完整 designer 输出) */
    
    @TableField("agent_definitions")
    private String agentDefinitions;

    /** 部署配置 JSON (目标/资源/凭证) */
    
    @TableField("deploy_config")
    private String deployConfig;

    /** 生成的 Dockerfile / K8s manifest (YAML/JSON) */
    private String manifests;

    /** 状态: DRAFT(草稿) / BUILDING(构建中) / DEPLOYING(部署中) / ACTIVE(运行中) / FAILED(失败) / ROLLED_BACK(已回滚) */
    private String status;

    /** 部署目标: DOCKER / K8S / CLOUD / EDGE */
    
    @TableField("deploy_target")
    private String deployTarget;

    /** 当前部署实例数 */
    private Integer replicas;

    /** 镜像仓库地址 */
    
    @TableField("image_registry")
    private String imageRegistry;

    /** 镜像 tag (默认等于 version) */
    
    @TableField("image_tag")
    private String imageTag;

    /** 部署耗时 (秒) */
    
    @TableField("deploy_duration")
    private Integer deployDuration;

    /** 创建用户 ID */
    
    @TableField("created_by")
    private Long createdBy;

    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    
    @TableField("deployed_at")
    private LocalDateTime deployedAt;
}
