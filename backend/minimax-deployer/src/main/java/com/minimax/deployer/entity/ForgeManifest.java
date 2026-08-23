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
 * Forge Manifest (V4.0) - 独立子表
 *
 * 替代 ForgeRelease.manifests (JSON 字符串)
 * 每个 manifest 文件 (Dockerfile / k8s-yaml / argocd-app) 是独立行
 * 支持按 type 索引/查询, content hash 用于版本对比
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("forge_manifest")
public class ForgeManifest implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("release_id")
    private Long releaseId;

    /** manifest 类型: dockerfile / k8s-deployment / k8s-service / k8s-configmap / k8s-hpa / k8s-ingress / argocd-app / edge-script */
    @TableField("type")
    private String type;

    /** 文件路径 (如: docker/Dockerfile, k8s/deployment.yaml) */
    @TableField("path")
    private String path;

    /** 文件内容 */
    @TableField("content")
    private String content;

    /** SHA256 hash (用于版本对比) */
    @TableField("content_hash")
    private String contentHash;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
