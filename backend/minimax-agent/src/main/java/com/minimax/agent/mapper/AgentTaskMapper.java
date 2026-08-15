package com.minimax.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.agent.entity.AgentTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentTaskMapper extends BaseMapper<AgentTask> {
}
