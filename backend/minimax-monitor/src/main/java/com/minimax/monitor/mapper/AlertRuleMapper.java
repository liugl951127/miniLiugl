package com.minimax.monitor.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.monitor.entity.AlertRule;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AlertRuleMapper extends BaseMapper<AlertRule> {

    /** 默认查所有启用的规则 (extracted) */
    default List<AlertRule> selectEnabled() {
        return selectList(new QueryWrapper<AlertRule>().eq("enabled", 1));
    }
}
