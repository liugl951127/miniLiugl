package com.minimax.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.auth.entity.AuthRefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthRefreshTokenMapper extends BaseMapper<AuthRefreshToken> {

    int revokeByToken(@Param("token") String token);

    int revokeByUserId(@Param("userId") Long userId);
}
