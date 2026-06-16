package com.minimax.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.rag.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

    /** 拉 KB 内所有 chunk (含向量)。单次建议 limit ≤ 5000。 */
    List<DocumentChunk> selectEmbeddingsByKb(@Param("kbId") Long kbId,
                                             @Param("limit") int limit);

    /** 拉 doc 内所有 chunk (按 chunk_index 排序)。 */
    List<DocumentChunk> selectByDoc(@Param("docId") Long docId);

    /** 召回后 +1。 */
    int touchAccess(@Param("id") Long id);

    /** 删除 doc 所有 chunk。 */
    int deleteByDoc(@Param("docId") Long docId);
}
