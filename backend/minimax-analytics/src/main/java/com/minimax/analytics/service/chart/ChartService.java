package com.minimax.analytics.service.chart;

import com.minimax.analytics.vo.QueryResult;

import java.util.List;
import java.util.Map;

/**
 * 图表服务接口 (V5.31) - 生成 ECharts option
 */
public interface ChartService {

    /** 根据查询结果自动生成 ECharts 配置 */
    Map<String, Object> autoChart(QueryResult result);

    /** 批量生成多个图表 */
    List<Map<String, Object>> autoCharts(List<QueryResult> results);
}
