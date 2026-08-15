package com.minimax.monitor.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.monitor.entity.AlertEvent;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AlertEventMapper extends BaseMapper<AlertEvent> {

    /** 查某规则最新一条事件 */
    default AlertEvent selectLatestByRule(Long ruleId) {
        return selectOne(new QueryWrapper<AlertEvent>()
                .eq("rule_id", ruleId)
                .orderByDesc("fired_at")
                .last("LIMIT 1"));
    }

    /** 最近 N 条 */
    default List<AlertEvent> selectRecent(int n) {
        return selectList(new QueryWrapper<AlertEvent>()
                .orderByDesc("fired_at")
                .last("LIMIT " + n));
    }

    /** 按状态查 */
    default List<AlertEvent> selectByStatus(String status, int n) {
        return selectList(new QueryWrapper<AlertEvent>()
                .eq("status", status)
                .orderByDesc("fired_at")
                .last("LIMIT " + n));
    }
}
