package com.minimax.deployer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.deployer.entity.ForgeAgent;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ForgeAgentMapper extends BaseMapper<ForgeAgent> {
    @Delete("DELETE FROM forge_agent WHERE release_id = #{releaseId}")
    int deleteByReleaseId(Long releaseId);
}
