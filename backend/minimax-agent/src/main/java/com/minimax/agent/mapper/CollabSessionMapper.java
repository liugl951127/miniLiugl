package com.minimax.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.agent.entity.CollabSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CollabSessionMapper extends BaseMapper<CollabSession> {
    List<CollabSession> selectByOwner(@Param("ownerId") Long ownerId);
    CollabSession selectBySessionId(@Param("sessionId") String sessionId);
}
