package com.minimax.ai.framework.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 群组执行结果 (V3.0.3)
 *
 * <p>汇总 manager + workers + critics 的输出
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResult {

    /** 关联任务 ID */
    private String taskId;
    /** 群组 ID */
    private String groupId;
    /** 状态 */
    private Status status;
    /** 最终输出 (manager 聚合) */
    private String finalOutput;
    /** 各 worker 输出 (agentName → output) */
    @Builder.Default
    private Map<String, String> workerOutputs = new HashMap<>();
    /** 投票/共识 (VOTE/DEBATE 策略) */
    private String consensus;
    /** 评分 (1-10) */
    private double score;
    /** 总耗时 (ms) */
    private long durationMs;
    /** 错误信息 (如有) */
    private String error;
    /** 详细元数据 (策略 / 轮数 / tokens) */
    @Builder.Default
    private Map<String, Object> meta = new HashMap<>();

    public enum Status { SUCCESS, FAILED, TIMEOUT, ABORTED }

    /**
     * 判定是否成功
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
