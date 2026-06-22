package com.minimax.analytics.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 索引信息 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexInfo {
    private String name;              // 索引名
    private Boolean unique;
    private String type;              // BTREE / HASH
    private Integer seqInIndex;       // 索引中列序号
    private String columnName;        // 列名
}
