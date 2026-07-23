package com.minimax.model.prompt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.model.prompt.entity.PromptTemplate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PromptTemplateMapper extends BaseMapper<PromptTemplate> {
}
