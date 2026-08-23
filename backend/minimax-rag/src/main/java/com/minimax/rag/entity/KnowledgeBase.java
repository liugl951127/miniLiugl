package com.minimax.rag.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_base")
public class KnowledgeBase implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("owner_id")
    private Long ownerId;
    @TableField("tenant_id")
    private Long tenantId;
    @TableField("name")
    private String name;
    @TableField("description")
    private String description;
    @TableField("visibility")
    private String visibility;
    @TableField("doc_count")
    private Integer docCount;
    @TableField("chunk_count")
    private Integer chunkCount;
    @TableField("tags")
    private String tags;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
