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
