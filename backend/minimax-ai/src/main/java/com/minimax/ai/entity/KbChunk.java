package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库分块 (V3.4.0 自研知识库)
 *
 * <p>文档解析后切成的语义块, 附带向量用于语义检索
 */
@Data
@TableName("kb_chunk")
public class KbChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务 chunkId (UUID) */
    @TableField("chunk_id")
    private String chunkId;
    /** 关联文档 docId */
    @TableField("doc_id")
    private String docId;
    /** 关联知识库 kbId */
    @TableField("kb_id")
    private String kbId;
    /** 分块序号 (从 0 开始) */
    @TableField("seq")
    private Integer seq;
    /** 文本内容 */
    @TableField("content")
    private String content;
    /** 字符数 */
    @TableField("char_count")
    private Integer charCount;
    /** token 数 (估算) */
    @TableField("token_count")
    private Integer tokenCount;
    /** 向量 (JSON 数组, 维度=model.dim) */
    @TableField("embedding")
    private String embedding;
    /** 向量模型名 */
    @TableField("embedding_model")
    private String embeddingModel;
    /** 关键词 (逗号分隔) */
    @TableField("keywords")
    private String keywords;
    /** 摘要 */
    @TableField("summary")
    private String summary;
    /** 位置信息 (页码/章节, JSON) */
    @TableField("location")
    private String location;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
