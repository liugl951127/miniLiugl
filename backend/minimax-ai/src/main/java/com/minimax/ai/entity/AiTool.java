package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
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
    private String code;

    /** 工具名称 */
    private String name;

    /** 分类: DATA_CLEAN / DATA_ANALYZE / CODE_GEN / CHAT / SQL_QUERY / CUSTOM */
    private String category;

    /** 工具描述 */
    private String description;

    /** 图标 */
    private String icon;

    /** 是否启用 0否 1是 */
    private Integer enabled;

    /** 是否内置 0否 1是 */
    private Integer builtin;

    /** 输入 JSON Schema */
    private String inputSchema;

    /** 输出 JSON Schema */
    private String outputSchema;

    /** 默认配置 JSON */
    private String defaultConfig;

    /** 实现方式: java / sql / prompt / http */
    private String implType;

    /** 实现类/SQL/Prompt/URL */
    private String implValue;

    /** 每分钟调用次数 */
    private Integer rateLimit;

    /** 超时 (秒) */
    private Integer timeoutSeconds;

    /** 角色: USER / ADMIN / SUPER_ADMIN */
    private String roleRequired;

    /** 标签 */
    private String tags;

    /** 版本 */
    private String version;

    /** 作者 */
    private String author;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
