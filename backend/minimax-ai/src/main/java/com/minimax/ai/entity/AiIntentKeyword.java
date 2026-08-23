package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 意图关键词实体 (V2.8.5)
 *
 * <h3>设计目标</h3>
 * 把 KeywordEngine 里的硬编码关键词搬到 DB, 运营可后台动态调整
 * 不需要重新部署.
 *
 * <h3>字段</h3>
 * - intent: 意图枚举 (GENERATE_CHART / GENERATE_MUSIC / ...)
 * - keyword: 关键词文本
 * - weight: 权重 (1-10, 默认 1)
 * - is_regex: 是否正则 (0=否, 1=是)
 * - enabled: 启用 (0=否, 1=是)
 */
@Data
@TableName("ai_intent_keyword")
public class AiIntentKeyword {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 意图 */
    @TableField("intent")
    private String intent;
    /** 关键词或正则 */
    @TableField("keyword")
    private String keyword;
    /** 权重 (1-10) */
    @TableField("weight")
    private Integer weight;
    /** 是否正则 0/1 */
    @TableField("is_regex")
    private Integer isRegex;
    /** 启用 0/1 */
    @TableField("enabled")
    private Integer enabled;
    /** 语言 (zh/en) */
    @TableField("language")
    private String language;
    /** 备注 */
    @TableField("remark")
    private String remark;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
