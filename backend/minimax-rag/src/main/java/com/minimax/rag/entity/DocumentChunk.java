package com.minimax.rag.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("document_chunk")
public class DocumentChunk implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long docId;
    private Long kbId;
    private Long ownerId;
    private Integer chunkIndex;
    private String content;

    @TableField(select = false)
    private byte[] embedding;

    private Integer dim;
    private Integer charCount;
    private Integer startPos;
    private Integer endPos;
    private Integer accessCount;
    private LocalDateTime lastAccessAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
