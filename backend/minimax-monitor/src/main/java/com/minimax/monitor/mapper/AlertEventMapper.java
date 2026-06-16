package com.minimax.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.monitor.entity.AlertEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AlertEventMapper extends BaseMapper<AlertEvent> {

    List<AlertEvent> selectRecent(@Param("limit") int limit);

    List<AlertEvent> selectByStatus(@Param("status") String status, @Param("limit") int limit);

    AlertEvent selectLatestByRule(@Param("ruleId") Long ruleId);
}
