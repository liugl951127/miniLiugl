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

    /** 按业务 group_id 查 */
    @Select("SELECT * FROM agent_group WHERE group_id = #{groupId} LIMIT 1")
    AgentGroup findByGroupId(@Param("groupId") String groupId);

    /** 按状态查 */
    @Select("SELECT * FROM agent_group WHERE status = #{status} ORDER BY created_at DESC")
    List<AgentGroup> findByStatus(@Param("status") String status);

    /** 按 owner_id 查 */
    @Select("SELECT * FROM agent_group WHERE owner_id = #{ownerId} ORDER BY created_at DESC")
    List<AgentGroup> findByOwnerId(@Param("ownerId") Long ownerId);

    /** 累加运行次数 */
    @Update("UPDATE agent_group SET run_count = run_count + 1, last_run_at = NOW() WHERE group_id = #{groupId}")
    int incrementRunCount(@Param("groupId") String groupId);

    /** 更新状态 */
    @Update("UPDATE agent_group SET status = #{status}, updated_at = NOW() WHERE group_id = #{groupId}")
    int updateStatus(@Param("groupId") String groupId, @Param("status") String status);
}
