package com.minimax.ai.distribute.spark;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Spark Task (V3.5.4 自研)
 *
 * <p>Stage 的最小执行单元, 一个 Task 处理一个 partition.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SparkTask {
    /** Task 唯一 ID */
    private String taskId;
    /** 所属 Stage ID */
    private String stageId;
    /** 所属 Job ID */
    private String jobId;
    /** 输入 partition 数据 (来自上一 Stage 的 shuffle) */
    private List<Object> partition;
    /** partition 索引 (在 Stage 内) */
    private int partitionIdx;
    /** 执行函数 (Java Function) */
    private TaskFunction function;

    @FunctionalInterface
    public interface TaskFunction {
        Object apply(List<Object> input);
    }
}
