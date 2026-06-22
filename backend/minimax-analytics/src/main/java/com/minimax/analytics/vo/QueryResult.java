package com.minimax.analytics.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * SQL 查询结果 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResult {
    private List<String> columns;             // 列名
    private List<Map<String, Object>> rows;   // 数据行
    private Long rowCount;                    // 行数
    private Long durationMs;                  // 执行耗时
    private String explain;                   // EXPLAIN 结果 (V5.31)
    private List<Map<String, Object>> profile; // V5.31: 列画像 (profile 查询时填充)
}
