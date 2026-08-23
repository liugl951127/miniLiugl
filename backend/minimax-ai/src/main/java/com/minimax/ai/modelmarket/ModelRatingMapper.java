package com.minimax.ai.modelmarket;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * ModelRatingMapper (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * 模型市场 - ModelRatingMapper.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 ModelRatingMapper 的业务能力</li>
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
public interface ModelRatingMapper extends BaseMapper<ModelRating> {

    @Select("SELECT * FROM model_rating WHERE model_key = #{modelKey} ORDER BY created_at DESC LIMIT #{limit}")
    List<ModelRating> findByModelKey(@Param("modelKey") String modelKey, @Param("limit") int limit);

    @Select("SELECT * FROM model_rating WHERE model_key = #{modelKey} AND user_id = #{userId} LIMIT 1")
    ModelRating findUserRating(@Param("modelKey") String modelKey, @Param("userId") Long userId);
}
