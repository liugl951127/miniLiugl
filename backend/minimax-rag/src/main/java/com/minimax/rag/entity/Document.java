package com.minimax.rag.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("document")
public class Document implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("kb_id")
    private Long kbId;
    @TableField("owner_id")
    private Long ownerId;
    @TableField("title")
    private String title;
    @TableField("source_type")
    private String sourceType;
    @TableField("source_uri")
    private String sourceUri;
    @TableField("content")
    private String content;
    @TableField("size_bytes")
    private Long sizeBytes;
    @TableField("status")
    private String status;
    @TableField("error_msg")
    private String errorMsg;
    @TableField("chunk_count")
    private Integer chunkCount;
    @TableField("checksum")
    private String checksum;
    @TableField("tags")
    private String tags;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
