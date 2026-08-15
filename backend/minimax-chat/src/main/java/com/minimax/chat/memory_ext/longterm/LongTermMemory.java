package com.minimax.chat.memory_ext.longterm;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("memory_long_term")
public class LongTermMemory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long sessionId;
    private String content;
    private String summary;
    private String role;

    /** 向量 (float[] 序列化，存 BLOB)。 */
    @TableField(select = false)   // 默认不查，量大
    private byte[] embedding;

    private Integer dim;
    private BigDecimal importance;
    private String tags;
    private Integer accessCount;
    private LocalDateTime lastAccessAt;
    private LocalDateTime expiresAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
