package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.ModelLicense;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * ModelLicenseMapper (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * MyBatis Mapper - ModelLicenseMapper.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 ModelLicenseMapper 的业务能力</li>
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
public interface ModelLicenseMapper extends BaseMapper<ModelLicense> {

    @Select("SELECT * FROM model_license WHERE license_key = #{license_key} LIMIT 1")
    ModelLicense findByKey(@Param("licenseKey") String licenseKey);

    @Select("SELECT * FROM model_license WHERE user_id = #{user_id} AND status = 'ACTIVE'")
    List<ModelLicense> findActiveByUser(@Param("userId") Long userId);

    @Select("SELECT * FROM model_license WHERE user_id = #{user_id} AND model_entry_id = #{model_entry_id} AND status = 'ACTIVE'")
    List<ModelLicense> findByUserAndEntry(@Param("userId") Long userId, @Param("modelEntryId") Long modelEntryId);

    @Update("UPDATE model_license SET used_calls = used_calls + 1, updated_at = NOW() WHERE id = #{id} AND (quota_calls = 0 OR used_calls < quota_calls)")
    int incrementUsage(@Param("id") Long id);
}
