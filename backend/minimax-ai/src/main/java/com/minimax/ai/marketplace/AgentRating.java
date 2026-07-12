package com.minimax.ai.marketplace;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent 评分 (V2.9.0)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent_rating")
public class AgentRating {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("agentKey")
    private String agentKey;

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
