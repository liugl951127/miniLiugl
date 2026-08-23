package com.minimax.deployer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.deployer.entity.ForgeDeployment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ForgeDeploymentMapper extends BaseMapper<ForgeDeployment> {
}
