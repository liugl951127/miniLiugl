package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.TrainingMetric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * TrainingMetric Mapper (V3.2.0)
 */
@Mapper
public interface TrainingMetricMapper extends BaseMapper<TrainingMetric> {

    /** 按 taskId 查全部 (按 step 升序) */
    @Select("SELECT * FROM training_metric WHERE taskId = #{taskId} ORDER BY step ASC")
    List<TrainingMetric> findByTaskId(@Param("taskId") String taskId);

    /** 按 taskId 查最近 N 条 */
    @Select("SELECT * FROM training_metric WHERE taskId = #{taskId} ORDER BY step DESC LIMIT #{limit}")
    List<TrainingMetric> findRecent(@Param("taskId") String taskId, @Param("limit") int limit);

    /** 计数 */
    @Select("SELECT COUNT(*) FROM training_metric WHERE taskId = #{taskId}")
    int countByTaskId(@Param("taskId") String taskId);
}
