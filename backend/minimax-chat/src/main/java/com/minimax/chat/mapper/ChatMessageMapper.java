package com.minimax.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.chat.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /** 按 session 拉取消息，分页（按 id 升序稳定）。 */
    List<ChatMessage> selectBySessionId(@Param("sessionId") Long sessionId,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    /** 拉最后 N 条（用于上下文窗口）。 */
    List<ChatMessage> selectLastN(@Param("sessionId") Long sessionId,
                                  @Param("limit") int limit);

    /** 物理计数（绕过 @TableLogic）。 */
    long countBySessionId(@Param("sessionId") Long sessionId);
}
