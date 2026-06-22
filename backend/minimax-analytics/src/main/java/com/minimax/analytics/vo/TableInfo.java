package com.minimax.analytics.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 表结构详情 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableInfo {
    private String name;              // 表名
    private String comment;           // 注释
    private String engine;            // InnoDB / MyISAM
    private Long rowCount;            // 估算行数 (来自 information_schema.TABLES)
    private Long dataSize;            // 数据大小 (字节)
    private Long indexSize;           // 索引大小
    private String createTime;        // 创建时间
    private String updateTime;        // 更新时间
    private String ddl;               // CREATE TABLE 语句 (V5.31 新增)

    private List<ColumnInfo> columns;
    private List<IndexInfo> indexes;

    // V5.31 扩展: 样本数据
    private List<Map<String, Object>> sample;  // 前 5 行样本
}
