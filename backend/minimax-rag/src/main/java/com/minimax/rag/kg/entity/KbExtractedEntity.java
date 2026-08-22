// =============================================================
// MiniMax - RAG 模块 KB 抽取实体 (V7.3)
//
// 映射表: kb_extracted_entity
// 用途: EntityExtractor 从知识库文档分词后写回的实体
// 字段:
//   id           主键
//   kbId         所属知识库
//   name         实体名 (跨 KB 唯一, 用于合并到 kg_entity)
//   type         PERSON/ORG/CONCEPT/PRODUCT/PLACE
//   freq         在本 KB 文档中累计出现频次
//   sourceDocId  首次发现该实体的文档 ID (可空)
//   createdAt    抽取时间
//
// 索引: (kb_id, freq), (name), (type)
//
// @author general
// @since 2026-08-22
// =============================================================

package com.minimax.rag.kg.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("kb_extracted_entity")
public class KbExtractedEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long kbId;
    private String name;
    private String type;
    private Integer freq;
    private Long sourceDocId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
