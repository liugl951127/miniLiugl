package com.minimax.ai.marketplace;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * MarketplaceMapper (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * 模型市场 - MarketplaceMapper.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 MarketplaceMapper 的业务能力</li>
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
public interface MarketplaceMapper extends BaseMapper<MarketplaceAgent> {

    @Update("UPDATE agent_marketplace SET usage_count = usage_count + 1 WHERE agent_key = #{agentKey}")
    int incrementUsage(@Param("agentKey") String agentKey);

    @Update("UPDATE agent_marketplace SET " +
            "avg_rating = (SELECT AVG(rating) FROM agent_rating WHERE agent_key = #{agentKey}), " +
            "rating_count = (SELECT COUNT(*) FROM agent_rating WHERE agent_key = #{agentKey}) " +
            "WHERE agent_key = #{agentKey}")
    int updateRatingStats(@Param("agentKey") String agentKey);
}
