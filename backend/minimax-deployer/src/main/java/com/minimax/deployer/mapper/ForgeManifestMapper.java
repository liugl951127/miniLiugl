package com.minimax.deployer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.deployer.entity.ForgeManifest;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ForgeManifestMapper extends BaseMapper<ForgeManifest> {
    @Delete("DELETE FROM forge_manifest WHERE release_id = #{releaseId}")
    int deleteByReleaseId(Long releaseId);
}
