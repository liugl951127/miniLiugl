package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.TrainedModel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自研训练模型 Mapper (T1-backend-apis / P0)
 *
 * @since V7.2
 */
@Mapper
public interface TrainedModelMapper extends BaseMapper<TrainedModel> {
}
