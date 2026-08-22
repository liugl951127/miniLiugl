package com.minimax.ai.marketplace;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * AgentGroupMember Mapper (T1-backend-orchestrator)
 *
 * <p>BaseMapper 自带: insert / updateById / deleteById / selectById / selectList
 * 这里只追加群组级批量操作。</p>
 */
@Mapper
public interface AgentGroupMemberMapper extends BaseMapper<AgentGroupMember> {

    /** 按 group_id 列出全部成员 (按 position 升序) */
    @Select("SELECT * FROM agent_group_member WHERE group_id = #{groupId} AND enabled = 1 ORDER BY position ASC, id ASC")
    List<AgentGroupMember> findByGroupId(@Param("groupId") Long groupId);

    /** 按 group_id 列出全部成员 (含 disabled, 管理员视图) */
    @Select("SELECT * FROM agent_group_member WHERE group_id = #{groupId} ORDER BY position ASC, id ASC")
    List<AgentGroupMember> findAllByGroupId(@Param("groupId") Long groupId);

    /** 按 group_id 计数 */
    @Select("SELECT COUNT(*) FROM agent_group_member WHERE group_id = #{groupId} AND enabled = 1")
    int countByGroupId(@Param("groupId") Long groupId);

    /** 按 group_id 物理删除全部成员 (reorder 前清空) */
    @Update("DELETE FROM agent_group_member WHERE group_id = #{groupId}")
    int deleteByGroupId(@Param("groupId") Long groupId);
}
