package com.minimax.pipeline.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.pipeline.entity.RuleDefinition;
import org.apache.ibatis.annotations.Mapper;

/**
 * 规则定义 Mapper (T1-backend-apis / P0)
 *
 * <h2>职责</h2>
 * MyBatis-Plus BaseMapper - 复用 insert/update/delete/selectById/list/page
 *
 * @since V7.2
 */
@Mapper
public interface RuleDefinitionMapper extends BaseMapper<RuleDefinition> {
}
