package com.minimax.deployer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.deployer.entity.AgentTemplate;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentTemplateMapper extends BaseMapper<AgentTemplate> {

    /** 按行业查询模板 */
    default List<AgentTemplate> findByIndustry(String industry) {
        return selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AgentTemplate>()
                .eq("industry", industry)
                .eq("status", "PUBLISHED")
                .orderByDesc("usage_count")
        );
    }

    /** 查询所有已发布模板 (按热度排序) */
    default List<AgentTemplate> findAllPublished() {
        return selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AgentTemplate>()
                .eq("status", "PUBLISHED")
                .orderByDesc("usage_count")
                .orderByDesc("created_at")
        );
    }
}
