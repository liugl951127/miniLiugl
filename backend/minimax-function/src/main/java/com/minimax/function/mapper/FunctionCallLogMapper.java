package com.minimax.function.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.function.entity.FunctionCallLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FunctionCallLogMapper extends BaseMapper<FunctionCallLog> {

    List<FunctionCallLog> selectByUser(@Param("userId") Long userId,
                                        @Param("limit") int limit);

    List<FunctionCallLog> selectByTool(@Param("toolName") String toolName,
                                       @Param("limit") int limit);
}
