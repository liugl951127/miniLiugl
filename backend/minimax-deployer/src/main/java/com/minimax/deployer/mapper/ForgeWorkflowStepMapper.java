package com.minimax.deployer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.deployer.entity.ForgeWorkflowStep;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ForgeWorkflowStepMapper extends BaseMapper<ForgeWorkflowStep> {
    @Delete("DELETE FROM forge_workflow_step WHERE project_id = #{projectId}")
    int deleteByProjectId(Long projectId);

    @Delete("DELETE FROM forge_workflow_step WHERE release_id = #{releaseId}")
    int deleteByReleaseId(Long releaseId);
}
