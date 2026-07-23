package com.minimax.pipeline.function_ext.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.pipeline.function_ext.entity.FunctionTool;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FunctionToolMapper extends BaseMapper<FunctionTool> {

    FunctionTool selectByName(@Param("name") String name);

    List<FunctionTool> selectByCategory(@Param("category") String category);

    List<FunctionTool> selectEnabled();
}
