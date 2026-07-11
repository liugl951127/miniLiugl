package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.SensitiveWord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {
}
