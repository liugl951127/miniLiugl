package com.minimax.pipeline.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * analytics_datasource 表 Mapper（pipeline 直接查 analytics 数据源配置）
 *
 * V6.8.1: 从 minimax-analytics 解耦，pipeline 自建 Mapper 查同一张表。
 * 两个服务共用同一 MySQL DB，analytics_datasource 表是共享配置表。
 *
 * 复用 DataSourceDTO（shared DTO）而非新建 pipeline 实体，避免重复定义。
 */
@Mapper
public interface AnalyticsDataSourceMapper {

    @Select("SELECT id, user_id AS userId, name, type, jdbc_url AS jdbcUrl, " +
            "username, password_enc AS passwordEnc, description, " +
            "deleted, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM analytics_datasource WHERE id = #{id} AND deleted = 0 LIMIT 1")
    com.minimax.common.feign.analytics.DataSourceDTO selectById(@Param("id") Long id);
}
