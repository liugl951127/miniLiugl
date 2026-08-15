package com.minimax.pipeline.service.datasource;

import com.minimax.analytics.service.datasource.DataSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/**
 * 数据源 Service 桥接 (V5.32)
 *
 * minimax-pipeline 复用 minimax-analytics 的 DataSourceService
 * 通过 Spring 注入直接调
 */
@Service
@RequiredArgsConstructor
public class PipelineDataSourceService {

    private final DataSourceService analyticsDataSourceService;

    /** 拿 javax.sql.DataSource (内部池缓存) */
    public DataSource getDataSource(Long dataSourceId) {
        return analyticsDataSourceService.getDataSource(dataSourceId);
    }

    /** 拿 DataSource entity (V5.32.x) */
    public com.minimax.analytics.entity.DataSource getById(Long userId, Long id) {
        return analyticsDataSourceService.getById(userId, id);
    }
}
