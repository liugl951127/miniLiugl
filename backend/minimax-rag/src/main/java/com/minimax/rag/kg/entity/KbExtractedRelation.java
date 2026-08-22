// =============================================================
// MiniMax - RAG 模块 KB 抽取关系 (V7.3)
//
// 映射表: kb_extracted_relation
// 用途: EntityExtractor 启发式抽取出的实体关系
// 关系类型:
//   CO_OCCUR  同一段/同一句共现
//   RELATED   跨段关联
//   MENTION   提及
// 字段:
//   id        主键
//   kbId      所属知识库
//   srcEntity 起点实体
//   rel       关系类型
//   tgtEntity 终点实体
//   weight    权重 (累计共现次数)
//   createdAt 抽取时间
//
// 索引: (kb_id, src_entity, tgt_entity), (src_entity), (tgt_entity)
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
@TableName("kb_extracted_relation")
public class KbExtractedRelation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long kbId;
    private String srcEntity;
    private String rel;
    private String tgtEntity;
    private Integer weight;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
