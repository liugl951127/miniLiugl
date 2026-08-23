package com.minimax.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("chat_session")
public class ChatSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    @TableField("title")
    private String title;
    @TableField("model")
    private String model;
    @TableField("system_prompt")
    private String systemPrompt;
    @TableField("temperature")
    private BigDecimal temperature;
    /** 0 归档 / 1 正常 */
    @TableField("status")
    private Integer status;
    @TableField("message_count")
    private Integer messageCount;
    @TableField("last_message_at")
    private LocalDateTime lastMessageAt;
    @TableField("tenant_id")
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
