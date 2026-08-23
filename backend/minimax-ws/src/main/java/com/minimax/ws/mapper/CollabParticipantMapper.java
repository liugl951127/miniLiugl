package com.minimax.ws.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ws.entity.CollabParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 协作参与者 Mapper (V2.8.7)
 *
 * @author MiniMax
 */
@Mapper
public interface CollabParticipantMapper extends BaseMapper<CollabParticipant> {

    /**
     * 查询房间内所有参与者 (包括离线)
     */
    @Select("SELECT * FROM collab_participant WHERE room_id = #{roomId} ORDER BY joined_at ASC")
    List<CollabParticipant> findByRoomId(@Param("roomId") String roomId);

    /**
     * 查询房间内在线参与者
     */
    @Select("SELECT * FROM collab_participant WHERE room_id = #{roomId} " +
            "AND status IN ('ONLINE', 'AWAY') AND left_at IS NULL ORDER BY joined_at ASC")
    List<CollabParticipant> findOnlineByRoomId(@Param("roomId") String roomId);

    /**
     * 用户在该房间的参与记录
     */
    @Select("SELECT * FROM collab_participant WHERE room_id = #{roomId} AND user_id = #{userId} " +
            "AND left_at IS NULL LIMIT 1")
    CollabParticipant findActiveParticipant(@Param("roomId") String roomId,
                                            @Param("userId") Long userId);

    /**
     * 标记用户离开
     */
    @Update("UPDATE collab_participant SET status = 'OFFLINE', left_at = NOW() " +
            "WHERE room_id = #{roomId} AND user_id = #{userId} AND left_at IS NULL")
    int markLeave(@Param("roomId") String roomId, @Param("userId") Long userId);

    /**
     * 更新心跳
     */
    @Update("UPDATE collab_participant SET last_heartbeat = NOW() " +
            "WHERE room_id = #{roomId} AND user_id = #{userId} AND left_at IS NULL")
    int updateHeartbeat(@Param("roomId") String roomId, @Param("userId") Long userId);

    /**
     * 更新光标位置
     */
    @Update("UPDATE collab_participant SET cursor_x = #{x}, cursor_y = #{y}, selection_id = #{selection_id}, " +
            "last_heartbeat = NOW() " +
            "WHERE room_id = #{roomId} AND user_id = #{userId} AND left_at IS NULL")
    int updateCursor(@Param("roomId") String roomId,
                     @Param("userId") Long userId,
                     @Param("x") Integer x,
                     @Param("y") Integer y,
                     @Param("selectionId") String selectionId);
}
