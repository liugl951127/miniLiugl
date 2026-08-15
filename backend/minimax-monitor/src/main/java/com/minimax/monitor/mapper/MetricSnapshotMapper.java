package com.minimax.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.monitor.entity.MetricSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MetricSnapshotMapper extends BaseMapper<MetricSnapshot> {

    /** 最近 N 分钟的指标 */
    List<MetricSnapshot> selectRecent(@Param("metricName") String metricName,
                                       @Param("service") String service,
                                       @Param("sinceMinutes") int sinceMinutes,
                                       @Param("limit") int limit);

    /** 按指标聚合 (avg/max/min/count) - 供趋势图 */
    List<Map<String, Object>> aggregate(@Param("metricName") String metricName,
                                         @Param("service") String service,
                                         @Param("sinceMinutes") int sinceMinutes);

    /** 清理 N 天前的快照 */
    int deleteOlderThan(@Param("days") int days);
}
