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
     * 按 template_key 查 (唯一)
     */
    @Select("SELECT * FROM license_template WHERE template_key = #{templateKey} LIMIT 1")
    LicenseTemplate findByKey(String templateKey);
    /**
     * 按类型列 (公开 + 启用)
     */
    @Select("SELECT * FROM license_template WHERE license_type = #{type} AND is_public = 1 AND is_active = 1 ORDER BY price_cents ASC")
    List<LicenseTemplate> listByType(String type);
    /**
     * 列所有公开 + 启用的模板
     */
    @Select("SELECT * FROM license_template WHERE is_public = 1 AND is_active = 1 ORDER BY price_cents ASC")
    List<LicenseTemplate> listPublic();
    /**
     * 按 ID 查
     */
    @Select("SELECT * FROM license_template WHERE id = #{id}")
    LicenseTemplate findById(Long id);
}
