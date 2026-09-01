package com.minimax.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.monitor.entity.AlertRcaKnowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AlertRcaKnowledge Mapper (Day 58).
 */
@Mapper
public interface AlertRcaKnowledgeMapper extends BaseMapper<AlertRcaKnowledge> {

    /** 按指标名查询知识条目（降序，最新优先） */
    List<AlertRcaKnowledge> selectByMetricName(@Param("metricName") String metricName,
                                                 @Param("limit") int limit);

    /** 查询用户保存的所有知识条目 */
    List<AlertRcaKnowledge> selectBySavedBy(@Param("savedBy") Long savedBy,
                                            @Param("limit") int limit);
}
