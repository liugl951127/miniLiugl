package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.KbChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KbChunkMapper extends BaseMapper<KbChunk> {

    @Select("SELECT * FROM kb_chunk WHERE doc_id = #{doc_id} ORDER BY seq ASC")
    List<KbChunk> findByDoc(@Param("docId") String docId);

    @Select("SELECT * FROM kb_chunk WHERE kb_id = #{kb_id} ORDER BY seq ASC")
    List<KbChunk> findByKb(@Param("kbId") String kbId);

    @Select("SELECT * FROM kb_chunk WHERE content LIKE CONCAT('%', #{keyword}, '%') LIMIT #{limit}")
    List<KbChunk> findByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM kb_chunk WHERE doc_id = #{doc_id}")
    int countByDoc(@Param("docId") String docId);

    @Select("SELECT COUNT(*) FROM kb_chunk WHERE kb_id = #{kb_id}")
    int countByKb(@Param("kbId") String kbId);
}
