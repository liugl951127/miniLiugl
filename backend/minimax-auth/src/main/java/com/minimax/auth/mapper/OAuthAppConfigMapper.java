package com.minimax.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.auth.entity.OAuthAppConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OAuthAppConfigMapper extends BaseMapper<OAuthAppConfig> {
}