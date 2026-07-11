package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {

    /** 按会话查, 按 created_at 升序 */
    default List<AiChatMessage> findBySessionOrderByCreatedAsc(Long sessionId) {
        return selectList(new QueryWrapper<AiChatMessage>()
                .eq("session_id", sessionId)
                .orderByAsc("created_at"));
    }
}
