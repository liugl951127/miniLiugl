package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档 (V3.4.0 自研知识库)
 *
 * <p>用户上传的原始文档, 经过解析分块后生成 kb_chunk
 */
@Data
@TableName("kb_document")
public class KbDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务 docId (UUID) */
    @TableField("doc_id")
    private String docId;
    /** 关联知识库 kbId */
    @TableField("kb_id")
    private String kbId;
    /** 文件名 (原文件名) */
    @TableField("filename")
    private String filename;
    /** MIME 类型 */
    @TableField("mime_type")
    private String mimeType;
    /** 文件大小 (字节) */
    @TableField("size_bytes")
    private Long sizeBytes;
    /** 文件 SHA256 */
    @TableField("sha256")
    private String sha256;
    /** 文件路径 (相对 ${DATA_ROOT}/kb/) */
    @TableField("file_path")
    private String filePath;
    /** 来源: UPLOAD / URL / SYNC / IMPORT */
    @TableField("source")
    private String source;
    /** 原始 URL (来源=URL 时) */
    @TableField("source_url")
    private String sourceUrl;
    /** 状态: PENDING / PARSING / INDEXED / FAILED / DELETED */
    @TableField("status")
    private String status;
    /** 分块总数 */
    @TableField("chunk_count")
    private Integer chunkCount;
    /** 向量数 (= chunkCount) */
    @TableField("embedding_count")
    private Integer embeddingCount;
    /** 解析错误 */
    @TableField("error")
    private String error;
    /** 标签 (逗号分隔) */
    @TableField("tags")
    private String tags;
    /** 上传人 */
    @TableField("owner_id")
    private Long ownerId;
    /** 是否公开 */
    @TableField("is_public")
    private Boolean isPublic;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
