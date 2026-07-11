package com.minimax.ws.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ws.entity.CollabMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 协作消息 Mapper (V2.8.7)
 *
 * @author MiniMax
 */
@Mapper
public interface CollabMessageMapper extends BaseMapper<CollabMessage> {

    /**
     * 查询房间最近消息 (按时间正序, 用于回放)
     */
    @Select("SELECT * FROM collab_message WHERE roomId = #{roomId} " +
            "AND createdAt >= #{since} ORDER BY createdAt ASC LIMIT #{limit}")
    List<CollabMessage> findRecent(@Param("roomId") String roomId,
                                   @Param("since") java.time.LocalDateTime since,
                                   @Param("limit") int limit);

    /**
     * 查询聊天历史 (CHAT + AI 类型, 倒序)
     */
    @Select("SELECT * FROM collab_message WHERE roomId = #{roomId} " +
            "AND type IN ('CHAT', 'AI') ORDER BY createdAt DESC LIMIT #{limit}")
    List<CollabMessage> findChatHistory(@Param("roomId") String roomId, @Param("limit") int limit);

    /**
     * 统计房间消息数
     */
    @Select("SELECT COUNT(*) FROM collab_message WHERE roomId = #{roomId}")
    long countByRoom(@Param("roomId") String roomId);
}
