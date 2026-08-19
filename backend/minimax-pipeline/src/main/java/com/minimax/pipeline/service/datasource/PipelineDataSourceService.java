package com.minimax.pipeline.service.datasource;

import com.minimax.common.feign.analytics.DataSourceDTO;
import com.minimax.pipeline.mapper.AnalyticsDataSourceMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源 Service（V6.8.1 重构版）
 *
 * minimax-pipeline 自建数据源连接池，不再依赖 minimax-analytics 的 Maven 依赖。
 * 两服务共用 analytics_datasource 表，pipeline 通过 MyBatis Mapper 直接查询。
 *
 * V6.8.1: 从 minimax-analytics 解耦
 */
@Slf4j
@Service
public class PipelineDataSourceService {

    private final AnalyticsDataSourceMapper dsMapper;
    /** 缓存已创建的数据源连接池 */
    private final Map<Long, HikariDataSource> poolCache = new ConcurrentHashMap<>();

    public PipelineDataSourceService(AnalyticsDataSourceMapper dsMapper) {
        this.dsMapper = dsMapper;
    }

    /** 拿 javax.sql.DataSource（内部 HikariCP 池缓存） */
    public DataSource getDataSource(Long dataSourceId) {
        return poolCache.computeIfAbsent(dataSourceId, id -> {
            DataSourceDTO dto = dsMapper.selectById(id);
            if (dto == null) {
                throw new IllegalArgumentException("数据源不存在或已删除: id=" + id);
            }
            return buildPool(dto);
        });
    }

    /** 拿 DataSourceDTO */
    public DataSourceDTO getById(Long dataSourceId) {
        return dsMapper.selectById(dataSourceId);
    }

    private HikariDataSource buildPool(DataSourceDTO dto) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(dto.getJdbcUrl());
        cfg.setUsername(dto.getUsername());
        // 密码是 AES 加密的，analytics 服务会解密。
        // pipeline 这里直接用加密后的密文（需要 analytics 提供解密接口，或 pipeline 自己解密）
        // 临时方案：从环境变量/配置中取解密密钥
        cfg.setPassword(loadDecryptedPassword(dto));
        cfg.setMaximumPoolSize(4);
        cfg.setMinimumIdle(1);
        cfg.setConnectionTimeout(30000);
        cfg.setIdleTimeout(600000);
        cfg.setMaxLifetime(1800000);
        cfg.setPoolName("pipeline-ds-" + dto.getId());
        log.info("[PipelineDataSource] 创建连接池: name={} type={}", dto.getName(), dto.getType());
        return new HikariDataSource(cfg);
    }

    private String loadDecryptedPassword(DataSourceDTO dto) {
        // TODO(V6.8.2): analytics 服务应提供密码解密 API，pipeline 通过 Feign 调用。
        // 临时：pipeline 本地持有 AES 密钥（应从配置中心读取）
        String encrypted = dto.getPasswordEnc();
        if (encrypted == null || encrypted.isBlank()) return "";
        try {
            // AES-256 解密（复用 analytics 的密钥）
            String key = System.getProperty("datasource.aes.key",
                    "minimax-aes-key-32bytes!!!!!"); // 临时硬编码，生产从配置中心读取
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES");
            javax.crypto.spec.SecretKeySpec keySpec =
                    new javax.crypto.spec.SecretKeySpec(key.getBytes(java.nio.charset.StandardCharsets.UTF_8), "AES");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(java.util.Base64.getDecoder().decode(encrypted));
            return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("[PipelineDataSource] 密码解密失败，使用密文: {}", e.getMessage());
            return encrypted;
        }
    }
}
