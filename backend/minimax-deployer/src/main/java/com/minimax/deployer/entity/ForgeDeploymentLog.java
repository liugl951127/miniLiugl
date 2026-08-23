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
 * Forge Deployment Log (V4.0) - 独立子表
 *
 * 替代 ForgeDeployment.logs (TEXT 字段)
 * 部署日志每条独立行, 可按时间排序/分页/过滤
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("forge_deployment_log")
public class ForgeDeploymentLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("deployment_id")
    private Long deploymentId;

    /** 日志级别: INFO / WARN / ERROR */
    @TableField("level")
    private String level;

    /** 日志阶段 (如: build / push / deploy / health) */
    @TableField("stage")
    private String stage;

    /** 日志内容 */
    @TableField("message")
    private String message;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
