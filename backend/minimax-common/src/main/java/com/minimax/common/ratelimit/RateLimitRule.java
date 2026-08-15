package com.minimax.common.ratelimit;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 限流规则表 (V4.3 补 entity).
 * 对应表: rate_limit_rule (在 13_optimization.sql)
 *
 * 把 Bucket4j 内存 hardcode 的 ip/user/global 规则搬到 DB, 可动态调整.
 */
@Data
@TableName("rate_limit_rule")
public class RateLimitRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作用域: ip/user/global/api/api+method */
    private String scope;

    /** 规则名 (唯一) */
    @TableField("`key`")
    private String key;

    /** 描述 */
    private String description;

    /** 桶容量 (突发) */
    private Integer capacity;

    /** 长期速率 (refillGreedy tokens) */
    private Integer refillTokens;

    /** 周期 (秒) */
    private Integer periodSeconds;

    /** 0=禁用 1=启用 */
    private Integer enabled;

    /** 优先级 (高先匹配) */
    private Integer priority;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
