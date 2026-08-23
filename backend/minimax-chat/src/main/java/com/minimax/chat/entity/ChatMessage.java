package com.minimax.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private Long sessionId;
    @TableField("user_id")
    private Long userId;
    /** user / assistant / system / tool */
    @TableField("role")
    private String role;
    @TableField("content")
    private String content;
    @TableField("tokens")
    private Integer tokens;
    @TableField("finish_reason")
    private String finishReason;
    @TableField("error_message")
    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
