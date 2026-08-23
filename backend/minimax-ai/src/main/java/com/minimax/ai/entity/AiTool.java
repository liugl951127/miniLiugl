package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 工具定义 (V2.5)
 */
@Data
@TableName("ai_tool")
public class AiTool {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工具唯一编码 */
    @TableField("code")
    private String code;

    /** 工具名称 */
    @TableField("name")
    private String name;

    /** 分类: DATA_CLEAN / DATA_ANALYZE / CODE_GEN / CHAT / SQL_QUERY / CUSTOM */
    @TableField("category")
    private String category;

    /** 工具描述 */
    @TableField("description")
    private String description;

    /** 图标 */
    @TableField("icon")
    private String icon;

    /** 是否启用 0否 1是 */
    @TableField("enabled")
    private Integer enabled;

    /** 是否内置 0否 1是 */
    @TableField("builtin")
    private Integer builtin;

    /** 输入 JSON Schema */
    @TableField("input_schema")
    private String inputSchema;

    /** 输出 JSON Schema */
    @TableField("output_schema")
    private String outputSchema;

    /** 默认配置 JSON */
    @TableField("default_config")
    private String defaultConfig;

    /** 实现方式: java / sql / prompt / http */
    @TableField("impl_type")
    private String implType;

    /** 实现类/SQL/Prompt/URL */
    @TableField("impl_value")
    private String implValue;

    /** 每分钟调用次数 */
    @TableField("rate_limit")
    private Integer rateLimit;

    /** 超时 (秒) */
    @TableField("timeout_seconds")
    private Integer timeoutSeconds;

    /** 角色: USER / ADMIN / SUPER_ADMIN */
    @TableField("role_required")
    private String roleRequired;

    /** 标签 */
    @TableField("tags")
    private String tags;

    /** 版本 */
    @TableField("version")
    private String version;

    /** 作者 */
    @TableField("author")
    private String author;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * V6.8.1: 工具状态 (0=disabled, 1=enabled, 2=deprecated)
     */
    private Integer status = 1;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
