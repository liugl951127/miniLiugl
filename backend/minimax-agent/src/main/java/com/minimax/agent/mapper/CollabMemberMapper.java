package com.minimax.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.agent.entity.CollabMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CollabMemberMapper extends BaseMapper<CollabMember> {
    CollabMember selectByUser(@Param("collabId") Long collabId, @Param("userId") Long userId);
}
