package com.minimax.rag.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField("doc_id")
    private Long docId;
    @TableField("kb_id")
    private Long kbId;
    @TableField("owner_id")
    private Long ownerId;
    @TableField("chunk_index")
    private Integer chunkIndex;
    @TableField("content")
    private String content;

    @TableField(select = false)
    private byte[] embedding;

    private Integer dim;
    private Integer charCount;
    private Integer startPos;
    @TableField("end_pos")
    private Integer endPos;
    @TableField("access_count")
    private Integer accessCount;
    @TableField("last_access_at")
    private LocalDateTime lastAccessAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
