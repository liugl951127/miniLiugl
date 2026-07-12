package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.ModelVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ModelVersionMapper extends BaseMapper<ModelVersion> {

    @Select("SELECT * FROM model_version WHERE versionId = #{versionId} LIMIT 1")
    ModelVersion findByVersionId(@Param("versionId") String versionId);

    @Select("SELECT * FROM model_version WHERE modelEntryId = #{modelEntryId} ORDER BY createdAt DESC")
    List<ModelVersion> findByEntry(@Param("modelEntryId") Long modelEntryId);

    @Select("SELECT * FROM model_version WHERE modelEntryId = #{modelEntryId} AND isLatest = TRUE LIMIT 1")
    ModelVersion findLatest(@Param("modelEntryId") Long modelEntryId);

    @Update("UPDATE model_version SET isLatest = (id = #{id}) WHERE modelEntryId = #{modelEntryId}")
    int setLatest(@Param("id") Long id, @Param("modelEntryId") Long modelEntryId);

    @Update("UPDATE model_version SET status = #{status}, updatedAt = NOW() WHERE versionId = #{versionId}")
    int updateStatus(@Param("versionId") String versionId, @Param("status") String status);
}
