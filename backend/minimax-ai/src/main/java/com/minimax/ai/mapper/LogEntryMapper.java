package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.LogEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Raft 日志 Mapper (V3.5.0)
 */
@Mapper
public interface LogEntryMapper extends BaseMapper<LogEntry> {

    /**
     * 按索引范围查日志
     */
    @Select("SELECT * FROM raft_log WHERE log_index BETWEEN #{start_idx} AND #{end_idx} ORDER BY log_index ASC")
    List<LogEntry> findRange(@Param("startIdx") long startIdx, @Param("endIdx") long endIdx);

    /**
     * 查某节点最大日志索引
     */
    @Select("SELECT IFNULL(MAX(log_index), 0) FROM raft_log WHERE node_id = #{node_id}")
    Long maxIndexOf(@Param("nodeId") String nodeId);

    /**
     * 查全局最大日志索引
     */
    @Select("SELECT IFNULL(MAX(log_index), 0) FROM raft_log")
    Long maxIndex();

    /**
     * 查某任期前的最后一条日志
     */
    @Select("SELECT * FROM raft_log WHERE term <= #{term} ORDER BY log_index DESC LIMIT 1")
    LogEntry lastLogAtTerm(@Param("term") long term);

    /**
     * 标记提交
     */
    @Update("UPDATE raft_log SET committed = 1, committed_at = NOW() WHERE log_index <= #{idx} AND committed = 0")
    int markCommitted(@Param("idx") long idx);
}
