package com.minimax.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("model_quota")
public class ModelQuota implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("model_id")
    private Long modelId;
    @TableField("quota_date")
    private LocalDate quotaDate;
    @TableField("used_tokens")
    private Long usedTokens;
    @TableField("used_requests")
    private Integer usedRequests;
    @TableField("limit_tokens")
    private Long limitTokens;
    @TableField("limit_requests")
    private Integer limitRequests;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
