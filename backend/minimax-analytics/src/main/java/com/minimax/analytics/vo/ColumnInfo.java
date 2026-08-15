package com.minimax.analytics.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 表结构字段信息 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnInfo {
    private String name;              // 列名
    private String type;              // 数据类型 (varchar(64))
    private String comment;           // 注释
    private Boolean nullable;         // 是否可空
    private String defaultValue;      // 默认值
    private String keyType;           // PRI / UNI / MUL
    private Long ordinalPosition;     // 序号

    // 数据画像 (V5.31): 仅 profile 接口返回
    private Long nullCount;           // 空值数
    private Long distinctCount;       // 不同值数
    private Double nullRate;          // 空值率
    private List<String> topValues;   // 高频值 top 5
    private Object minValue;          // 最小值
    private Object maxValue;          // 最大值
    private Double avgValue;          // 平均值 (数值列)
}
