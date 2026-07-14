package com.minimax.ws.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ws.entity.CollabRoom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 协作房间 Mapper (V2.8.7)
 *
 * @author MiniMax
 */
@Mapper
public interface CollabRoomMapper extends BaseMapper<CollabRoom> {

    /**
     * 增加参与人数 (原子操作, 避免并发串号)
     *
     * @param roomId 房间 ID
     * @param delta 变化量 (+1 加入, -1 离开)
     * @return 受影响行数
     */
    @Update("UPDATE collab_room SET current_participants = current_participants + #{delta}, " +
            "lastActivityAt = NOW() WHERE roomId = #{roomId}")
    int updateParticipantCount(@Param("roomId") String roomId, @Param("delta") int delta);

    /**
     * 关闭房间
     */
    @Update("UPDATE collab_room SET status = 'CLOSED', closed_at = NOW() " +
            "WHERE roomId = #{roomId} AND status = 'ACTIVE'")
    int closeRoom(@Param("roomId") String roomId);
}
