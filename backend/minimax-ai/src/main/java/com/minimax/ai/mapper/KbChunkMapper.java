package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.KbChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * KbChunkMapper (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * MyBatis Mapper - KbChunkMapper.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 KbChunkMapper 的业务能力</li>
 *   <li>参与 AI 平台整体架构</li>
 *   <li>支持 Spring 依赖注入</li>
 * </ul>
 *
 * <h2>依赖</h2>
 * <ul>
 *   <li>Spring Framework (自动注入)</li>
 *   <li>Lombok (简化代码)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V6.1
 */
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
