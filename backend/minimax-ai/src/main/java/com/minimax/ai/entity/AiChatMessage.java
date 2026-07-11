package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_chat_message")
public class AiChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private String role;

    private String content;

    private String toolCode;

    private String toolInput;

    private String toolOutput;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
