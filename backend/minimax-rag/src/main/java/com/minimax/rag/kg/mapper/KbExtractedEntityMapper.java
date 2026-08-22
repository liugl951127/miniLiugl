package com.minimax.rag.kg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.rag.kg.entity.KbExtractedEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KbExtractedEntityMapper extends BaseMapper<KbExtractedEntity> {

    /**
     * 按 kbId 列出全部实体 (按 freq 倒序)
     */
    List<KbExtractedEntity> selectByKb(@Param("kbId") Long kbId);

    /**
     * 按 name 模糊搜索 (跨 KB 搜索, 用于 /kg/search 接口)
     */
    List<KbExtractedEntity> searchByName(@Param("keyword") String keyword,
                                         @Param("limit") int limit);

    /**
     * 按 kbId 删除全部实体
     */
    int deleteByKb(@Param("kbId") Long kbId);

    /**
     * 按 kbId 统计
     */
    int countByKb(@Param("kbId") Long kbId);
}
