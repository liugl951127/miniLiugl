package com.minimax.pipeline.enums;

/**
 * 节点执行状态 (V5.32)
 */
public enum NodeStatus {
    PENDING,    // 等待执行 (依赖尚未完成)
    RUNNING,    // 执行中
    SUCCESS,    // 成功
    FAILED,     // 失败
    SKIPPED     // 跳过 (上游失败导致)
}
