package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.TrainingCheckpoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * TrainingCheckpoint Mapper (V3.2.0)
 */
@Mapper
public interface TrainingCheckpointMapper extends BaseMapper<TrainingCheckpoint> {

    /** 按 taskId 查全部 (按 epoch 倒序) */
    @Select("SELECT * FROM training_checkpoint WHERE task_id = #{task_id} ORDER BY epoch DESC, step DESC")
    List<TrainingCheckpoint> findByTaskId(@Param("taskId") String taskId);

    /** 按 taskId + tag 查 (e.g. "best", "latest") */
    @Select("SELECT * FROM training_checkpoint WHERE task_id = #{task_id} AND tags LIKE CONCAT('%', #{tag}, '%') " +
            "ORDER BY epoch DESC, step DESC LIMIT 1")
    TrainingCheckpoint findByTaskIdAndTag(@Param("taskId") String taskId, @Param("tag") String tag);

    /** 按 taskId 找 val_loss 最小 (best) */
    @Select("SELECT * FROM training_checkpoint WHERE task_id = #{task_id} ORDER BY val_loss ASC LIMIT 1")
    TrainingCheckpoint findBestByTaskId(@Param("taskId") String taskId);

    /** 按 checkpointId 查 */
    @Select("SELECT * FROM training_checkpoint WHERE checkpoint_id = #{checkpoint_id} LIMIT 1")
    TrainingCheckpoint findByCheckpointId(@Param("checkpointId") String checkpointId);
}
