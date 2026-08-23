package com.minimax.deployer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.deployer.entity.ForgeDeploymentLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ForgeDeploymentLogMapper extends BaseMapper<ForgeDeploymentLog> {
    @Delete("DELETE FROM forge_deployment_log WHERE deployment_id = #{deploymentId}")
    int deleteByDeploymentId(Long deploymentId);
}
