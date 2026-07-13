package com.minimax.ai.distribute.spark;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Spark Stage (V3.5.4 自研)
 *
 * <p>DAG 节点, 由若干 Task 组成 (每个 partition 一个 Task).
 * Stage 之间有依赖关系 (RDD lineage).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SparkStage {
    /** Stage ID */
    private String stageId;
    /** 所属 Job ID */
    private String jobId;
    /** 输入 partition 数 (= 输出 partition 数, 由父 Stage 决定) */
    private int numPartitions;
    /** 父 Stage ID (null = 根) */
    private String parentId;
    /** Shuffle 输出 ID (跨 Stage 通信用) */
    private String shuffleId;
    /** 父 Stage 的 shuffleId (从父 Stage 读输入) */
    private String parentShuffleId;
    /** Stage 计算函数 */
    private SparkTask.TaskFunction function;
    /** 初始数据 (仅根 Stage 有, 来自构造时的 dataset) */
    private List<List<Object>> initialData;
    /** 是否已执行 */
    private boolean completed;

    public static String newId() {
        return "stage-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String newShuffleId() {
        return "shuffle-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
