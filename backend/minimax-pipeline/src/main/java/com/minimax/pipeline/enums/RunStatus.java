package com.minimax.pipeline.enums;

/**
 * 工作流执行状态 (V5.32)
 */
public enum RunStatus {
    PENDING,    // 已创建, 未开始
    RUNNING,    // 执行中
    SUCCESS,    // 全部节点成功
    FAILED,     // 至少一个节点失败
    CANCELED,   // 用户取消 (V5.32.x)
    SKIPPED     // 全部 SKIPPED (前置失败)
}
