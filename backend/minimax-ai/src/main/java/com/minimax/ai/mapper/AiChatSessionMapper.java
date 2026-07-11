package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.AiChatSession;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AiChatSessionMapper extends BaseMapper<AiChatSession> {

    /** 按用户查, 按 updated_at 倒序 */
    default List<AiChatSession> findByUserOrderByUpdatedDesc(Long userId) {
        return selectList(new QueryWrapper<AiChatSession>()
                .eq("user_id", userId)
                .orderByDesc("updated_at"));
    }
}
