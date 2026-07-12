package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.ClusterNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * ClusterNode Mapper (V3.3.0)
 */
@Mapper
public interface ClusterNodeMapper extends BaseMapper<ClusterNode> {

    /** 按 nodeId 查 */
    @Select("SELECT * FROM cluster_node WHERE nodeId = #{nodeId} LIMIT 1")
    ClusterNode findByNodeId(@Param("nodeId") String nodeId);

    /** 按状态查 */
    @Select("SELECT * FROM cluster_node WHERE status = #{status} ORDER BY createdAt DESC")
    List<ClusterNode> findByStatus(@Param("status") String status);

    /** 所有 ACTIVE 节点 */
    @Select("SELECT * FROM cluster_node WHERE status = 'ACTIVE' ORDER BY createdAt ASC")
    List<ClusterNode> findActive();

    /** 找当前 leader */
    @Select("SELECT * FROM cluster_node WHERE isLeader = TRUE AND status = 'ACTIVE' LIMIT 1")
    ClusterNode findCurrentLeader();

    /** 设 leader (清掉其他, 设这个) */
    @Update("UPDATE cluster_node SET isLeader = (nodeId = #{nodeId})")
    int setLeader(@Param("nodeId") String nodeId);

    /** 心跳 (更新 lastHeartbeat) */
    @Update("UPDATE cluster_node SET lastHeartbeat = NOW(), status = 'ACTIVE', " +
            "cpuUsage = #{cpu}, memoryUsage = #{mem}, gpuUsage = #{gpu}, " +
            "activeTasks = #{tasks} WHERE nodeId = #{nodeId}")
    int heartbeat(@Param("nodeId") String nodeId,
                  @Param("cpu") double cpu,
                  @Param("mem") double mem,
                  @Param("gpu") double gpu,
                  @Param("tasks") int tasks);

    /** 标离线 */
    @Update("UPDATE cluster_node SET status = 'OFFLINE' WHERE nodeId = #{nodeId}")
    int markOffline(@Param("nodeId") String nodeId);
}
