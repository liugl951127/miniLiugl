package com.minimax.rag.kg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.rag.kg.entity.KbExtractedRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KbExtractedRelationMapper extends BaseMapper<KbExtractedRelation> {

    /**
     * 按 kbId 列出全部关系
     */
    List<KbExtractedRelation> selectByKb(@Param("kbId") Long kbId);

    /**
     * 按 kbId 删除全部关系
     */
    int deleteByKb(@Param("kbId") Long kbId);

    /**
     * 按 kbId 统计
     */
    int countByKb(@Param("kbId") Long kbId);

    /**
     * 查找两个实体之间的所有直接边 (用于 RelationReasoner)
     */
    List<KbExtractedRelation> selectEdges(@Param("kbId") Long kbId,
                                          @Param("a") String a,
                                          @Param("b") String b);

    /**
     * 列出某实体的所有出边 (用于 BFS 推理)
     */
    List<KbExtractedRelation> selectOutEdges(@Param("kbId") Long kbId,
                                             @Param("entity") String entity);
}
