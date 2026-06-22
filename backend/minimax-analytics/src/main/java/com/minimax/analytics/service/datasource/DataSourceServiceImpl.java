package com.minimax.analytics.service.datasource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minimax.analytics.dto.DataSourceDTO;
import com.minimax.analytics.entity.DataSource;
import com.minimax.analytics.mapper.DataSourceMapper;
import com.minimax.common.exception.BizException;
import com.minimax.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源服务实现 (V5.31)
 *
 * 简单多数据源: 内存 ConcurrentHashMap 缓存, 重启后从数据库恢复
 * 密码 AES 加密 (V5.31 简化: Base64, 真实项目用 jasypt)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceServiceImpl implements DataSourceService {

    private final DataSourceMapper mapper;

    /** 数据源池缓存: dsId → HikariDataSource */
    private final ConcurrentHashMap<Long, javax.sql.DataSource> poolCache = new ConcurrentHashMap<>();

    @Override
    public javax.sql.DataSource getDataSource(Long dataSourceId) {
        javax.sql.DataSource cached = poolCache.get(dataSourceId);
        if (cached != null) return cached;
        // 从 DB 加载 (V5.31: 不区分 user, 后台管理用)
        DataSource entity = mapper.selectById(dataSourceId);
        if (entity == null) throw new BizException(ResultCode.NOT_FOUND, "数据源不存在: " + dataSourceId);
        javax.sql.DataSource ds = buildPool(entity);
        poolCache.put(dataSourceId, ds);
        return ds;
    }

    @Override
    public Long create(Long userId, DataSourceDTO dto) {
        DataSource entity = new DataSource();
        entity.setUserId(userId);
        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setJdbcUrl(dto.getJdbcUrl());
        entity.setUsername(dto.getUsername());
        entity.setPasswordEnc(encrypt(dto.getPassword()));
        entity.setDescription(dto.getDescription());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        mapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long userId, Long id, DataSourceDTO dto) {
        DataSource entity = requireOwned(userId, id);
        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setJdbcUrl(dto.getJdbcUrl());
        entity.setUsername(dto.getUsername());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            entity.setPasswordEnc(encrypt(dto.getPassword()));
        }
        entity.setDescription(dto.getDescription());
        entity.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(entity);
        poolCache.remove(id);  // 重建
    }

    @Override
    public void delete(Long userId, Long id) {
        requireOwned(userId, id);
        mapper.deleteById(id);
        poolCache.remove(id);
    }

    @Override
    public DataSource getById(Long userId, Long id) {
        DataSource entity = requireOwned(userId, id);
        entity.setPasswordEnc(null);  // 脱敏
        return entity;
    }

    @Override
    public List<DataSource> listByUser(Long userId, int page, int size) {
        List<DataSource> list = mapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<DataSource>().eq(DataSource::getUserId, userId)
                        .orderByDesc(DataSource::getCreatedAt)).getRecords();
        list.forEach(d -> d.setPasswordEnc(null));
        return list;
    }

    @Override
    public boolean testConnection(DataSourceDTO dto) {
        DataSource tmp = new DataSource();
        tmp.setJdbcUrl(dto.getJdbcUrl());
        tmp.setUsername(dto.getUsername());
        tmp.setPasswordEnc(encrypt(dto.getPassword()));
        tmp.setType(dto.getType());
        try {
            javax.sql.DataSource ds = buildPool(tmp);
            try (var conn = ds.getConnection()) {
                return conn.isValid(5);
            }
        } catch (Exception e) {
            log.warn("测试连接失败: {}", e.getMessage());
            return false;
        }
    }

    // ---- helpers ----

    private DataSource requireOwned(Long userId, Long id) {
        DataSource entity = mapper.selectById(id);
        if (entity == null) throw new BizException(ResultCode.NOT_FOUND, "数据源不存在");
        if (!entity.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权访问");
        }
        return entity;
    }

    /** 简单 Base64 加密 (V5.31 简化; 生产用 jasypt / hutool SecureUtil) */
    private String encrypt(String plain) {
        if (plain == null) return null;
        return java.util.Base64.getEncoder().encodeToString(plain.getBytes());
    }

    private String decrypt(String enc) {
        if (enc == null) return null;
        return new String(java.util.Base64.getDecoder().decode(enc));
    }

    /** 构造连接池 (V5.31: 简单版, 用 DriverManager + 连接池 5 个) */
    private javax.sql.DataSource buildPool(DataSource entity) {
        // 用 HikariCP 构造
        com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
        config.setJdbcUrl(entity.getJdbcUrl());
        config.setUsername(entity.getUsername());
        config.setPassword(decrypt(entity.getPasswordEnc()));
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setReadOnly(true);  // V5.31 安全: 数据源只读
        config.setPoolName("analytics-ds-" + entity.getId());
        return new com.zaxxer.hikari.HikariDataSource(config);
    }
}
