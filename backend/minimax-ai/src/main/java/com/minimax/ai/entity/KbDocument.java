package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
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
    private String docId;
    /** 关联知识库 kbId */
    private String kbId;
    /** 文件名 (原文件名) */
    private String filename;
    /** MIME 类型 */
    private String mimeType;
    /** 文件大小 (字节) */
    private Long sizeBytes;
    /** 文件 SHA256 */
    private String sha256;
    /** 文件路径 (相对 ${DATA_ROOT}/kb/) */
    private String filePath;
    /** 来源: UPLOAD / URL / SYNC / IMPORT */
    private String source;
    /** 原始 URL (来源=URL 时) */
    private String sourceUrl;
    /** 状态: PENDING / PARSING / INDEXED / FAILED / DELETED */
    private String status;
    /** 分块总数 */
    private Integer chunkCount;
    /** 向量数 (= chunkCount) */
    private Integer embeddingCount;
    /** 解析错误 */
    private String error;
    /** 标签 (逗号分隔) */
    private String tags;
    /** 上传人 */
    private Long ownerId;
    /** 是否公开 */
    private Boolean isPublic;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
