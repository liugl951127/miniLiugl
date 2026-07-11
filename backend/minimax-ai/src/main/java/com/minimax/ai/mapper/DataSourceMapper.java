package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.DbDataSource;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DataSourceMapper extends BaseMapper<DbDataSource> {
}
