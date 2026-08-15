package com.minimax.ai.eval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 评测报告 (汇总)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalReport {

    /** 报告 ID (时间戳) */
    public String reportId;

    /** 开始时间 */
    public LocalDateTime startedAt;

    /** 结束时间 */
    public LocalDateTime finishedAt;

    /** 总耗时 (ms) */
    public long totalMs;

    /** 总用例数 */
    public int total;

    /** 通过数 */
    public int passed;

    /** 失败数 */
    public int failed;

    /** 通过率 */
    public double passRate;

    /** 平均分 */
    public double avgScore;

    /** 平均延迟 (ms) */
    public double avgLatencyMs;

    /** 按类别统计: 类别 -> {total, passed, passRate} */
    public Map<String, CategoryStat> categoryStats;

    /** 失败用例详情 (用于 debug) */
    public List<EvalResult> failures;

    /** 所有结果 */
    public List<EvalResult> allResults;

    /**
     * 类别统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStat {
        public String category;
        public int total;
        public int passed;
        public double passRate;
    }

    /**
     * 简化为单行摘要
     */
    public String summary() {
        return String.format(
            "[%s] %d/%d 通过 (%.1f%%), 平均分 %.3f, 平均延迟 %.0fms",
            reportId, passed, total, passRate * 100, avgScore, avgLatencyMs
        );
    }
}
