package com.minimax.pipeline.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 规则定义 (T1-backend-apis / P0)
 *
 * 用于画布规则引擎 / 内容审核 / 限流等业务。
 * jsonContent 字段保存 DSL/JSON 规则体, 由前端编辑器维护。
 *
 * @since V7.2
 */
@Data
@TableName("rule_definition")
public class RuleDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则名 (唯一+可读) */
    @TableField("name")
    private String name;

    /** 规则 JSON DSL */
    @TableField("json_content")
    private String jsonContent;

    /** 作用域: GLOBAL / TENANT / USER */
    @TableField("scope")
    private String scope;

    /** 0=禁用 1=启用 */
    @TableField("enabled")
    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
