package com.minimax.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
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
    private String chunkId;
    /** 关联文档 docId */
    private String docId;
    /** 关联知识库 kbId */
    private String kbId;
    /** 分块序号 (从 0 开始) */
    private Integer seq;
    /** 文本内容 */
    private String content;
    /** 字符数 */
    private Integer charCount;
    /** token 数 (估算) */
    private Integer tokenCount;
    /** 向量 (JSON 数组, 维度=model.dim) */
    private String embedding;
    /** 向量模型名 */
    private String embeddingModel;
    /** 关键词 (逗号分隔) */
    private String keywords;
    /** 摘要 */
    private String summary;
    /** 位置信息 (页码/章节, JSON) */
    private String location;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
