package com.minimax.ai.marketplace.template;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * License 模板 Mapper (V3.5.2)
 */
@Mapper
public interface LicenseTemplateMapper extends BaseMapper<LicenseTemplate> {

    /**
     * 按 templateKey 查 (唯一)
     */
    @Select("SELECT * FROM license_template WHERE templateKey = #{templateKey} LIMIT 1")
    LicenseTemplate findByKey(String templateKey);

    /**
     * 按类型列 (公开 + 启用)
     */
    @Select("SELECT * FROM license_template WHERE licenseType = #{type} AND isPublic = 1 AND isActive = 1 ORDER BY priceCents ASC")
    List<LicenseTemplate> listByType(String type);

    /**
     * 列所有公开 + 启用的模板
     */
    @Select("SELECT * FROM license_template WHERE isPublic = 1 AND isActive = 1 ORDER BY priceCents ASC")
    List<LicenseTemplate> listPublic();

    /**
     * 按 ID 查
     */
    @Select("SELECT * FROM license_template WHERE id = #{id}")
    LicenseTemplate findById(Long id);
}
