package com.minimax.common.ratelimit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RateLimitRuleMapper extends BaseMapper<RateLimitRule> {
}
