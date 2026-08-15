package com.minimax.chat.memory_ext.pref;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserPrefMapper extends BaseMapper<UserPref> {

    List<UserPref> selectByUser(@Param("userId") Long userId);
}
