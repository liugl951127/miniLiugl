package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sensitive_word")
public class SensitiveWord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String word;
    private String category;
    private String level;
    private String action;
    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
