package com.minimax.ai.modelmarket;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * ModelRating (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * 模型市场 - ModelRating.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 ModelRating 的业务能力</li>
 *   <li>参与 AI 平台整体架构</li>
 *   <li>支持 Spring 依赖注入</li>
 * </ul>
 *
 * <h2>依赖</h2>
 * <ul>
 *   <li>Spring Framework (自动注入)</li>
 *   <li>Lombok (简化代码)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V6.1
 */
@TableName("model_rating")
public class ModelRating {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("modelKey")
    private String modelKey;

    @TableField("userId")
    private Long userId;

    @TableField("username")
    private String username;

    /** 1-5 星 */
    @TableField("rating")
    private Integer rating;

    @TableField("comment")
    private String comment;

    @TableField(value = "createdAt", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
