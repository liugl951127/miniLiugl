package com.minimax.ai.marketplace;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.AgentGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * AgentGroup Mapper (V3.0.3)
 */
@Mapper
public interface AgentGroupMapper extends BaseMapper<AgentGroup> {

    /** 按业务 groupId 查 */
    @Select("SELECT * FROM agent_group WHERE groupId = #{groupId} LIMIT 1")
    AgentGroup findByGroupId(@Param("groupId") String groupId);

    /** 按状态查 */
    @Select("SELECT * FROM agent_group WHERE status = #{status} ORDER BY createdAt DESC")
    List<AgentGroup> findByStatus(@Param("status") String status);

    /** 按 ownerId 查 */
    @Select("SELECT * FROM agent_group WHERE ownerId = #{ownerId} ORDER BY createdAt DESC")
    List<AgentGroup> findByOwnerId(@Param("ownerId") Long ownerId);

    /** 累加运行次数 */
    @Update("UPDATE agent_group SET runCount = runCount + 1, lastRunAt = NOW() WHERE groupId = #{groupId}")
    int incrementRunCount(@Param("groupId") String groupId);

    /** 更新状态 */
    @Update("UPDATE agent_group SET status = #{status}, updatedAt = NOW() WHERE groupId = #{groupId}")
    int updateStatus(@Param("groupId") String groupId, @Param("status") String status);
}
