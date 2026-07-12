package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.ModelLicense;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ModelLicenseMapper extends BaseMapper<ModelLicense> {

    @Select("SELECT * FROM model_license WHERE licenseKey = #{licenseKey} LIMIT 1")
    ModelLicense findByKey(@Param("licenseKey") String licenseKey);

    @Select("SELECT * FROM model_license WHERE userId = #{userId} AND status = 'ACTIVE'")
    List<ModelLicense> findActiveByUser(@Param("userId") Long userId);

    @Select("SELECT * FROM model_license WHERE userId = #{userId} AND modelEntryId = #{modelEntryId} AND status = 'ACTIVE'")
    List<ModelLicense> findByUserAndEntry(@Param("userId") Long userId, @Param("modelEntryId") Long modelEntryId);

    @Update("UPDATE model_license SET usedCalls = usedCalls + 1, updatedAt = NOW() WHERE id = #{id} AND (quotaCalls = 0 OR usedCalls < quotaCalls)")
    int incrementUsage(@Param("id") Long id);
}
