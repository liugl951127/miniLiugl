package com.minimax.analytics.service.schema;

import com.minimax.analytics.vo.ColumnInfo;
import com.minimax.analytics.vo.IndexInfo;
import com.minimax.analytics.vo.TableInfo;

import java.util.List;
import java.util.Map;

/**
 * 数据库 Schema 服务接口 (V5.31)
 *
 * 读 information_schema, 生成表结构 + 画像
 */
public interface SchemaService {

    /** 列出所有数据库 (V5.31: SELECT SCHEMA_NAME FROM information_schema.SCHEMATA) */
    List<String> listDatabases(Long dataSourceId);

    /** 列出指定数据库的所有表 (按 keyword 模糊搜索) */
    List<TableInfo> listTables(Long dataSourceId, String database, String keyword);

    /** 表结构详情 (列 + 索引 + DDL + 样本) */
    TableInfo describeTable(Long dataSourceId, String database, String tableName);

    /** 数据画像: 每列空值率/分布/类型 */
    TableInfo profileTable(Long dataSourceId, String database, String tableName);

    /** ER 关系图 (V5.31: 返回 ECharts graph 配置) */
    Map<String, Object> buildErGraph(Long dataSourceId, String database);
}
