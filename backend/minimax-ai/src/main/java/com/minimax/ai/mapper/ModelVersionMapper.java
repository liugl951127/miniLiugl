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

    @Select("SELECT * FROM model_version WHERE version_id = #{version_id} LIMIT 1")
    ModelVersion findByVersionId(@Param("versionId") String versionId);

    @Select("SELECT * FROM model_version WHERE model_entry_id = #{model_entry_id} ORDER BY created_at DESC")
    List<ModelVersion> findByEntry(@Param("modelEntryId") Long modelEntryId);

    @Select("SELECT * FROM model_version WHERE model_entry_id = #{model_entry_id} AND is_latest = TRUE LIMIT 1")
    ModelVersion findLatest(@Param("modelEntryId") Long modelEntryId);

    @Update("UPDATE model_version SET is_latest = (id = #{id}) WHERE model_entry_id = #{model_entry_id}")
    int setLatest(@Param("id") Long id, @Param("modelEntryId") Long modelEntryId);

    @Update("UPDATE model_version SET status = #{status}, updated_at = NOW() WHERE version_id = #{version_id}")
    int updateStatus(@Param("versionId") String versionId, @Param("status") String status);
}
