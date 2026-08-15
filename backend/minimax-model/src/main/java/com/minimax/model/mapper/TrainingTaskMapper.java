package com.minimax.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.model.entity.TrainingTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrainingTaskMapper extends BaseMapper<TrainingTask> {
}
