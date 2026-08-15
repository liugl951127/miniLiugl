package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.DashboardMetric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DashboardMetric Mapper (V3.2.1)
 */
@Mapper
public interface DashboardMetricMapper extends BaseMapper<DashboardMetric> {

    /** 按指标 + 时间范围查 */
    @Select("SELECT * FROM dashboard_metric WHERE metric = #{metric} AND timestamp >= #{since} " +
            "ORDER BY timestamp ASC")
    List<DashboardMetric> findByMetricSince(@Param("metric") String metric,
                                              @Param("since") LocalDateTime since);

    /** 最新一条 (按 metric + dimension) */
    @Select("SELECT * FROM dashboard_metric WHERE metric = #{metric} AND dimension = #{dimension} " +
            "ORDER BY timestamp DESC LIMIT 1")
    DashboardMetric findLatest(@Param("metric") String metric, @Param("dimension") String dimension);
}
