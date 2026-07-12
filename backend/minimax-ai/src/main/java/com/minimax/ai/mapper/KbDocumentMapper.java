package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.KbDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface KbDocumentMapper extends BaseMapper<KbDocument> {

    @Select("SELECT * FROM kb_document WHERE docId = #{docId} LIMIT 1")
    KbDocument findByDocId(@Param("docId") String docId);

    @Select("SELECT * FROM kb_document WHERE kbId = #{kbId} ORDER BY createdAt DESC")
    List<KbDocument> findByKb(@Param("kbId") String kbId);

    @Select("SELECT * FROM kb_document WHERE ownerId = #{ownerId} ORDER BY createdAt DESC")
    List<KbDocument> findByOwner(@Param("ownerId") Long ownerId);

    @Select("SELECT * FROM kb_document WHERE status = #{status} ORDER BY createdAt ASC")
    List<KbDocument> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM kb_document WHERE isPublic = TRUE ORDER BY createdAt DESC LIMIT #{limit}")
    List<KbDocument> findPublic(@Param("limit") int limit);

    @Update("UPDATE kb_document SET status = #{status}, chunkCount = #{chunkCount}, " +
            "embeddingCount = #{embedCount}, error = #{error}, updatedAt = NOW() WHERE docId = #{docId}")
    int updateIndexResult(@Param("docId") String docId,
                          @Param("status") String status,
                          @Param("chunkCount") int chunkCount,
                          @Param("embedCount") int embedCount,
                          @Param("error") String error);
}
