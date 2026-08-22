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

    /** 最近 N 条 (V6.8.2 修复: n 是 int, 拼接前先 max(0) 限幅, 再用 String.format 安全拼接) */
    default List<AlertEvent> selectRecent(int n) {
        int safe = Math.max(0, Math.min(n, 10000));  // 上限 10000 防滥用
        return selectList(new QueryWrapper<AlertEvent>()
                .orderByDesc("fired_at")
                .last(String.format("LIMIT %d", safe)));
    }

    /** 按状态查 (V6.8.2 修复: n 限幅, 避免 int 注入隐患) */
    default List<AlertEvent> selectByStatus(String status, int n) {
        int safe = Math.max(0, Math.min(n, 10000));
        return selectList(new QueryWrapper<AlertEvent>()
                .eq("status", status)
                .orderByDesc("fired_at")
                .last(String.format("LIMIT %d", safe)));
    }
}
