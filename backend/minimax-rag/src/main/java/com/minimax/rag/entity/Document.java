package com.minimax.rag.entity;

import com.baomidou.mybatisplus.annotation.*;
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
    private Long kbId;
    private Long ownerId;
    private String title;
    private String sourceType;
    private String sourceUri;
    private String content;
    private Long sizeBytes;
    private String status;
    private String errorMsg;
    private Integer chunkCount;
    private String checksum;
    private String tags;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
