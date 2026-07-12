package com.minimax.ai.marketplace;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgentRatingMapper extends BaseMapper<AgentRating> {

    @Select("SELECT * FROM agent_rating WHERE agentKey = #{agentKey} ORDER BY createdAt DESC LIMIT #{limit}")
    List<AgentRating> findByAgentKey(@Param("agentKey") String agentKey, @Param("limit") int limit);

    @Select("SELECT * FROM agent_rating WHERE agentKey = #{agentKey} AND userId = #{userId} LIMIT 1")
    AgentRating findUserRating(@Param("agentKey") String agentKey, @Param("userId") Long userId);
}
