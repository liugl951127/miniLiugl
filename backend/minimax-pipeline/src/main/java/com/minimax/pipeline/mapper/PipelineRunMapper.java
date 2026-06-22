package com.minimax.pipeline.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.pipeline.entity.PipelineRun;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PipelineRunMapper extends BaseMapper<PipelineRun> {
}
