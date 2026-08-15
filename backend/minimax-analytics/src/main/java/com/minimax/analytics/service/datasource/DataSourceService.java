package com.minimax.analytics.service.datasource;

import com.minimax.analytics.dto.DataSourceDTO;
import com.minimax.analytics.entity.DataSource;

import java.util.List;

/**
 * 数据源服务接口 (V5.31)
 *
 * 多数据源池: 按 dataSourceId 缓存 HikariDataSource
 * 密码 AES 加密存储
 */
public interface DataSourceService {

    /** 拿 javax.sql.DataSource (内部池缓存) */
    javax.sql.DataSource getDataSource(Long dataSourceId);

    /** CRUD */
    Long create(Long userId, DataSourceDTO dto);
    void update(Long userId, Long id, DataSourceDTO dto);
    void delete(Long userId, Long id);
    DataSource getById(Long userId, Long id);
    List<DataSource> listByUser(Long userId, int page, int size);

    /** 测试连接 (不保存) */
    boolean testConnection(DataSourceDTO dto);
}
