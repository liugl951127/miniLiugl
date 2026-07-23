package com.minimax.pipeline.function_ext.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("function_call_log")
public class FunctionCallLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long sessionId;
    private String toolName;
    private String arguments;
    private String result;
    private String status;
    private String errorMsg;
    private Integer durationMs;
    private String ip;
    private String userAgent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
