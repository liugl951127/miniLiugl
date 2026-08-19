package com.minimax.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户偏好设置 (V6.8.9)
 * 支持深色模式、语言偏好等。
 */
@Data
@TableName("user_preferences")
public class UserPreference {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID（唯一约束） */
    private Long userId;

    /** 主题: light | dark */
    private String theme = "light";

    /** 语言: zh-CN | en */
    private String language = "zh-CN";

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
