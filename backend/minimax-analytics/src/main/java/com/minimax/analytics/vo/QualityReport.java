package com.minimax.analytics.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 文件质量报告 VO (V5.31)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityReport {
    private Long totalRows;            // 总行数
    private Long totalColumns;         // 总列数
    private Long totalSizeBytes;       // 文件大小
    private String encoding;           // 编码
    private String separator;          // 分隔符 (csv)
    private Long nullCellCount;        // 空单元格总数
    private Double nullRate;           // 整体空值率
    private Long duplicateRowCount;    // 重复行数
    private List<ColumnQuality> columnQualities;  // 每列质量

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnQuality {
        private String name;
        private String inferredType;    // 推断类型: STRING / NUMBER / DATE / BOOL
        private Long nullCount;
        private Long distinctCount;
        private Double nullRate;
        private Object minValue;
        private Object maxValue;
        private Double avgValue;
        private List<String> topValues;     // top 5
        private List<Map<String, Object>> valueDistribution;  // 数值列的分布直方图
    }
}
