package com.minimax.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.chat.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    /** 用户会话列表（按最近活跃倒序）。 */
    List<ChatSession> selectByUserId(@Param("userId") Long userId,
                                    @Param("status") Integer status);

    /** 增加消息计数 + 更新最后消息时间。 */
    int bumpMessage(@Param("sessionId") Long sessionId,
                    @Param("at") java.time.LocalDateTime at);

    /** 软删：把 status 设为 0。 */
    int archiveByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);
}
