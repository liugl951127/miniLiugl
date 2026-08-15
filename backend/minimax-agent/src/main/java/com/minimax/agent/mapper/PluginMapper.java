package com.minimax.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.agent.entity.Plugin;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PluginMapper extends BaseMapper<Plugin> {
}
