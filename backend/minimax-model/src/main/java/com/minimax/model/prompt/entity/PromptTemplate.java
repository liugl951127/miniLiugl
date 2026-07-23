package com.minimax.model.prompt.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Prompt 模板实体.
 * 支持变量占位符 {{variable}}，前端可动态解析填值。
 */
@Data
@TableName("prompt_template")
public class PromptTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板名称 */
    private String name;

    /** 模板描述 */
    private String description;

    /** 分类: 翻译/代码/写作/分析/客服/其他 */
    private String category;

    /** 模板内容，支持 {{variable}} 占位符 */
    private String content;

    /**
     * 变量列表 JSON 数组.
     * 例: [{"name":"语言","description":"目标语言","required":true}]
     */
    private String variables;

    /** 创建者用户ID */
    private Long creatorId;

    /** 创建者用户名 */
    private String creatorName;

    /** 是否公开模板 (true=所有人可用, false=仅创建者) */
    private Boolean isPublic;

    /** 使用次数 */
    private Integer useCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
