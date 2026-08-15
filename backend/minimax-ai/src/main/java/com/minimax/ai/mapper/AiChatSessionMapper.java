package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.AiChatSession;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AiChatSessionMapper (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * MyBatis Mapper - AiChatSessionMapper.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 AiChatSessionMapper 的业务能力</li>
 *   <li>参与 AI 平台整体架构</li>
 *   <li>支持 Spring 依赖注入</li>
 * </ul>
 *
 * <h2>依赖</h2>
 * <ul>
 *   <li>Spring Framework (自动注入)</li>
 *   <li>Lombok (简化代码)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V6.1
 */
@Mapper
public interface AiChatSessionMapper extends BaseMapper<AiChatSession> {

    /** 按用户查, 按 updated_at 倒序 */
    default List<AiChatSession> findByUserOrderByUpdatedDesc(Long userId) {
        return selectList(new QueryWrapper<AiChatSession>()
                .eq("user_id", userId)
                .orderByDesc("updated_at"));
    }

    /** 按 sessionId (字符串) 查找会话 (V7.0 Flow③ 消息持久化) */
    default AiChatSession findBySessionId(String sessionId) {
        return selectOne(new QueryWrapper<AiChatSession>()
                .eq("session_id", sessionId));
    }
}
