package com.minimax.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.auth.entity.OAuthBinding;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OAuthBindingMapper extends BaseMapper<OAuthBinding> {
}