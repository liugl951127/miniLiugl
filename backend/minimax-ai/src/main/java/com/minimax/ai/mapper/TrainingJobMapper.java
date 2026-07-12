package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.TrainingJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * TrainingJob Mapper (V3.2.0)
 */
@Mapper
public interface TrainingJobMapper extends BaseMapper<TrainingJob> {

    /** 按 taskId 查 */
    @Select("SELECT * FROM training_job WHERE taskId = #{taskId} LIMIT 1")
    TrainingJob findByTaskId(@Param("taskId") String taskId);

    /** 按状态查 */
    @Select("SELECT * FROM training_job WHERE status = #{status} ORDER BY createdAt DESC")
    List<TrainingJob> findByStatus(@Param("status") String status);

    /** 按 ownerId 查 */
    @Select("SELECT * FROM training_job WHERE ownerId = #{ownerId} ORDER BY createdAt DESC")
    List<TrainingJob> findByOwnerId(@Param("ownerId") Long ownerId);

    /** 更新最新指标 (单字段, 高频调用) */
    @Update("UPDATE training_job SET currentEpoch = #{epoch}, currentStep = #{step}, " +
            "lastLoss = #{loss}, lastValLoss = #{valLoss}, lastAccuracy = #{accuracy}, " +
            "updatedAt = NOW() WHERE taskId = #{taskId}")
    int updateLatestMetric(@Param("taskId") String taskId,
                            @Param("epoch") int epoch,
                            @Param("step") int step,
                            @Param("loss") double loss,
                            @Param("valLoss") double valLoss,
                            @Param("accuracy") double accuracy);

    /** 更新状态 */
    @Update("UPDATE training_job SET status = #{status}, endTimeMs = #{endTimeMs}, " +
            "error = #{error}, updatedAt = NOW() WHERE taskId = #{taskId}")
    int updateStatus(@Param("taskId") String taskId,
                     @Param("status") String status,
                     @Param("endTimeMs") Long endTimeMs,
                     @Param("error") String error);
}
