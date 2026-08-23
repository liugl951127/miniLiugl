package com.minimax.deployer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.deployer.entity.ForgeRelease;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ForgeReleaseMapper extends BaseMapper<ForgeRelease> {

    /** 查询项目所有 release (按 version 倒序) */
    default List<ForgeRelease> findByProjectId(Long projectId) {
        return selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ForgeRelease>()
                .eq("project_id", projectId)
                .orderByDesc("created_at")
        );
    }

    /** 查询项目的当前激活 release */
    default ForgeRelease findCurrentByProjectId(Long projectId) {
        return selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ForgeRelease>()
                .eq("project_id", projectId)
                .eq("status", "ACTIVE")
                .orderByDesc("deployed_at")
                .last("LIMIT 1")
        );
    }
}
