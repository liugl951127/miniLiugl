package com.minimax.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.auth.entity.AuthLoginLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthLoginLogMapper extends BaseMapper<AuthLoginLog> {
}
