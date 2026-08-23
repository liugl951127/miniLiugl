package com.minimax.ai.modelmarket;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * ModelMarketMapper (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * 模型市场 - ModelMarketMapper.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 ModelMarketMapper 的业务能力</li>
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
public interface ModelMarketMapper extends BaseMapper<ModelEntry> {

    @Update("UPDATE model_market SET download_count = download_count + 1 WHERE model_key = #{modelKey}")
    int incrementDownload(@Param("modelKey") String modelKey);

    @Update("UPDATE model_market SET " +
            "avg_rating = COALESCE((SELECT AVG(rating) FROM model_rating WHERE model_key = #{modelKey}), 0), " +
            "rating_count = (SELECT COUNT(*) FROM model_rating WHERE model_key = #{modelKey}) " +
            "WHERE model_key = #{modelKey}")
    int updateRatingStats(@Param("modelKey") String modelKey);
}
