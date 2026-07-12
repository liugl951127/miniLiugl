package com.minimax.ai.marketplace;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MarketplaceMapper extends BaseMapper<MarketplaceAgent> {

    @Update("UPDATE agent_marketplace SET usageCount = usageCount + 1 WHERE agentKey = #{agentKey}")
    int incrementUsage(@Param("agentKey") String agentKey);

    @Update("UPDATE agent_marketplace SET " +
            "avgRating = (SELECT AVG(rating) FROM agent_rating WHERE agentKey = #{agentKey}), " +
            "ratingCount = (SELECT COUNT(*) FROM agent_rating WHERE agentKey = #{agentKey}) " +
            "WHERE agentKey = #{agentKey}")
    int updateRatingStats(@Param("agentKey") String agentKey);
}
