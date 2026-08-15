package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.AiVotingRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AiVotingRecordMapper (V6.8.10 Day 39)
 * 投票记录持久层，提供统计聚合查询
 *
 * @author Mavis
 * @since V6.8.10
 */
@Mapper
public interface AiVotingRecordMapper extends BaseMapper<AiVotingRecord> {

    /**
     * 投票汇总统计
     * @return totalVotes, avgAgreement, topModel, consensusRate
     */
    @Select("""
        SELECT
          COUNT(*)                                    AS totalVotes,
          COALESCE(AVG(agreement_rate), 0)            AS avgAgreement,
          COALESCE(SUM(CASE WHEN agreement_rate >= 0.7 THEN 1 ELSE 0 END) * 1.0 / COUNT(*), 0) AS consensusRate,
          COUNT(DISTINCT JSON_EXTRACT(model_votes, CONCAT('$[', n.n, '].model')))
            AS modelCount
        FROM ai_voting_record v
        LEFT JOIN (
          SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
        ) n ON JSON_EXTRACT(v.model_votes, CONCAT('$[', n.n, '].model')) IS NOT NULL
        """)
    Map<String, Object> selectVotingStats();

    /**
     * 近N天投票趋势
     * @param since 起始时间
     * @return list of {date, votes, avgAgreement}
     */
    @Select("""
        SELECT
          DATE(created_at)                    AS voteDate,
          COUNT(*)                            AS votes,
          ROUND(AVG(agreement_rate), 4)      AS avgAgreement
        FROM ai_voting_record
        WHERE created_at >= #{since}
        GROUP BY DATE(created_at)
        ORDER BY voteDate ASC
        """)
    List<Map<String, Object>> selectVotingTrend(@Param("since") LocalDateTime since);

    /**
     * 分页查询投票记录
     * @param offset 偏移
     * @param limit 每页数量
     * @return 投票记录列表
     */
    @Select("""
        SELECT id, session_id, user_id, username, question,
               final_answer, strategy, total_votes, agreement_rate,
               model_votes, duration_ms, created_at
        FROM ai_voting_record
        ORDER BY created_at DESC
        LIMIT #{offset}, #{limit}
        """)
    List<Map<String, Object>> selectRecords(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 投票总数 (分页用)
     */
    @Select("SELECT COUNT(*) FROM ai_voting_record")
    long selectTotalCount();
}
