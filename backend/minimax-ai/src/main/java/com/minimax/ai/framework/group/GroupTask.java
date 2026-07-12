package com.minimax.ai.framework.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 群组任务 (V3.0.3)
 *
 * <p>用户提交给群组的一个任务
 * <p>manager 拆解后, 每个 worker 处理一个 subtask
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupTask {

    /** 任务 ID (UUID) */
    private String taskId;
    /** 群组 ID */
    private String groupId;
    /** 任务主题 */
    private String subject;
    /** 任务原始内容 (用户输入) */
    private String input;
    /** 任务类型 (e.g. "writing", "code", "analysis") */
    private String type;
    /** 期望产出 (e.g. "200字摘要") */
    private String expectedOutput;
    /** 超时 (ms) */
    private long timeoutMs;
    /** 附加参数 */
    @Builder.Default
    private Map<String, Object> params = new HashMap<>();
    /** 拆解后的子任务 (manager 生成) */
    @Builder.Default
    private java.util.List<SubTask> subTasks = new java.util.ArrayList<>();

    /**
     * 子任务 (Worker 单元)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubTask {
        /** 子任务 ID */
        private String subId;
        /** 分配的 worker */
        private String assignee;
        /** 指令 */
        private String instruction;
        /** 依赖的子任务 (上游结果) */
        private String dependsOn;
        /** 状态 */
        private Status status;
        /** 结果 */
        private String result;

        public enum Status { PENDING, RUNNING, DONE, FAILED }
    }
}
