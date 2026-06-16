package com.minimax.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.rag.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {
    int incDocCount(@Param("id") Long id, @Param("delta") int delta);
    int incChunkCount(@Param("id") Long id, @Param("delta") int delta);
}
